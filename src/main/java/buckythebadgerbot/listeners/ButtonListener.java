package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.httpclients.Scraper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
            buttonResults.add(Button.secondary( (userID)+":"+result, result));
        }
        return buttonResults;
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
            String[] pressedArgs = event.getComponentId().split(":");
            String buttonName = event.getButton().getLabel();
            String eventUserID = event.getUser().getId();

            //If the user didn't press the same button within 30 seconds, the task executes and the user gets added to the cooldown after.
            //Otherwise, they get a message saying to wait until 30 seconds has passed since the initial button press.
            if (!coolDownChecker.containsKey(eventUserID+":"+pressedArgs[1]) || System.currentTimeMillis() > coolDownChecker.get(eventUserID+":"+pressedArgs[1]) + 30000){
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
                        event.getHook().sendMessage("This course" + " (" +courseSearch.get(3).toUpperCase() + " " + courseSearch.get(1) + " - " + "'"+courseSearch.get(4)+"')" + " is no longer taught!").queue();
                    }
                } else {
                    event.getHook().sendMessage("No courses found.").queue();
                }

                //Adds the user and the button they pressed to the cooldown
                coolDownChecker.put(eventUserID+":"+pressedArgs[1], System.currentTimeMillis());

                } else {
                event.reply(event.getUser().getAsMention() + " Slow down! You already requested \"" + buttonName + "\".. Please wait 30 seconds.").queue();
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