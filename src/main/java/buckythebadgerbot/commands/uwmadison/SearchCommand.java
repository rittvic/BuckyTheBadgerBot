package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.json.JsonObject;
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
            ArrayList<JsonObject> results = bot.madGradesClient.courseQuery(courseInfo);
            if (!results.isEmpty()){
                StringBuilder resultsDisplay = new StringBuilder();
                ArrayList<String> buttonResults = new ArrayList<>();
                try {
                    for (JsonObject result : results) {
                        resultsDisplay.append("`").append(result.getJsonArray("subjects").getJsonObject(0).getJsonString("abbreviation").toString().replaceAll("\"", "")).append(" ").append(result.getJsonNumber("number")).append(" - ").append(result.getJsonString("name").toString().replaceAll("\"", "")).append("`").append("\n");
                        buttonResults.add(result.getJsonArray("subjects").getJsonObject(0).getJsonString("abbreviation").toString().replaceAll("\"", "") + " " + result.getJsonNumber("number"));
                    }
                } catch(RuntimeException d){
                    event.getHook().sendMessage("No results found.").queue();
                    return;
                }
                String resultSizeDisplay;
                if(results.size()>=10){
                    resultSizeDisplay = "Showing the first " + results.size() + " results.";
                } else{
                    resultSizeDisplay = "Showing all " + results.size() + " results.";
                }

                //Builds the embed message to show the results
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Query: " + courseInfo)
                        .setColor(Color.red)
                        .setDescription(resultSizeDisplay)
                        .addField("Results: ", String.valueOf(resultsDisplay), false);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb.setFooter("This took " + duration + " ms to respond.");

               //Generate the buttons, one per result
                ArrayList<Button> buttonsToSend = bot.buttonListener.getButtons(buttonResults, userID);
                MessageBuilder message = new MessageBuilder(eb.build());

                //If there are 5 buttons or less, create a single ActionRow
                if (buttonsToSend.size()<=5){
                    message.setActionRows(ActionRow.of(buttonsToSend));
                    event.getHook().sendMessage(message.build()).queue();

                    //Disable the buttons after 10 minutes starting the execution of slash command
                    event.getHook().editOriginalComponents(ActionRow.of(buttonsToSend).asDisabled()).queueAfter(10, TimeUnit.MINUTES);
                } else{

                    //If there are more than 5 buttons, create two ActionRows and split the buttons between the rows
                    message.setActionRows(
                            (ActionRow.of(buttonsToSend.subList(0, 5))),
                            ActionRow.of((buttonsToSend.subList(5, buttonsToSend.size()))));
                    event.getHook().sendMessage(message.build()).queue();

                    //Disable the buttons after 10 minutes starting the execution of slash command
                    event.getHook().editOriginalComponents(
                            ActionRow.of(buttonsToSend.subList(0,5)).asDisabled(),
                           ActionRow.of(buttonsToSend.subList(5,buttonsToSend.size())).asDisabled()
                    ).queueAfter(10, TimeUnit.MINUTES);

                }
            } else{
                event.getHook().sendMessage("No results found.").queue();
            }
        }, bot.service);
    }
}
