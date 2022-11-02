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
import java.util.*;
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
            Iterator<Map.Entry<String, String>> iterator = stations.entrySet().iterator();

            //Previous station pointer for hashmap iteration
            String prevStation = null;

            //Declare and initialize EmbedBuilder
            EmbedBuilder embed = new EmbedBuilder();

            //Iterate through the HashMap
            while (iterator.hasNext()) {
                //Grab the current entry
                Map.Entry<String, String> entry = iterator.next();
                //Obtain the station in the current entry
                String currentStation = entry.getKey().split("-0")[1];
                //Check if it on the first entry
                if (prevStation == null){
                    //Initialize embed to new EmbedBuilder object with edited title
                    embed = new EmbedBuilder()
                            .setTitle(diningMarket + " - " + menuType + " Menu\n\n" + "Station: " + currentStation)
                            .setThumbnail(thumbnail.url)
                            .setFooter("Date - " + LocalDate.now())
                            .setColor(Color.red);

                //Check if the current station is not equal to the station in the previous entry (which means it's a new station)
                } else if (!currentStation.equals(prevStation)){
                    //Build the previous embed and add to the ArrayList before initializing the embed to a new EmbedBuilder
                    embeds.add(embed.build());
                    //Initialize embed to new EmbedBuilder object with edited title
                    embed = new EmbedBuilder()
                            .setTitle(diningMarket + " - " + menuType + " Menu\n\n" + "Station: " + currentStation)
                            .setThumbnail(thumbnail.url)
                            .setFooter("Date - " + LocalDate.now())
                            .setColor(Color.red);
                }
                //Add a new field of the food type and every food item that corresponds with the type
                embed.addField(entry.getKey().split("-0")[2],entry.getValue(),false);

                //Set previous pointer to the current pointer (station)
                prevStation = currentStation;

                //If it is currently on the last entry, build the embed and add it to the ArrayList
                if (!iterator.hasNext()){
                    embeds.add(embed.build());
                }
            }
        }
        return embeds;
    }
}

