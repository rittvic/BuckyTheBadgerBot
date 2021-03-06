package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.httpclients.Scraper;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information about a course from api.madgrades.com and guide.wisc.edu
 * Calls from HTTPClient.java and scrapes from Scraper.java
 */
public class CourseCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CourseCommand.class);
    public CourseCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "course";
        this.description = "Display course information";
        this.args.add(new OptionData(OptionType.STRING, "course", "Course name and/or number", true));
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", CourseCommand.class.getSimpleName());
            long startTime = System.nanoTime();
            ArrayList<String> courseSearch;
            ArrayList<String> courseInformation;
            String averageGPA;
            String courseNameAndNumber = Objects.requireNonNull(event.getOption("course")).getAsString();

            //Fetches the UUID, Course Number, Course Code, Course Subject, and Course Name
            courseSearch = bot.madGradesClient.courseLookUp(courseNameAndNumber);
            if (!courseSearch.isEmpty()) {

                //Calculates the average GPA of the course
                averageGPA = bot.madGradesClient.courseAverageGPA(courseSearch.get(0));

                //Fetches rest of the course information such as requisites, description, designations, etc
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
                    event.replyEmbeds(eb.build()).queue();
                } else{
                    event.reply("This course" + " (" +courseSearch.get(3) + " " + courseSearch.get(1) + " - " + "'"+courseSearch.get(4)+"')" + " is no longer taught!").queue();
                }
            } else {
                event.reply("No courses found.").queue();
            }
        }, bot.service);
    }
}