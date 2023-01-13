package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.data.Course;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information of a course at UW-Madison
 */
public class CourseCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(CourseCommand.class);
    public CourseCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "course";
        this.description = "Search for a course";
        this.explanation = """
                `e.g., <COMP SCI 577>, <Biology>, <102>, <Machine Learning>`
                Searches for the specified course (or the top result) and displays the following information (in order):\s
                 - Course Subject, Number and Title
                 - Course Description
                 - Cumulative GPA
                 - Credits
                 - Requisites
                 - Course Designation
                 - Repeatable For Credit
                 - Last Taught
                 - Cross-listed Subjects (if any)
                NOTE: Cross-listed course querying is currently not supported (e.g., "COMP SCI/MATH 240").
                Additionally, abbreviated subject querying may not work as intended (e.g., "CS 240")""";
        this.args.add(new OptionData(OptionType.STRING, "course", "Course subject and number, and/or title", true));
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
                    String graphImgName = result.getSubjectAbbrev().replaceAll(" ","_") + "-" + result.getNumber() + ".png";
                    File gradeDistGraph = new File("./grade-dist-graphs" + File.separator + graphImgName);
                    if (gradeDistGraph.exists()) {
                        FileUpload uploadedGradeDistGraph = FileUpload.fromData(gradeDistGraph);
                        eb.setImage("attachment://" + graphImgName);
                        event.getHook().editOriginalEmbeds(eb.build()).setFiles(uploadedGradeDistGraph).queue();
                    }
                } else {
                    event.reply("No courses found. Try to be more specific.").queue();
                }
            } catch (Exception e) {
                logger.error("Could not fetch courses!",e);
                event.reply("An error has occurred. Unable to fetch courses...").queue();
            }
        }, bot.service);
    }
}
