package buckythebadgerbot.commands;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.impl.utility.HelpCommand;
import buckythebadgerbot.commands.impl.uwmadison.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommandManager extends ListenerAdapter {

    public static final List<Command> commands = new ArrayList<>();
    public static final Map<String, Command> commandsMap = new HashMap<>();

    /**
     * Adds the slash commands to a  list and registers them as event listener.
     * @param bot An instance of BuckyTheBadgerBot
     */
    public CommandManager(BuckyTheBadgerBot bot) {
        mapCommand(
                new CourseCommand(bot),
                new ProfCommand(bot),
                new SearchCommand(bot),
                new HelpCommand(bot),
                new GymCommand(bot),
                new DiningMenuCommand(bot),
                new RSOCommand(bot)
        );
    }

    /**
     * Adds a command to the static list and map.
     * @param cmds a list (spread) of command objects
     */
    private void mapCommand(Command ...cmds) {
        for (Command cmd : cmds) {
            commandsMap.put(cmd.name, cmd);
            commands.add(cmd);
        }
    }

    /**
     * Creates a list of CommandData for all commands.
     * @return a list of CommandData for registration.
     */
    public static List<CommandData> unpackCommandData() {
        // Register slash commands
        List<CommandData> commandData = new ArrayList<>();
        for (Command command : commands) {
            SlashCommandData slashCommand = Commands.slash(command.name, command.description).addOptions(command.args);
            if (!command.subCommands.isEmpty()) {
                slashCommand.addSubcommands(command.subCommands);
            }
            commandData.add(slashCommand);
        }
        return commandData;
    }


    /**
     * Executes when a slash command is run
     * @param event the slash command event
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // Get command by name
        Command cmd = commandsMap.get(event.getName());
        if (cmd != null) {
            // Run command
            cmd.execute(event);
        }
    }

    /**
     * Registers slash commands as global commands
     * NOTE: May take up to an hour for changes to apply
     * @param event executes when bot is ready
     */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // Register slash commands
        event.getJDA().updateCommands().addCommands(unpackCommandData()).queue(succ -> {}, fail -> {});
    }
}
