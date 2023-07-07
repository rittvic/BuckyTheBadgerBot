package buckythebadgerbot.commands.impl.uwmadison;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.data.RegStudentOrg;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Command that retrieves information for dining menu
 * Calls from HTTPClient.java
 */
public class RSOCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(RSOCommand.class);

    public RSOCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "rso";
        this.description = "Query registered student organizations";
        this.explanation = """
                 """;
        this.args.add(new OptionData(OptionType.STRING, "query", "Name of organization/club", true));
    }

    /**
     * Method to execute the task of the command
     * @param event the event of the slash command
     * NOTE: The entire command is a Runnable task, meaning it is a thread managed by the ExecutorService threadpool. This is to allow asynchronous executions.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", RSOCommand.class.getSimpleName());

            String query = event.getOption("query").getAsString();
            long startTime = System.nanoTime();
            List<RegStudentOrg> results = bot.rsoClient.getOrgs(query);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;

            //Get embeds in pagination menu
            ArrayList<MessageEmbed> rsoEmbeds = buildMenu(results,duration);
            if (!rsoEmbeds.isEmpty()) {
                //Send pagination menu
                ReplyCallbackAction action = event.replyEmbeds(rsoEmbeds.get(0));
                if (rsoEmbeds.size() > 1) {
                    PaginationUtils.sendPaginatedMenu(event.getUser().getId(), action, rsoEmbeds);
                    return;
                }
                action.queue();
            } else {
                event.reply("No results found.").queue();
            }
        }, bot.service);
    }

    /**
     * Generate embeds for pagination menu
     * @param orgs list of registered student orgs
     * @param duration the duration of the get request
     * @return an ArrayList of all embeds in the pagination menu
     */
    private ArrayList<MessageEmbed> buildMenu(List<RegStudentOrg> orgs, long duration) {
        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        if (orgs != null) {
            for (RegStudentOrg org : orgs) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(org.getName(), org.getWebsite())
                        .setColor(Color.red);
                if (org.getProfilePictureLink() != null) {
                    embed.setThumbnail(org.getProfilePictureLink());
                }
                if (org.getSummary() != null) {
                    embed.addField("Summary",org.getSummary(),false);
                }
                embed.addField("Status",org.getStatus(),false);
                if (org.getCategoryNames().length != 0) {
                    StringBuilder categories = new StringBuilder();
                    for (String category : org.getCategoryNames()) {
                        categories.append("`").append(category).append("`").append("\n");
                    }
                    embed.addField("Categories", categories.toString(),false);
                }
                embed.setFooter("This took " + duration + " ms to respond.");
                embeds.add(embed.build());
            }
        }
        return embeds;
    }
}

