package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.services.Scraper;
import buckythebadgerbot.utility.pagination.PaginationUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
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

    //Scheduler to disable buttons after a set period of time
    public static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(20);

    public ButtonListener(BuckyTheBadgerBot bot) {
        this.bot = bot;
    }

    /**
     * Gathers the buttons to send back to SearchCommand based on the results
     *
     * @param buttonContents an ArrayList of all the results (Course name and number)
     * @param uuid the user ID + random UUID of the user who initiated the SearchCommand
     * @return an ArrayList of Buttons to add onto Action Rows
     */
    public static ArrayList<Button> getButtons(ArrayList<String> buttonContents, String uuid, String buttonArgument,ButtonStyle buttonStyle) {
        ArrayList<Button> buttonResults = new ArrayList<>();
        for (String content : buttonContents) {
            buttonResults.add(Button.of(buttonStyle,uuid + ":" + buttonArgument + ":" + content, content));
        }
        return buttonResults;
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
            //[0] User ID [1] UUID [2] Argument ID (E.g, pagination or courseSearch) [3] Argument Label (e.g, "next", Course to query for, Professor's Name)
            String[] pressedArgs = event.getComponentId().split(":");
            //Store the user ID of who pressed the button
            String eventUserID = event.getUser().getId();

            //For the course command - if the user didn't press the same button within 30 seconds, the task executes and the user gets added to the cooldown after.
            //Otherwise, they get a message saying to wait until 30 seconds has passed since the initial button press.
            if (pressedArgs[2].equals("courseSearch")) {
                if (!BuckyTheBadgerBot.coolDownChecker.containsKey(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3])
                        || System.currentTimeMillis() > BuckyTheBadgerBot.coolDownChecker.get(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3]) + 30000) {
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
                    BuckyTheBadgerBot.coolDownChecker.put(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3], System.currentTimeMillis());
                } else {
                    event.reply("Stop spamming! You already selected `" + event.getButton().getLabel() + "` recently. Please wait 30 seconds....").setEphemeral(true).queue();
                }
                //Clean map of expired timestamps
                //NOTE: Doing a while loop is faster than Collection.removeif by a few milliseconds since it only iterates through expired elements
                Iterator<Map.Entry<String, Long>> iterator = BuckyTheBadgerBot.coolDownChecker.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    if (entry.getValue() + 30000 < System.currentTimeMillis()) {
                        iterator.remove();
                    }
                }
            } else if (pressedArgs[2].equals("pagination")) {
                //Check if the user requested the original menu
                if (pressedArgs[0].equals(eventUserID)) {
                    //Update the buttons according to the pressed arg
                    PaginationUtility.updatePaginationButtons(pressedArgs[0] + ":" + pressedArgs[1], pressedArgs[3], event);
                } else {
                    event.reply("You didn't request this!").setEphemeral(true).queue();
                }
            } else if (pressedArgs[2].equals("studentRatings")) {
                String uuid = pressedArgs[0] + ":" + pressedArgs[1];
                if (pressedArgs[0].equals(eventUserID)) {
                    List<SelectOption> selectOptions = StringSelectListener.StringSelectOptions.get(uuid);

                    StringSelectMenu menu = StringSelectMenu.create(uuid + ":" + "studentRatings" + ":" + pressedArgs[3])
                            .setPlaceholder("Select a course")
                            .setRequiredRange(1, 1)
                            .addOptions(selectOptions)
                            .build();

                    event.reply("Select a course you want to see student ratings for:").addActionRow(menu).queue(interactionHook
                            -> {
                        Runnable task = () -> interactionHook.editOriginalComponents(ActionRow.of(menu).asDisabled()).queue();
                        scheduledExecutor.schedule(task, 10, TimeUnit.MINUTES);
                    });
                    event.editButton(event.getButton().asDisabled()).queue();

                } else {
                    event.reply("You didn't request this!").setEphemeral(true).queue();
                }
            }
        }, bot.service);
    }
}
