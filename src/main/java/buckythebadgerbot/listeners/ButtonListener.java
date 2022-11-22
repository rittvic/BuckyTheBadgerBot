package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.services.Scraper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for button clicks and handles them accordingly
 * Has a cooldown system (30s) in place to prevent spams
 */
public class ButtonListener extends ListenerAdapter {

    private final BuckyTheBadgerBot bot;
    private static final Logger logger = LoggerFactory.getLogger(ButtonListener.class);

    //Synchronized the LinkedHashMap to allow concurrent modifications
    //Follows the format "userID:buttonName"=timestamp
    private static final Map<String, Long> coolDownChecker = Collections.synchronizedMap(new LinkedHashMap<>());

    //Map to store every embed for a paginated menu
    public static final Map<String, List<MessageEmbed>> paginatedMenus = new HashMap<>();

    //Map to store the paginated buttons for a paginated menu
    public static final Map<String, List<Button>> paginationButtons = new HashMap<>();

    //Scheduler to disable paginated buttons after a set period of time
    public static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(20);

    public ButtonListener(BuckyTheBadgerBot bot) {
        this.bot = bot;
    }

    /**
     * Gathers the buttons to send back to SearchCommand based on the results
     *
     * @param buttonContents an ArrayList of all the results (Course name and number)
     * @param userID  the user id of the user who initiated the SearchCommand
     * @return an ArrayList of Buttons to add onto Action Rows
     */
    public static ArrayList<Button> getButtons(ArrayList<String> buttonContents, String userID, String buttonArgument,ButtonStyle buttonStyle) {
        String uuid = userID + ":" + UUID.randomUUID();
        ArrayList<Button> buttonResults = new ArrayList<>();
        for (String content : buttonContents) {
            buttonResults.add(Button.of(buttonStyle,uuid + ":" + buttonArgument + ":" + content, content));
        }
        return buttonResults;
    }

    /**
     * Create and send a paginated menu
     *
     * @param userID the user id of the user who initiated the search command
     * @param action the original search command
     * @param embeds the list of embeds for the menu
     */
    public static void sendPaginatedMenu(String userID, ReplyCallbackAction action, List<MessageEmbed> embeds) {
        String uuid = userID + ":" + UUID.randomUUID();
        List<Button> components = getPaginationButtons(uuid, embeds.size());
        paginationButtons.put(uuid, components);
        paginatedMenus.put(uuid, embeds);
        //Add the updated buttons and disable them after 10 minutes
        action.setActionRow(components).queue(interactionHook -> disableButtons(uuid, interactionHook));
    }

    /**
     * Create and send the paginated buttons
     *
     * @param uuid     the user ID + random UUID
     * @param maxPages the maximum number of pages on the menu
     * @return a list of the paginated buttons
     */
    private static List<Button> getPaginationButtons(String uuid, int maxPages) {
        return Arrays.asList(
                Button.primary(uuid + ":pagination:first", Emoji.fromUnicode("\u23EA")).asDisabled(),
                Button.primary(uuid + ":pagination:prev", Emoji.fromUnicode("\u25C0")).asDisabled(),
                Button.secondary("pagination:page:0", "1/" + maxPages).asDisabled(),
                Button.primary(uuid + ":pagination:next", Emoji.fromUnicode("\u25B6")),
                Button.primary(uuid + ":pagination:last", Emoji.fromUnicode("\u23E9"))
        );
    }

    /**
     * Disable pagination buttons after 10 minutes
     *
     * @param uuid the user ID + random UUID of the buttons to disable
     * @param hook The message hook pointing to the original message
     */
    public static void disableButtons(String uuid, InteractionHook hook) {
        Runnable task = () -> {
            List<Button> actionRow = ButtonListener.paginationButtons.get(uuid);
            List<Button> newActionRow = new ArrayList<>();
            for (Button button : actionRow) {
                newActionRow.add(button.asDisabled());
            }
            hook.editOriginalComponents(ActionRow.of(newActionRow)).queue();
            ButtonListener.paginationButtons.remove(uuid);
            ButtonListener.paginatedMenus.remove(uuid);
        };
        ButtonListener.scheduledExecutor.schedule(task, 10, TimeUnit.MINUTES);
    }


