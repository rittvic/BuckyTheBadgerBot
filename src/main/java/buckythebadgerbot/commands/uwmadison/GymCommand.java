package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.listeners.ButtonListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
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
        this.description = "Display live usage for every gym equipment at the Nicholas Recreation Center and the Shell";
        this.explanation = """
                Check live usages for gym equipments at the Nicholas Recreation Center and the Shell.""";
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", GymCommand.class.getSimpleName());
            //Create an ArrayList of embeds by calling the HTTP client's gymLookup() method
            ArrayList<MessageEmbed> gymEmbeds = buildMenu(bot.client.gymLookup());
            if (!gymEmbeds.isEmpty()){
                //Send a paginated menu
                ReplyCallbackAction action = event.replyEmbeds(gymEmbeds.get(0));
                if (gymEmbeds.size() > 1){
                    ButtonListener.sendPaginatedMenu(event.getUser().getId(), action, gymEmbeds);
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
    private ArrayList<MessageEmbed> buildMenu(ArrayList<HashMap<String, String>> gymInformation){

        //Create an ArrayList of embeds
        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        if (gymInformation != null){
            //Iterate through every HashMap in the ArrayList of gym information
            for (HashMap<String, String> entry : gymInformation){
                //Create a new embed
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(entry.keySet().stream().findFirst().get().split("\\|")[0])
                        .setColor(Color.red)
                        .setFooter("The displayed timestamps are local.");

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

