package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.data.Course;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
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
        this.explanation = """
                `e.g., <COMP SCI 577>, <Biology>, <102>, <Machine Learning>`
                 Searches for the specified course (or the top result) and displays the following information:\s
                 - Course Description
                 - Cumulative GPA
                 - Credits
                 - Requisites
                 - Course Designation
                 - Repeatable For Credit
                 - Last Taught
                 NOTE: The cumulative gpa for cross-listed courses are combined since the individual courses may not have its own cumulative gpa.
                 Additionally, cross-listed course querying is not currently supported (i.e. COMP SCI/MATH 240)""";
        this.args.add(new OptionData(OptionType.STRING, "course", "Course subject and number and/or title", true));
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow asynchronous executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", CourseCommand.class.getSimpleName());
            long startTime = System.nanoTime();
            String courseQuery = event.getOption("course").getAsString();
            String sqlQuery = "SELECT *," +
                    " ts_rank_cd(full_subject_name_number_idx_col, query) AS rank_full," +
                    " ts_rank_cd(subject_abbrev_number_idx_col, query) AS rank_abbrev," +
                    " similarity(title, '" + courseQuery + "') AS sml\n" +
                    " FROM courses, plainto_tsquery('simple', '" + courseQuery + "') query\n" +
                    " WHERE subject_abbrev_number_idx_col @@ query " +
                    " OR full_subject_name_number_idx_col @@ query " +
                    " OR title % '" + courseQuery + "'\n" +
                    " ORDER BY rank_full DESC, rank_abbrev DESC, sml DESC\n" +
                    " LIMIT 1;";
            try {
                List<Course> courses = bot.getDatabase().getRepository("courses").read(sqlQuery);
                if (!courses.isEmpty()) {
                    Course result = courses.get(0);
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle(result.getCrosslistedSubjectsWithNumber() + " — " +result.getTitle())
                            .setColor(Color.RED)
                            .setDescription(result.getDescription() != null ? result.getDescription() : "N/A")
                            .addField("Cumulative GPA", result.getCumulativeGpa() != null ? result.getCumulativeGpa().toString() : "N/A", false)
                            .addField("Credits", result.getCredits() != null ? result.getCredits() : "N/A", false)
                            .addField("Requisites", result.getRequisites() != null ? result.getRequisites() : "N/A", false)
                            .addField("Course Designation",result.getCourseDesignation() != null ? result.getCourseDesignation() : "N/A", false)
                            .addField("Repeatable For Credit", result.getRepeatable() != null ? result.getRepeatable() : "N/A", false)
                            .addField("Last Taught", result.getLastTaught() != null ? result.getLastTaught() : "N/A", false);
                    long endTime = System.nanoTime();
                    long duration = (endTime - startTime) / 1000000;
                    eb.setFooter("This took " + duration + " ms to respond.");
                    event.replyEmbeds(eb.build()).queue();
                } else {
                    event.reply("No courses found. Try to be more specific.").queue();
                }
            } catch (Exception e) {
                logger.error("Could not fetch courses! {}",e.toString());
                event.reply("An error has occurred. Unable to fetch courses...").queue();
            }
        }, bot.service);
    }
}
