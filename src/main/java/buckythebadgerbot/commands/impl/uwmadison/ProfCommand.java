package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.listeners.StringSelectListener;
import buckythebadgerbot.data.Professor;
import buckythebadgerbot.data.StudentRating;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Command that retrieves information about a professor from ratemyprofessor.com
 * Calls from HTTPClient.java
 */
public class ProfCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(ProfCommand.class);
    public ProfCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "professor";
        this.description = "Search for a professor";
        this.explanation = """
                 `e.g., <Hobbes>, <Boya Wen>, <Vermillion>`
                 Searches for a professor and displays the following information (in order):\s
                 - Department
                 - Average Rating
                 - Total Ratings
                 - Would Take Again Percentage
                 - Top Tags
                 - Top Courses Taught
                 You can also view student ratings for every course taught by the professor.""";
        this.args.add(new OptionData(OptionType.STRING, "professor", "Professor's name", true));

    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow asynchronous executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", ProfCommand.class.getSimpleName());
            long startTime = System.nanoTime();
            String uuid = event.getUser().getId() + ":" + UUID.randomUUID();
            String profName = Objects.requireNonNull(event.getOption("professor")).getAsString();
            //Assigns the Professor instance to the results of the HTTP request
            Professor prof = bot.rateMyProfessorClient.getProf(profName);
            if (prof.getDoesExist() && !prof.getFallback()) {
                StringBuilder topTags = new StringBuilder();
                StringBuilder coursesTaught = new StringBuilder();
                String coursesTaughtDisplay;
                String tagsDisplay;

                //Obtain the Professor's reviews
                if (!prof.getTopFiveTags().isEmpty()) {
                    for (String tag : prof.getTopFiveTags()) {
                        topTags.append("`").append(tag.strip()).append("`").append("\n");
                    }
                } else {
                    topTags.append("None");
                }

                //Change the Tags field title based on number of tags
                tagsDisplay = prof.getTopFiveTags().size() < 5 ? "Tags" : "Top Tags";

                //Obtain the Professor's courses taught
                if (!prof.getCoursesTaught().isEmpty()) {
                    for (String course : prof.getCoursesTaught()) {
                        coursesTaught.append("`").append(course.strip()).append("`").append("\n");
                    }
                    //Add every course as an String Select Option
                    StringSelectListener.sendStringSelectOptions(uuid,prof.getFirstName() + " " + prof.getLastName(),prof.getCoursesTaught());
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
                        .addField("Would Take Again", String.valueOf(prof.getWouldTakeAgainPercent()).replace("-1.0", "N/A") + "%", false)
                        .addField(tagsDisplay, topTags.toString(), false)
                        .addField(coursesTaughtDisplay, coursesTaught.toString(), false);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb.setFooter("This took " + duration + " ms to respond.");

                MessageCreateBuilder message = new MessageCreateBuilder();
                message.addEmbeds(eb.build());

                if (!prof.getCoursesTaught().isEmpty()){
                    message.addActionRow(Button.of(ButtonStyle.PRIMARY,uuid + ":" + "studentRatings" + ":" + prof.getRegularId(),"See Student Ratings"));
                }
                event.reply(message.build()).queue();
                //Disable the buttons after 10 minutes starting the execution of slash command
                event.getHook().editOriginalComponents(message.getComponents().get(0).asDisabled()).queueAfter(10, TimeUnit.MINUTES);

            } else if (prof.getDoesExist() && prof.getFallback()) {
                event.reply("Professor " + "\"" + prof.getFirstName() + " " + prof.getLastName() + "\"" + " does not teach at UW-Madison!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();

            } else {
                event.reply("Professor " + "\"" + profName + "\"" + " does not exist!" + " (Note: If this is inaccurate, try to be more specific or blame RMP)").queue();
            }
        }, bot.service);
    }

    /**
     * To generate embeds for student ratings paginated menu
     * @param ratings the list of student ratings
     * @param profName the name professor that the student ratings correspond to
     * @param duration the duration of the API call
     * @return a list of MessageEmbeds
     */
    public static ArrayList<MessageEmbed> buildMenu(ArrayList<StudentRating> ratings, String profName, long duration) {
        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        for (int i = 0; i < ratings.size(); i++) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ratings.get(i).getCourse() + " - " + " Student Rating (" + (i + 1) + "/" + ratings.size() + ")")
                    .setDescription("This rating was written on " + "<t:" + ratings.get(i).getDate() + ":f>")
                    .setColor(Color.decode(ratings.get(i).getRatingQuality().hexColor))
                    .addField("Professor", profName, false)
                    .addField("Quality", String.valueOf(ratings.get(i).getQuality()), true)
                    .addField("Difficulty", String.valueOf(ratings.get(i).getDifficulty()), true)
                    .addField("For Credit", ratings.get(i).isForCredit(), true)
                    .addField("Attendance", ratings.get(i).getAttendance(), true)
                    .addField("Would Take Again", ratings.get(i).getWouldTakeAgain(), true)
                    .addField("Grade", ratings.get(i).getGrade(), true)
                    .addField("Textbook", ratings.get(i).getTextbookUse(), true)
                    .addField("Comment", ratings.get(i).getComment(), false);
            StringBuilder tagDisplay = new StringBuilder();
            if (ratings.get(i).getTags() != null) {
                for (String tag : ratings.get(i).getTags()) {
                    tagDisplay.append("`").append(tag.strip()).append("`").append("\n");
                }
            } else {
                tagDisplay.append("None");
            }
            embed.addField("Tags", tagDisplay.toString(), true);
            embed.setImage(ratings.get(i).getRatingQuality().imageUrl);
            embed.addField("Helpful/Not Helpful", ":thumbsup:" + " " + ratings.get(i).getThumbsUpTotal()
                    + " " + ":thumbsdown:" + " " + ratings.get(i).getThumbsDownTotal(), false);
            embed.setFooter("This took " + duration + " ms to respond.");
            embeds.add(embed.build());
        }
        return embeds;
    }
}

