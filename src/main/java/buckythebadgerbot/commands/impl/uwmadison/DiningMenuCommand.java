package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.utils.enums.DiningMenuImage;
import buckythebadgerbot.utils.pagination.PaginationUtils;
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
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow asynchronous executions.
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
                        "`" + " is not a valid option (doesn't exist).").setEphemeral(true).queue();
                return;
            }

            long startTime = System.nanoTime();
            HashMap<String,String> stations = bot.diningMenuClient.getDiningMenu(diningMarket,menuType);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;
            //Get embeds in pagination menu
            ArrayList<MessageEmbed> diningMenuEmbeds = buildMenu(stations,diningMarketArg.split("-0")[1], menuTypeArg.split("-0")[1],duration);

            if (!diningMenuEmbeds.isEmpty()){
                //Send a paginated menu
                ReplyCallbackAction action = event.replyEmbeds(diningMenuEmbeds.get(0));
                if (diningMenuEmbeds.size() > 1){
                    PaginationUtils.sendPaginatedMenu(event.getUser().getId(), action, diningMenuEmbeds);
                    return;
                }
                action.queue();
            } else{
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
    private ArrayList<MessageEmbed> buildMenu(HashMap<String, String> stations, String diningMarket, String menuType, long duration) {
        DiningMenuImage thumbnail = DiningMenuImage.valueOf(diningMarket.substring(0, 2).toUpperCase());
        ArrayList<MessageEmbed> embeds = new ArrayList<>();

        //Iterate through the stations and build an embed for every station and its food contents
        if (stations != null) {
            Iterator<Map.Entry<String, String>> iterator = stations.entrySet().iterator();
            String prevStation = null;
            EmbedBuilder embed = new EmbedBuilder();

            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getValue() == null) {
                    continue;
                }
                String currentStation = entry.getKey().split("-0")[1];
                if (!currentStation.equals(prevStation)) {
                    if (prevStation != null) {
                        embeds.add(embed.build());
                    }
                    embed = new EmbedBuilder()
                            .setTitle(diningMarket + " - " + menuType + " Menu\n\n" + "Station: " + currentStation)
                            .setThumbnail(thumbnail.url)
                            .setFooter("This took " + duration + " ms to respond." + " " + LocalDateTime.now(TimeZone.getTimeZone("US/Central").toZoneId()).format(DateTimeFormatter.ofPattern("MM/dd/uuuu at h:mm a"))
                                    + " (US Central Time)")
                            .setColor(Color.red);

                }
                embed.addField(entry.getKey().split("-0")[2], entry.getValue(), false);
                prevStation = currentStation;
                if (!iterator.hasNext()) {
                    embeds.add(embed.build());
                }
            }
        }
        return embeds;
    }
}

