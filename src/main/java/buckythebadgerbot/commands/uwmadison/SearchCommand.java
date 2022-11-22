package buckythebadgerbot.commands.uwmadison;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.listeners.ButtonListener;
import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
        this.description = "Queries courses and displays the top results";
        this.explanation = """
                `e.g., <Calculus>, <Amer Ind>, <Math 340>, <500>`\s
                Queries through the courses and finds the best matches. It then generates buttons for each result.""";
        this.args.add(new OptionData(OptionType.STRING, "query", "Search for courses", true));
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
            event.deferReply().queue();
            long startTime = System.nanoTime();
            String userID = event.getUser().getId();
            String courseInfo = Objects.requireNonNull(event.getOption("query")).getAsString();

            //Fetches the results by calling the API through the HTTP client.
            ArrayList<JsonNode> results = (ArrayList<JsonNode>) bot.madGradesClient.courseQuery(courseInfo);
            System.out.println("reached here");
            //Add every result for display
            if (!results.isEmpty()) {
                StringBuilder resultsDisplay = new StringBuilder();
                ArrayList<String> buttonResults = new ArrayList<>();
                try {
                    for (JsonNode result : results) {
                        resultsDisplay.append("`").append(result.withArray("subjects").get(0).get("abbreviation").asText()).append(" ").append(result.get("number")).append(" - ").append(result.get("name").asText()).append("`").append("\n");
                        buttonResults.add(result.withArray("subjects").get(0).get("abbreviation").asText() + " " + result.get("number"));
                    }
                } catch (Exception e) {
                    event.getHook().sendMessage("No results found.").queue();
                    return;
                }

                //To display the result size
                String resultSizeDisplay = results.size() >= 10 ? "Showing the first " + results.size() + " results." : "Showing all " + results.size() + " results.";

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Query: " + courseInfo)
                        .setColor(Color.red)
                        .setDescription(resultSizeDisplay)
                        .addField("Results: ", String.valueOf(resultsDisplay), false);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb.setFooter("This took " + duration + " ms to respond.");

                MessageCreateBuilder message = new MessageCreateBuilder();
                message.addEmbeds(eb.build());

                //Generate the buttons, one per result
                ArrayList<Button> buttonsToSend = ButtonListener.getButtons(buttonResults, userID, "courseSearch", ButtonStyle.SECONDARY);

                //If there are 5 buttons or less, create a single ActionRow
                if (buttonsToSend.size() <= 5) {
                    message.addActionRow(buttonsToSend);
                    event.getHook().sendMessage(message.build()).queue();

                    //Disable the buttons after 10 minutes starting the execution of slash command
                    event.getHook().editOriginalComponents(ActionRow.of(buttonsToSend).asDisabled()).queueAfter(10, TimeUnit.MINUTES);
                } else {
                    //If there are more than 5 buttons, create two ActionRows and split the buttons between the rows
                    message.addActionRow(buttonsToSend.subList(0, 5));
                    message.addActionRow(buttonsToSend.subList(5, buttonsToSend.size()));
                    event.getHook().sendMessage(message.build()).queue();

                    //Disable the buttons after 10 minutes starting the execution of slash command
                    event.getHook().editOriginalComponents(
                            ActionRow.of(buttonsToSend.subList(0, 5)).asDisabled(),
                            ActionRow.of(buttonsToSend.subList(5, buttonsToSend.size())).asDisabled()
                    ).queueAfter(10, TimeUnit.MINUTES);
                }
            } else {
                event.getHook().sendMessage("No results found.").queue();
            }
        }, bot.service);
    }
}
