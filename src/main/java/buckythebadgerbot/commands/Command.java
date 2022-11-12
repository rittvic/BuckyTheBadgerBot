package buckythebadgerbot.commands;

import buckythebadgerbot.BuckyTheBadgerBot;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

/**
 * A general slash command with basic properties for registration
 */
public abstract class Command {
    public BuckyTheBadgerBot bot;
    public String name;
    public String description;
    public String explanation;

    public List<OptionData> args;
    public List<SubcommandData> subCommands;


    public Command(BuckyTheBadgerBot bot){
        this.bot = bot;
        this.args = new ArrayList<>();
        this.subCommands = new ArrayList<>();
    }

    public abstract void execute(SlashCommandInteractionEvent event);
}
