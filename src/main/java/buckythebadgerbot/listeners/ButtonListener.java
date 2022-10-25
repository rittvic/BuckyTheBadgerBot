package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.httpclients.Scraper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private static final Map<String,Long> coolDownChecker = Collections.synchronizedMap(new LinkedHashMap<>());

    //To iterate each element that meets the conditions of being expired
    private final Iterator<Map.Entry<String,Long>> entry = coolDownChecker.entrySet().iterator();

    //Map to store every embed for a paginated menu
    public static final Map<String, List<MessageEmbed>> paginatedMenus = new HashMap<>();

    //Map to store the paginated buttons for a paginated menu
    public static final Map<String, List<Button>> paginationButtons = new HashMap<>();

    public ButtonListener(BuckyTheBadgerBot bot) {
        this.bot = bot;
    }

    /**
     * Gathers the buttons to send back to SearchCommand based on the results
     * @param results an ArrayList of all the results (Course name and number)
     * @param userID the user id of the user who initiated the SearchCommand
     * @return an ArrayList of Buttons to add onto Action Rows
     */
    public ArrayList<Button> getButtons(ArrayList<String> results, String userID) {
        ArrayList<Button> buttonResults = new ArrayList<>();
        for (String result : results) {
            buttonResults.add(Button.secondary(userID+":"+result, result));
        }
        return buttonResults;
    }

    /**
     * Create and send a paginated menu
     * @param userID the user id of the user who initiated the search command
     * @param action the original search command
     * @param embeds the list of embeds for the menu
     */
    public static void sendPaginatedMenu(String userID, ReplyCallbackAction action, List<MessageEmbed> embeds) {
        List<Button> components = getPaginationButtons(userID, embeds.size());
        paginationButtons.put(userID, components);
        paginatedMenus.put(userID, embeds);
        //Add the updated buttons and disable them after 10 minutes
        action.setActionRow(components).queue(interactionHook -> interactionHook.editOriginalComponents(ActionRow.of(components).asDisabled()).queueAfter(10, TimeUnit.MINUTES));
    }

    /**
     * Create and send the paginated buttons
     * @param userID the user id of the user who initiated the search command
     * @param maxPages the maximum number of pages on the menu
     * @return a list of the paginated buttons
     */
    private static List<Button> getPaginationButtons(String userID, int maxPages) {
        return Arrays.asList(Button.primary(userID + ":pagination:prev", "Previous").asDisabled(),
        Button.secondary("pagination:page:0", "1/" + maxPages).asDisabled(),
        Button.primary(userID + ":pagination:next", "Next")
        );
    }


    /**
     * Method to execute tasks based on the button clicked
     * @param event the event of the button interaction
     * NOTE: The entire method is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", ButtonListener.class.getSimpleName());

            //Store what was pressed
            String[] pressedArgs = event.getComponentId().split(":");
            String buttonName = event.getButton().getLabel();
            String eventUserID = event.getUser().getId();

            //If paginated buttons were pressed, retrieve them accordingly with the user
            List<Button>components = paginationButtons.get(eventUserID);

            //For the course command - if the user didn't press the same button within 30 seconds, the task executes and the user gets added to the cooldown after.
            //Otherwise, they get a message saying to wait until 30 seconds has passed since the initial button press.
            if ( (!coolDownChecker.containsKey(eventUserID+":"+pressedArgs[1]) || System.currentTimeMillis() > coolDownChecker.get(eventUserID+":"+pressedArgs[1]) + 30000) && pressedArgs.length == 2){
                long startTime = System.nanoTime();
                event.deferReply().queue();
                ArrayList<String> courseSearch;
                ArrayList<String> courseInformation;
                String averageGPA;
                courseSearch = bot.madGradesClient.courseLookUp(buttonName);
                if (!courseSearch.isEmpty()) {
                    averageGPA = bot.madGradesClient.courseAverageGPA(courseSearch.get(0));
                    courseInformation = Scraper.scrapeThis(courseSearch.get(1),courseSearch.get(3));
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
                    } else{
                        event.getHook().sendMessage("Unable to find information on `" + courseSearch.get(3).toUpperCase() + " " + courseSearch.get(1) + " - " + courseSearch.get(4)+"`").queue();
                    }
                } else {
                    event.getHook().sendMessage("No courses found.").queue();
                }

                //Adds the user and the button they pressed to the cooldown
                coolDownChecker.put(eventUserID+":"+pressedArgs[1], System.currentTimeMillis());

                //For the /gym command - condition to check if the buttons are part of a pagination menu
            } else if (pressedArgs[1].equals("pagination")){
                //Check if the user requested the original menu
                if (paginatedMenus.get(eventUserID) != null){
                    //If the "Next" button was pressed
                    if (pressedArgs[2].equals("next")) {
                        // Move to next embed
                        int page = Integer.parseInt(components.get(1).getId().split(":")[2]) + 1;
                        List<MessageEmbed> embeds = paginatedMenus.get(eventUserID);
                        if (page < embeds.size()) {
                            // Update buttons
                            components.set(1, components.get(1).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                            components.set(0, components.get(0).asEnabled());
                            if (page == embeds.size() - 1) {
                                components.set(2, components.get(2).asDisabled());
                            }
                            paginationButtons.put(eventUserID, components);
                            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                        }
                        //If the "Previous" button was pressed
                    } else if (pressedArgs[2].equals("prev")) {
                        // Move to previous embed
                        int page = Integer.parseInt(components.get(1).getId().split(":")[2]) - 1;
                        List<MessageEmbed> embeds = paginatedMenus.get(eventUserID);
                        if (page >= 0) {
                            // Update buttons
                            components.set(1, components.get(1).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                            components.set(2, components.get(2).asEnabled());
                            if (page == 0) {
                                components.set(0, components.get(0).asDisabled());
                            }
                            paginationButtons.put(eventUserID, components);
                            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                        }
                    }
                } else{
                    event.reply(event.getUser().getAsMention() + " You didn't request this!").setEphemeral(true).queue();
                }
            } else {
                event.reply(event.getUser().getAsMention() + " Stop spamming! Please wait 30 seconds.").queue();
            }

            //Clean map of expired timestamps
            //NOTE: Doing a while loop is faster than Collection.removeif by a few milliseconds since it only iterates through expired elements
            while(entry.hasNext()){
                Map.Entry<String,Long> actualEntry = entry.next();
                if(actualEntry.getValue()+30000<System.currentTimeMillis()){
                    entry.remove();
                }
            }
        }, bot.service);
    }
}