    /**
     * Method to execute tasks based on the button clicked
     *
     * @param event the event of the button interaction
     * NOTE: The entire method is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", ButtonListener.class.getSimpleName());

            //Check what was pressed
            //[0] User ID [1] UUID [2] Argument [3] Argument Value (E.g, pagination or courseSearch)
            String[] pressedArgs = event.getComponentId().split(":");
            //Store the user ID of who pressed the button
            String eventUserID = event.getUser().getId();

            //For the course command - if the user didn't press the same button within 30 seconds, the task executes and the user gets added to the cooldown after.
            //Otherwise, they get a message saying to wait until 30 seconds has passed since the initial button press.
            if (pressedArgs[2].equals("courseSearch")){
                if (!coolDownChecker.containsKey(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3])
                        || System.currentTimeMillis() > coolDownChecker.get(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3]) + 30000) {
                    long startTime = System.nanoTime();
                    event.deferReply().queue();
                    ArrayList<String> courseSearch;
                    ArrayList<String> courseInformation;
                    String averageGPA;
                    courseSearch = bot.madGradesClient.courseLookUp(event.getButton().getLabel());
                    if (!courseSearch.isEmpty()) {
                        averageGPA = bot.madGradesClient.courseAverageGPA(courseSearch.get(0));
                        courseInformation = Scraper.scrapeCourse(courseSearch.get(1), courseSearch.get(3));
                        if (!courseInformation.isEmpty()) {
                            EmbedBuilder eb = new EmbedBuilder()
                                    .setTitle(courseInformation.get(0))
                                    .setColor(Color.RED)
                                    .setDescription((courseInformation.get(2).replaceAll("replace", " ")))
                                    .addField("Cumulative GPA", String.valueOf(averageGPA), false)
                                    .addField("Credits", courseInformation.get(1), false)
                                    .addField("Requisites", courseInformation.get(3), false)
                                    .addField("Course Designation", courseInformation.get(4).replaceAll("replace", "\n"), false)
                                    .addField("Repeatable For Credit", courseInformation.get(5), false)
                                    .addField("Last Taught", courseInformation.get(6), false);
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime) / 1000000;
                            eb.setFooter("This took " + duration + " ms to respond.");
                            event.getHook().sendMessageEmbeds(eb.build()).queue();
                        } else {
                            event.getHook().sendMessage("Unable to find information on `" + courseSearch.get(3).toUpperCase() + " " + courseSearch.get(1) + " - " + courseSearch.get(4) + "`").queue();
                        }
                    } else {
                        event.getHook().sendMessage("No courses found.").queue();
                    }
                    //Adds the user and the button they pressed to the cooldown
                    coolDownChecker.put(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3], System.currentTimeMillis());
                } else {
                    event.reply("Stop spamming! Please wait 30 seconds.").setEphemeral(true).queue();
                }
                //Clean map of expired timestamps
                //NOTE: Doing a while loop is faster than Collection.removeif by a few milliseconds since it only iterates through expired elements
                Iterator<Map.Entry<String, Long>> iterator = coolDownChecker.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    if (entry.getValue() + 30000 < System.currentTimeMillis()) {
                        iterator.remove();
                    }
                }
            } else if (pressedArgs[2].equals("pagination")) {
                //Get paginated buttons
                List<Button> components = paginationButtons.get(pressedArgs[0] + ":" + pressedArgs[1]);
                //Check if the user requested the original menu
                if (pressedArgs[0].equals(eventUserID)) {
                    //If the next button was pressed
                    if (pressedArgs[3].equals("next")) {
                        int page = Integer.parseInt(components.get(2).getId().split(":")[2]) + 1;
                        List<MessageEmbed> embeds = paginatedMenus.get(pressedArgs[0] + ":" + pressedArgs[1]);
                        if (page < embeds.size()) {
                            // Update buttons
                            components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                            components.set(1, components.get(1).asEnabled());
                            components.set(0, components.get(0).asEnabled());
                            if (page == embeds.size() - 1) {
                                components.set(3, components.get(3).asDisabled());
                                components.set(4, components.get(4).asDisabled());
                            }
                            paginationButtons.put(pressedArgs[0] + ":" + pressedArgs[1], components);
                            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                        }
                     //if previous button was pressed
                    } else if (pressedArgs[3].equals("prev")) {
                        int page = Integer.parseInt(components.get(2).getId().split(":")[2]) - 1;
                        List<MessageEmbed> embeds = paginatedMenus.get(pressedArgs[0] + ":" + pressedArgs[1]);
                        if (page >= 0) {
                            // Update buttons
                            components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                            components.set(3, components.get(3).asEnabled());
                            components.set(4, components.get(4).asEnabled());
                            if (page == 0) {
                                components.set(1, components.get(1).asDisabled());
                                components.set(0, components.get(0).asDisabled());
                            }
                            paginationButtons.put(pressedArgs[0] + ":" + pressedArgs[1], components);
                            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                        }
                      //if fast-forward button was pressed
                    } else if (pressedArgs[3].equals("first")) {
                        int page = 0;
                        List<MessageEmbed> embeds = paginatedMenus.get(pressedArgs[0] + ":" + pressedArgs[1]);
                        //Update buttons
                        components.set(0, components.get(0).asDisabled());
                        components.set(1, components.get(1).asDisabled());
                        components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                        components.set(3, components.get(3).asEnabled());
                        components.set(4, components.get(4).asEnabled());
                        paginationButtons.put(pressedArgs[0] + ":" + pressedArgs[1], components);
                        event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                     //if rewind button was pressed
                    } else if (pressedArgs[3].equals("last")) {
                        List<MessageEmbed> embeds = paginatedMenus.get(pressedArgs[0] + ":" + pressedArgs[1]);
                        int page = embeds.size() - 1;
                        //Update buttons
                        components.set(4, components.get(4).asDisabled());
                        components.set(3, components.get(3).asDisabled());
                        components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                        components.set(1, components.get(1).asEnabled());
                        components.set(0, components.get(0).asEnabled());
                        paginationButtons.put(pressedArgs[0] + ":" + pressedArgs[1], components);
                        event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                    }
                } else {
                    event.reply("You didn't request this!").setEphemeral(true).queue();
                }
            }
        }, bot.service);
    }
}
