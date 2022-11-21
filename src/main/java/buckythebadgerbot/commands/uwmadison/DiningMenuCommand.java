package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.utility.enums.DiningMenuImage;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information for dining menu
 * Calls from HTTPClient.java
 */
public class DiningMenuCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(DiningMenuCommand.class);

    public DiningMenuCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "diningmenu";
        this.description = "Display dining menu";
        this.explanation = """
                 `e.g., <Rheta's Market> <Breakfast>, <Gordon Avenue Market> <Lunch>, <Four Lakes Market> <Dinner>`\s
                 Display a dining menu consisting of every station and its food items within every category""";
        this.args.add(new OptionData(OptionType.STRING, "dining-market", "choose a dining market", true)
                .addChoice("Rheta's Market","rhetas-market-0Rheta's Market")
                .addChoice("Gordon Avenue Market","gordon-avenue-market-0Gordon Avenue Market")
                .addChoice("Lowell Market","lowell-market-0Lowell Market")
                .addChoice("Liz's Market","lizs-market-0Liz's Market")
                .addChoice("Carson's Market","carsons-market-0Carson's Market")
                .addChoice("Four Lakes Market","four-lakes-market-0Four Lakes Market"));
        this.args.add(new OptionData(OptionType.STRING, "menu", "choose a menu", true)
                .addChoice("Breakfast","breakfast-0Breakfast")
                .addChoice("Lunch","lunch-0Lunch")
                .addChoice("Dinner","dinner-0Dinner")
                .addChoice("Daily","lowell-dining-daily-0Daily"));
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

            //Obtain the argument (value) of the dining market choice
            String diningMarketArg = event.getOption("dining-market").getAsString();
            //Get the chosen dining market
            String diningMarket = diningMarketArg.split("-0")[0];
            //Obtain the argument (value) of the dining market choice
            String menuTypeArg = event.getOption("menu").getAsString();
            //Get the chosen menu type
            String menuType = menuTypeArg.split("-0")[0];

            if ((menuType.equals("lowell-dining-daily")) &&  !((diningMarket.equals("four-lakes-market")) || (diningMarket.equals("gordon-avenue-market")))){
                event.reply("`" + diningMarketArg.split("-0")[1] + " - " + menuTypeArg.split("-0")[1] +
                        "`" + "is not a valid option (does not exist)!").setEphemeral(true).queue();
                return;
            }

            long startTime = System.nanoTime();
            HashMap<String,String> stations = bot.client.diningMenuLookup(diningMarket,menuType);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;
            //Get embeds in pagination menu
            ArrayList<MessageEmbed> diningMenuEmbeds = buildMenu(stations,diningMarketArg.split("-0")[1], menuTypeArg.split("-0")[1],duration);

            if (!diningMenuEmbeds.isEmpty()){
                //Send a paginated menu
                ReplyCallbackAction action = event.replyEmbeds(diningMenuEmbeds.get(0));
                if (diningMenuEmbeds.size() > 1){
                    ButtonListener.sendPaginatedMenu(event.getUser().getId(), action, diningMenuEmbeds);
                    return;
                }
                action.queue();
            } else{
                //event.reply(event.getUser().getAsMention() + " Unable to retrieve the dining menu at this moment!").queue();
                event.reply("`" + diningMarketArg.split("-0")[1] + " - " + menuTypeArg.split("-0")[1] + "`" +
                        " is not offered today!").queue();
            }
        }, bot.service);
    }


    /**
     * To generate embeds for pagination menu
     * @param stations the content of the menu (every food station and its food items)
     * @param diningMarket the chosen dining market from choice argument
     * @param menuType the chosen menu type from choice argument
     * @return an ArrayList of all embeds in the pagination menu
     */
    private ArrayList<MessageEmbed> buildMenu(HashMap<String,String> stations, String diningMarket, String menuType, long duration){

       //Set the image thumbnail of the chosen dining market
        DiningMenuImage thumbnail =  DiningMenuImage.valueOf(diningMarket.substring(0,2).toUpperCase());;

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

                //If the entry value is null, continue onto the next iteration
                if (entry.getValue() == null){
                    continue;
                }

                String currentStation = entry.getKey().split("-0")[1];

                if (!currentStation.equals(prevStation)){
                    if (prevStation != null){
                        embeds.add(embed.build());
                    }
                    //Initialize embed to new EmbedBuilder object with edited title
                    embed = new EmbedBuilder()
                            .setTitle(diningMarket + " - " + menuType + " Menu\n\n" + "Station: " + currentStation)
                            .setThumbnail(thumbnail.url)
                            .setFooter(LocalDateTime.now(TimeZone.getTimeZone("US/Central").toZoneId()).format(DateTimeFormatter.ofPattern("MM/dd/uuuu • h:mm a"))
                                    + " (US Central Time)" + "\n" + "This took " + duration + " ms to respond.")
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

