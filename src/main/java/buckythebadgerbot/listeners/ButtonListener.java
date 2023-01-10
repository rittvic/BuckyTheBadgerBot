package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.data.Course;
import buckythebadgerbot.utils.pagination.PaginationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Listens for button clicks and handles them accordingly
 * Has a cooldown system (30s) in place to prevent spams
 */
public class ButtonListener extends ListenerAdapter {

    private final BuckyTheBadgerBot bot;
    private static final Logger logger = LoggerFactory.getLogger(ButtonListener.class);

    //Scheduler to disable buttons after a set period of time
    //public static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(20);

    public ButtonListener(BuckyTheBadgerBot bot) {
        this.bot = bot;
    }

    /**
     * Generates buttons, 5 per row (since that's the limit set by the Discord API)
     * @param uuid the user ID + random UUID of the user
     * @param buttonArgument The argument ID of the Button for listener event
     * @param buttonLabels an ArrayList of button labels (strings) for Button conversion
     * @param buttonStyle the specified style of each Button
     * @param action the original event that called for the button generation
     */
    public static void generateButtons(String uuid, String buttonArgument, ArrayList<String> buttonIds, ArrayList<String> buttonLabels, ButtonStyle buttonStyle, ReplyCallbackAction action) {
        if (buttonLabels.size() > 25) {
            throw new IllegalArgumentException("Cannot have more than 25 buttons!");
        }
        ArrayList<ActionRow> actionRows = new ArrayList<>();
        int numActionRows = (buttonLabels.size() - 1) / 5 + 1;
        ArrayList<Button> buttonsRow;
        for (int i = 0; i < numActionRows; i++) {
            buttonsRow = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                if (!buttonLabels.isEmpty()) {
                    String buttonLabel = buttonLabels.remove(0);
                    String buttonId = buttonIds.remove(0);
                    buttonsRow.add(Button.of(buttonStyle,uuid + ":" + buttonArgument + ":" + buttonId, buttonLabel));
                } else {
                    break;
                }
            }
            actionRows.add(ActionRow.of(buttonsRow));
        }
        //After setting the message components to the newly generated ActionRows and executing it, call the method to disable the ActionRows (buttons) after 10 minutes
        action.setComponents(actionRows).queue(interactionHook -> disableButtons(actionRows,interactionHook));
    }

    /**
     * Disable buttons after 10 minutes
     * @param actionRows The ActionRows (buttons) to disable
     * @param hook the message hook pointing to the original message; this will be the message to disable the buttons from
     */
    public static void disableButtons(ArrayList<ActionRow> actionRows, InteractionHook hook) {
        Runnable task = () -> {
            List<ActionRow> newActionRows = actionRows.stream().map(ActionRow::asDisabled).collect(Collectors.toList());
            hook.editOriginalComponents(newActionRows).queue();
        };
        BuckyTheBadgerBot.scheduledExecutor.schedule(task, 10, TimeUnit.MINUTES);
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

            //For the search command - if the user didn't press the same button within 30 seconds, the task executes and the user gets added to the cooldown afterwards.
            //Otherwise, they get a message saying to wait until 30 seconds has passed since the initial button press.
            if (pressedArgs[2].equals("courseSearch")) {
                if (!BuckyTheBadgerBot.coolDownChecker.containsKey(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3])
                        || System.currentTimeMillis() > BuckyTheBadgerBot.coolDownChecker.get(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3]) + 30000) {
                    long startTime = System.nanoTime();
                    String courseQuery = pressedArgs[3];
                    //E.g. COMP SCI-T300
                    String[] courseQueryArgs = courseQuery.split("-T");
                    String sqlQuery = "SELECT * FROM courses WHERE subject_abbrev = '" + courseQueryArgs[0] + "' AND number = '" + courseQueryArgs[1] + "';";
                    try {
                        List<Course> courses = bot.getDatabase().getRepository("courses").read(sqlQuery);
                        Course result = courses.get(0);
                        EmbedBuilder eb = new EmbedBuilder()
                                .setTitle(result.getSubjectAbbrev() + " " + result.getNumber() + " — " +result.getTitle())
                                .setColor(Color.RED)
                                .addField("Cumulative GPA", result.getCumulativeGpa() != null ? result.getCumulativeGpa().toString() : "N/A", false)
                                .addField("Credits", result.getCredits() != null ? result.getCredits() : "None", false)
                                .addField("Requisites", result.getRequisites() != null ? result.getRequisites() : "None", false)
                                .addField("Course Designation",result.getCourseDesignation() != null ? result.getCourseDesignation() : "None", false)
                                .addField("Repeatable For Credit", result.getRepeatable() != null ? result.getRepeatable() : "None", false)
                                .addField("Last Taught", result.getLastTaught() != null ? result.getLastTaught() : "None", false)
                                .addField("Cross-listed Subjects", result.getCrosslistSubjects() != null ? result.getCrosslistSubjects() + " " + result.getNumber() : "None", false);
                        if (result.getDescription() != null) {
                            eb.setDescription(result.getDescription());
                        }
                        long endTime = System.nanoTime();
                        long duration = (endTime - startTime) / 1000000;
                        eb.setFooter("This took " + duration + " ms to respond.");
                        event.replyEmbeds(eb.build()).queue();
                    } catch (Exception e) {
                        logger.error("Could not fetch courses! {}",e.toString());
                        event.reply("An error has occurred. Unable to fetch courses...").queue();
                    }
                    //Adds the user and the button they pressed to the cooldown
                    BuckyTheBadgerBot.coolDownChecker.put(eventUserID + ":" + pressedArgs[1] + ":" + pressedArgs[3], System.currentTimeMillis());
                } else {
                    event.reply("Stop spamming! You already selected `" + event.getButton().getLabel() + "` recently. Please wait 30 seconds...").setEphemeral(true).queue();
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
                    PaginationUtils.updatePaginationButtons(pressedArgs[0] + ":" + pressedArgs[1], pressedArgs[3], event);
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
                        BuckyTheBadgerBot.scheduledExecutor.schedule(task, 10, TimeUnit.MINUTES);
                    });
                    event.editButton(event.getButton().asDisabled()).queue();

                } else {
                    event.reply("You didn't request this!").setEphemeral(true).queue();
                }
            }
        }, bot.service);
    }
}
