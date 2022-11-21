package buckythebadgerbot.commands.utility;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import buckythebadgerbot.commands.CommandManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Command that sends a help menu consisting information of all slash commands
 * Uses the explanation instance field from slash commands to automate
 */
public class HelpCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);

    public HelpCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "help";
        this.description = "Display all commands";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", HelpCommand.class.getSimpleName());

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Help Menu")
                    .setColor(Color.red)
                    .setDescription("The slash commands:")
                    .setFooter("Unable to find a particular course? Blame madgrades api for being outdated. I might work on something of my own to fix this.");

            CommandManager.commands.forEach(command -> {
                if (!command.name.equals("help")) {
                    String args = "";
                    for (OptionData arg : command.args) {
                        args += " <" + arg.getName() + ">";
                    }
                    eb.addField(command.name, "`/" + command.name + args + "`" + "\n" + command.explanation, false);
                }
            });
            event.replyEmbeds(eb.build()).queue();
        }, bot.service);
    }
}
