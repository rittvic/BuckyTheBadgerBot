package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.pojo.Professor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information about a professor from ratemyprofessor.com
 * Calls from HTTPClient.java
 */
public class ProfCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(ProfCommand.class);
    public ProfCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "professor";
        this.description = "Display professor information";
        this.explanation = """
                 `e.g., <Hobbes>, <Boya Wen>, <Vermillion>`
                 Searches for a professor and displays the following information:\s
                 - Department
                 - Average Rating
                 - Total Ratings
                 - Would Take Again
                 - Top Tags
                 - Courses Taught""";
        this.args.add(new OptionData(OptionType.STRING, "prof", "Professor name", true));

    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", ProfCommand.class.getSimpleName());
            long startTime = System.nanoTime();
            String userID = event.getUser().getId();
            String profName = Objects.requireNonNull(event.getOption("prof")).getAsString();

            //Assigns the Professor instance to the results of the HTTP request
            Professor prof = bot.RMPClient.profLookup(profName);

            if (prof.getDoesExist() && !prof.getFallback()) {
                StringBuilder topTags = new StringBuilder();
                StringBuilder coursesTaught = new StringBuilder();
                String coursesTaughtDisplay;
                String tagsDisplay;

                //Obtain the Professor's reviews
                if (!prof.getTopFiveTags().isEmpty()) {
                    for (String tag : prof.getTopFiveTags()) {
                        topTags.append("`").append(tag).append("`").append("\n");
                    }
                } else {
                    topTags.append("None");
                }

                //Change the Tags field title based on number of tags
                tagsDisplay = prof.getTopFiveTags().size() < 5 ? "Tags" : "Top Tags";

                //Obtain the Professor's courses taught
                if (!prof.getCoursesTaught().isEmpty()) {
                    for (String course : prof.getCoursesTaught()) {
                        coursesTaught.append("`").append(course).append("`").append("\n");
                    }
                } else {
                    coursesTaught.append("None");
                }

                //Change the Courses Taught field title based on number of courses taught
                coursesTaughtDisplay = prof.getCoursesTaught().size() == 10 ? "Popular Courses Taught" : "Courses Taught";

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle((prof.getFirstName() + " " + prof.getLastName()), "https://www.ratemyprofessors.com/ShowRatings.jsp?tid=" + prof.getLegacyId())
                        .setColor(Color.red)
                        .addField("Department", prof.getDepartment(), false)
                        .addField("Average Rating", prof.getAvgRating() + "/5", false)
                        .addField("Average Difficulty", prof.getAvgDifficulty() + "/5", false)
                        .addField("Total Ratings", String.valueOf(prof.getNumRating()), false)
                        .addField("Would Take Again", String.valueOf(prof.getTakeAgainPercentage()).replace("-1", "N/A") + "%", false)
                        .addField(tagsDisplay, topTags.toString(), false)
                        .addField(coursesTaughtDisplay, coursesTaught.toString(), false);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb.setFooter("This took " + duration + " ms to respond.");

                MessageCreateBuilder message = new MessageCreateBuilder();
                message.addEmbeds(eb.build());
                event.reply(message.build()).queue();


            } else if (prof.getDoesExist() && prof.getFallback()) {
                event.reply("Professor " + "\"" + prof.getFirstName() + " " + prof.getLastName() + "\"" + " does not teach at UW-Madison!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();

            } else {
                event.reply("Professor " + "\"" + profName + "\"" + " does not exist!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();
            }
        }, bot.service);
    }
}

