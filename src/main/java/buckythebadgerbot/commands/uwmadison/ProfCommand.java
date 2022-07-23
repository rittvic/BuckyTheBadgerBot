package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.pojo.Professor;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
            String profName = Objects.requireNonNull(event.getOption("prof")).getAsString();

            //Assigns the Professor instance to the results of the HTTP request
            Professor prof = bot.RMPClient.profInfo(profName);

            if(prof.getDoesExist() && !prof.getFallback()){
                StringBuilder topReviews = new StringBuilder();
                StringBuilder coursesTaught = new StringBuilder();
                String coursesTaughtDisplay;

                //Obtain the Professor's reviews
                if(!prof.getTopFiveReviews().isEmpty()) {
                    for (String tag : prof.getTopFiveReviews()) {
                        topReviews.append("`").append(tag).append("`").append("\n");
                    }
                } else{
                    topReviews.append("None");
                }

                //Obtain the Professor's courses taught
                if(!prof.getCoursesTaught().isEmpty()) {
                    for (String course : prof.getCoursesTaught()) {
                        coursesTaught.append("`").append(course).append("`").append("\n");
                    }
                } else{
                    coursesTaught.append("None");
                }

                //Change the field title based on number of courses taught
                if(prof.getCoursesTaught().size()==10){
                    coursesTaughtDisplay = "Popular Courses Taught";
                } else {
                    coursesTaughtDisplay = "Courses Taught";
                }

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle((prof.getFirstName() + " " + prof.getLastName()), "https://www.ratemyprofessors.com/ShowRatings.jsp?tid=" + prof.getLegacyId())
                        .setColor(Color.red)
                        .addField("Department", prof.getDepartment(), false)
                        .addField("Average Rating", prof.getAvgRating()+"/5", false)
                        .addField("Average Difficulty", prof.getAvgDifficulty()+"/5", false)
                        .addField("Total Ratings", String.valueOf(prof.getNumRating()), false)
                        .addField("Would Take Again", String.valueOf(prof.getTakeAgainPercentage()).replace("-1", "N/A") +"%", false)
                        .addField("Top Tags", topReviews.toString(), false)
                        .addField(coursesTaughtDisplay, coursesTaught.toString(), false);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb.setFooter("This took " + duration + " ms to respond.");
                event.replyEmbeds(eb.build()).queue();

            } else if(prof.getDoesExist() && prof.getFallback()){
                event.reply("Professor " + "\"" + prof.getFirstName() + " " + prof.getLastName() +"\"" + " does not teach at UW-Madison!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();

            } else{
                event.reply("Professor " + "\"" + profName +"\"" + " does not exist!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();
            }
        }, bot.service);
    }
}

