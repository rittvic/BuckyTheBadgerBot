package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.data.Course;
import buckythebadgerbot.listeners.ButtonListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command that queries courses and displays the top ten (or all if less) results.
 * Generates buttons for the results, which you can click on to fetch the course information
 * Uses api.madgrades.com for queries
 * Calls from HTTPClient.java
 */
public class SearchCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(SearchCommand.class);

    public SearchCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "search";
        this.description = "Queries the courses and displays the top ten results";
        this.explanation = """
                `e.g., <Calculus>, <Amer Ind>, <Math 340>, <500>`\s
                Queries courses based on the user input and returns the best matches. You can click on every result through the generated buttons.
                NOTE: Cross-listed course querying is currently not supported (e.g., "COMP SCI/MATH 240").
                Additionally, abbreviated subject querying may not work as intended (e.g., "CS 240")""";
        this.args.add(new OptionData(OptionType.STRING, "query", "Search for courses by subject, number, and/or title", true));
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", SearchCommand.class.getSimpleName());
            long startTime = System.nanoTime();
            String uuid = event.getUser().getId() + ":" + UUID.randomUUID();
            String courseQuery = event.getOption("query").getAsString();
            String sqlQuery = "SELECT *," +
                    " ts_rank_cd(full_subject_name_number_idx_col, query) AS rank_full," +
                    " ts_rank_cd(subject_abbrev_number_idx_col, query) AS rank_abbrev," +
                    " word_similarity('" + courseQuery + "', title) AS sml\n" +
                    " FROM courses, plainto_tsquery('simple', '" + courseQuery + "') query\n" +
                    " WHERE subject_abbrev_number_idx_col @@ query " +
                    " OR full_subject_name_number_idx_col @@ query " +
                    " OR '" + courseQuery + "' <% title\n" +
                    " ORDER BY rank_full DESC, rank_abbrev DESC, sml DESC\n" +
                    " LIMIT 10;";
            try {
                List<Course> courses = bot.getDatabase().getRepository("courses").read(sqlQuery);
                if (!courses.isEmpty()) {
                    StringBuilder results = new StringBuilder();
                    ArrayList<String> buttonResults = new ArrayList<>();
                    ArrayList<String> buttonIds = new ArrayList<>();
                    for (Course course : courses) {
                        results.append("`").append(course.getSubjectAbbrev()).append(" ").append(course.getNumber()).append(" — ").append(course.getTitle()).append("`").append("\n");
                        buttonIds.add(course.getSubjectAbbrev() + "-T" + course.getNumber());
                        buttonResults.add(course.getSubjectAbbrev() + " " + course.getNumber());
                    }
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Query: " + courseQuery)
                            .setColor(Color.red)
                            .addField("Results: ", results.toString(), false)
                            .setDescription(courses.size() == 10 ? "Showing the first 10 results." : "Showing all " + courses.size() + " results.");
                    long endTime = System.nanoTime();
                    long duration = (endTime - startTime) / 1000000;
                    eb.setFooter("This took " + duration + " ms to respond.");
                    MessageCreateBuilder message = new MessageCreateBuilder();
                    message.addEmbeds(eb.build());
                    ReplyCallbackAction action = event.reply(message.build());
                    ButtonListener.generateButtons(uuid,"courseSearch", buttonIds, buttonResults,ButtonStyle.SECONDARY, action);
                } else {
                    event.reply("No results found. Try to be more specific.").queue();
                }
            } catch (Exception e) {
                logger.error("Could not fetch courses! {}",e.toString());
                event.reply("An error has occurred. Unable to fetch courses...").queue();
            }
        }, bot.service);
    }
}
