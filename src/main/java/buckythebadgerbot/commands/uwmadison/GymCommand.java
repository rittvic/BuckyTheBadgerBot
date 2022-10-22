package buckythebadgerbot.commands.uwmadison;

import buckythebadgerbot.pojo.Professor;
import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Command that retrieves information about a professor from ratemyprofessor.com
 * Calls from HTTPClient.java
 */
public class GymCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(GymCommand.class);
    public GymCommand(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "gym";
        this.description = "Display live gym building usage";
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
            long startTime = System.nanoTime();
            event.deferReply().queue();

            String userID = event.getUser().getId();
            ArrayList<HashMap<String, String>> gymInformation = bot.gymClient.gymLookup();

            if (!gymInformation.isEmpty()){

                ArrayList<Button> buttonsToSend = bot.buttonListener.getPaginatedButtons(userID, 1,4);

                EmbedBuilder eb1 = new EmbedBuilder()
                        .setTitle(gymInformation.get(0).keySet().stream().findFirst().get().split(":")[0])
                        .setColor(Color.red);

                for (HashMap<String, String> entry : gymInformation){
                   for (String key : entry.keySet()){
                       if (key.contains("Nicholas Recreation Center")) {
                           eb1.addField(key.split(":")[1], entry.get(key), true);
                       } //else if (key.contains("Shell")){
                           //eb2.addField(key.split(":")[1], entry.get(key), true);
                       //} else if (key.contains("Aquatics")){
                           //eb3.addField(key.split(":")[1], entry.get(key), true);
                      // } else if (key.contains("Sport Programs Staff")){
                          // eb4.addField(key.split(":")[1], entry.get(key), true);
                       //}
                   }
                }


                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;
                eb1.setFooter("This took " + duration + " ms to respond.");

                //Create MessageBuilder
                MessageCreateBuilder message = new MessageCreateBuilder();

                //Add the embed
                message.addEmbeds(eb1.build());

                //add the buttons
                message.addActionRow(buttonsToSend);


                event.getHook().sendMessage(message.build()).queue();

                //Disable the buttons after 10 minutes starting the execution of slash command
                event.getHook().editOriginalComponents(ActionRow.of(buttonsToSend).asDisabled()).queueAfter(10, TimeUnit.MINUTES);

            } else{
                event.reply("Unable to retrieve gym information at the moment").queue();
            }

        }, bot.service);
    }
}

