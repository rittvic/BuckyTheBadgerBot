package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.commands.utility.enums.DiningMenuImage;
import buckythebadgerbot.listeners.ButtonListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information about a professor from ratemyprofessor.com
 * Calls from HTTPClient.java
 */
public class DiningMenuCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(DiningMenuCommand.class);

    public DiningMenuCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "diningmenu";
        this.description = "Display dining menu";
        this.args.add(new OptionData(OptionType.STRING, "dining-market", "choose a dining market", true)
                .addChoice("Rheta's Market","rhetas-market")
                .addChoice("Gordon Avenue Market","gordon-avenue-market")
                .addChoice("Lowell Market","lowell-market")
                .addChoice("Liz's Market","lizs-market")
                .addChoice("Carson's Market","carsons-market")
                .addChoice("Four Lakes Market","four-lakes-market"));
        this.args.add(new OptionData(OptionType.STRING, "menu", "choose a menu", true)
                .addChoice("Breakfast","breakfast")
                .addChoice("Lunch","lunch")
                .addChoice("Dinner","dinner"));
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow concurrent executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", DiningMenuCommand.class.getSimpleName());

            //Get the chosen dining market
            String diningMarket = Objects.requireNonNull(event.getOption("dining-market")).getAsString();

            //Get the chosen menu type
            String menuType = Objects.requireNonNull(event.getOption("menu")).getAsString();

            //Get embeds in pagination menu
            ArrayList<MessageEmbed> diningMenuEmbeds = buildMenu(bot.client.diningMenuLookup(diningMarket,menuType),diningMarket, menuType);

            if (!diningMenuEmbeds.isEmpty()){
                //Send a paginated menu
                ReplyCallbackAction action = event.replyEmbeds(diningMenuEmbeds.get(0));
                if (diningMenuEmbeds.size() > 1){
                    ButtonListener.sendPaginatedMenu(event.getUser().getId(), action, diningMenuEmbeds);
                    return;
                }
                action.queue();
            } else{
                event.reply(event.getUser().getAsMention() + " Unable to retrieve the dining menu at this moment!").queue();
            }
        }, bot.service);
    }


    /**
     * To generate embeds for pagination menu
     * @param stations the content of the menu (every food station and its food items)
     * @param diningMarketArg the chosen dining market from choice argument
     * @param menuArg the chosen menu type from choice argument
     * @return an ArrayList of all embeds in the pagination menu
     */
    private ArrayList<MessageEmbed> buildMenu(HashMap<String,String> stations, String diningMarketArg, String menuArg){

        String diningMarket = null;
        DiningMenuImage thumbnail = null;

        //To obtain the dining market name with proper capitalization and the image thumbnail
        for (net.dv8tion.jda.api.interactions.commands.Command.Choice choice : this.args.get(0).getChoices()){
            if (choice.getName().toLowerCase().substring(0,2).equals(diningMarketArg.substring(0,2))){
                diningMarket = choice.getName();
                thumbnail = DiningMenuImage.valueOf(diningMarketArg.substring(0,2).toUpperCase());
                break;
            }
        }

        //Capitalize the first letter of menu type
        String menuType = menuArg.substring(0,1).toUpperCase() + menuArg.substring(1);

        //Create an ArrayList of embeds
        ArrayList<MessageEmbed> embeds = new ArrayList<>();

        if (stations != null){
            for (Map.Entry<String,String> entry :  stations.entrySet()){
                //Create new embed
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(diningMarket + " - " + menuType + " Menu\n\n" + "Station: " + entry.getKey().split(":")[1]);
                embed.setThumbnail(thumbnail.url);
                embed.setFooter("Date - " + LocalDate.now());
                embed.setColor(Color.red);
                //Populate an Array of food items within the station
                String[] foodItems = entry.getValue().split(":");
                //Iterate and add every food item to the embed as a field
                for (String foodName : foodItems){
                    String[] splitArgs = foodName.split("-");
                    if (!foodName.equals("null")){
                        embed.addField(splitArgs[1], splitArgs[0] + "\n" + splitArgs[2], true);
                    }
                }
                //Store the embed in the ArrayList
                embeds.add(embed.build());
            }
        }
        return embeds;
    }
}

