package buckythebadgerbot.listeners;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.uwmadison.ProfCommand;
import buckythebadgerbot.pojo.ratemyprofessors.StudentRating;
import buckythebadgerbot.utility.pagination.PaginationUtility;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for select option clicks (from the Select Menu) and handles them accordingly
 * Has a cooldown system (30s) in place to prevent spams
 */
public class StringSelectListener extends ListenerAdapter {

    private final BuckyTheBadgerBot bot;

    public static final HashMap<String, List<SelectOption>> StringSelectOptions = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(StringSelectListener.class);

    public StringSelectListener(BuckyTheBadgerBot bot) {
        this.bot = bot;
    }

    /**
     * Populate a list of SelectOptions and store it in the map
     * @param uuid the user ID + random UUID
     * @param argument the argument to differentiate select options
     * @param options a list of Strings to convert to a list of SelectOptions
     */
    public static void sendStringSelectOptions(String uuid, String argument, List<String> options) {
        if (options != null) {
            List<SelectOption> selectOptions = new ArrayList<>();
            for (String option : options) {
                selectOptions.add(SelectOption.of(option, option + ":" + argument));
            }
            StringSelectOptions.put(uuid, selectOptions);
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        CompletableFuture.runAsync(() -> {
            logger.info("Executing {}", StringSelectListener.class.getSimpleName());

            //[0] UserID [1] UUID [2] Argument ID (e.g, studentRatings) [3] Argument Label (e.g, professor's regular id)
            String[] pressedArgs = event.getComponent().getId().split(":");

            //Store the user ID of who triggered the select options
            String eventUserID = event.getUser().getId();

            if (pressedArgs[2].equals("studentRatings")) {
                String uuid = pressedArgs[0] + ":" + pressedArgs[1];
                //Check if the user requested the StringSelectInteractionMenu
                if (pressedArgs[0].equals(eventUserID)) {
                    String profRegularId = pressedArgs[3];
                    //Label - Course | Value - Course:ProfName
                    List<SelectOption> selectedOptions = event.getSelectedOptions();
                    for (SelectOption option : selectedOptions) {
                        String course = option.getValue().split(":")[0];
                        //Check if the user didn't choose an option that's currently in cooldown
                        if (!BuckyTheBadgerBot.coolDownChecker.containsKey(uuid + ":" + course + ":" + profRegularId)
                                || System.currentTimeMillis() > BuckyTheBadgerBot.coolDownChecker.get(uuid + ":" + course + ":" + profRegularId) + 30000) {
                            String profName = option.getValue().split(":")[1];
                            long startTime = System.nanoTime();
                            ArrayList<StudentRating> ratings = bot.rateMyProfessorClient.getStudentRatings(profRegularId, course);
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime) / 1000000;
                            BuckyTheBadgerBot.coolDownChecker.put(uuid + ":" + course + ":" + profRegularId, System.currentTimeMillis());
                            if (ratings == null) {
                                event.reply("Could not find any student ratings for `" + course + "`!").queue();
                                return;
                            }
                            ArrayList<MessageEmbed> studentRatingEmbeds = ProfCommand.buildMenu(ratings, profName, duration);
                            ReplyCallbackAction action = event.replyEmbeds(studentRatingEmbeds.get(0));
                            if (studentRatingEmbeds.size() > 1) {
                                PaginationUtility.sendPaginatedMenu(eventUserID, action, studentRatingEmbeds);
                                return;
                            }
                            action.queue();
                        } else {
                            event.reply("Stop spamming! You already selected `" + course + "` recently. Please wait 30 seconds...").setEphemeral(true).queue();
                        }
                    }
                } else {
                    event.reply("You didn't request this!").setEphemeral(true).queue();
                }
                //Clean map of expired timestamps
                //NOTE: Doing a while loop is faster than Collection.removeif by a few milliseconds since it only iterates through expired elements
                Iterator<Map.Entry<String, Long>> iterator = BuckyTheBadgerBot.coolDownChecker.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    if (entry.getValue() + 30000 < System.currentTimeMillis()) {
                        iterator.remove();
                    }
                }
            }

        }, bot.service);
    }
}
