package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.utils.pagination.PaginationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves live usage for every gym equipment
 * Calls from HTTPClient.java
 */
public class GymCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(GymCommand.class);
    public GymCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "gym";
        this.description = "Check live usages for all gym equipments";
        this.explanation = """
                Displays live usages for every gym equipment at the Nicholas Recreation Center and the Shell.""";
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow asynchronous executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", GymCommand.class.getSimpleName());

            //Create an ArrayList of embeds by calling the HTTP client's gymLookup() method
            long startTime = System.nanoTime();
            ArrayList<HashMap<String,String>> gymInformation = bot.gymClient.getGymUsages();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;

            ArrayList<MessageEmbed> gymEmbeds = buildMenu(gymInformation, duration);
            if (!gymEmbeds.isEmpty()){
                //Send a paginated menu
                ReplyCallbackAction action = event.replyEmbeds(gymEmbeds.get(0));
                if (gymEmbeds.size() > 1){
                    PaginationUtils.sendPaginatedMenu(event.getUser().getId(), action, gymEmbeds);
                    return;
                }
                action.queue();
            } else{
                event.reply("Unable to retrieve the live gym usages at this moment!").queue();
            }
        }, bot.service);
    }

    /**
     * To generate embeds for the paginated menu
     * @param gymInformation the ArrayList with every embed to add onto the menu
     * @return an ArrayList of all embeds in the menu
     */
    private ArrayList<MessageEmbed> buildMenu(ArrayList<HashMap<String, String>> gymInformation, long duration){

        //Create an ArrayList of embeds
        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        if (gymInformation != null){
            //Iterate through every HashMap in the ArrayList of gym information
            for (HashMap<String, String> entry : gymInformation){
                //Create a new embed
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(entry.keySet().stream().findFirst().get().split("\\|")[0])
                        .setColor(Color.red)
                        .setFooter("This took " + duration + " ms to respond.")
                        .setTimestamp(Instant.now());

                //Iterate through every pair in the HashMap
                for (String key : entry.keySet()){
                    embed.addField(key.split("\\|")[1], entry.get(key), true);
                }

                //Store the embed in the ArrayList
                embeds.add(embed.build());
            }
        }
        return embeds;
    }
}

