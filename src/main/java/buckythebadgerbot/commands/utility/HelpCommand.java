package buckythebadgerbot.commands.utility;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Command that sends a help menu consisting information of all slash commands and other help information such as GitHub link.
 * NOTE: May choose to automate fetching all slash commands instead of hard-coding in the embed.
 */
public class HelpCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);

    public HelpCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "help";
        this.description = "Display all commands and other information";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", HelpCommand.class.getSimpleName());
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Help Menu")
                    .setColor(Color.red)
                    .setDescription("The slash commands:")
                    .addField("Course", """
                                    `/course <course>`\s
                                     `e.g., <CS 577>, <Biology>, <102>, <Machine Learning>`
                                     Searches for the specified course (or the top result) and displays the following information:\s
                                     - Course Description
                                     - Cumulative GPA
                                     - Credits
                                     - Requisites
                                     - Course Designation
                                     - Repeatable For Credit
                                     - Last Taught"""
                            ,false)
                    .addField("Professor", """
                                    `/professor <professor>`\s
                                     `e.g., <Hobbes>, <Boya Wen>, <Vermillion>`
                                     Searches for a professor and displays the following information:\s
                                     - Department
                                     - Average Rating
                                     - Total Ratings
                                     - Would Take Again
                                     - Top Tags
                                     - Courses Taught""",
                             false)
                    .addField("Search", """
                            `/search <course to query>`\s
                             `e.g., <Calculus>, <Amer Ind>, <Math 340>, <500>`\s
                             Queries through the courses and finds the best matches. It then generates buttons for each result.""", false)
                    .addField("Where I get the information from:",
                            """
                                    - api.madgrades.com\s
                                     - guide.wisc.edu\s
                                     - ratemyprofessors.com""", false)
                    .addField("Notice an issue or a bug? Want to make a suggestion?", "Make a request on " + "[Github](https://github.com/rittvic/BuckyTheBadgerBot)" + " or contact me via Discord: ||Betrayy#6834||", false);
            event.replyEmbeds(eb.build()).queue();
        }, bot.service);
    }
}