package buckythebadgerbot.utility.pagination;

import buckythebadgerbot.listeners.ButtonListener;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to handle anything related to pagination
 */
public class PaginationUtility {

    //Map to store every embed for a paginated menu
    public static final Map<String, List<MessageEmbed>> paginatedMenus = new HashMap<>();

    //Map to store the paginated buttons for a paginated menu
    public static final Map<String, List<Button>> paginationButtons = new HashMap<>();

    /**
     * Create and send a paginated menu
     *
     * @param userID the user id of the user who initiated the search command
     * @param action the original event
     * @param embeds the list of embeds for the menu
     */
    public static void sendPaginatedMenu(String userID, ReplyCallbackAction action, List<MessageEmbed> embeds) {
        String uuid = userID + ":" + UUID.randomUUID();
        List<Button> components = getPaginationButtons(uuid, embeds.size());
        paginationButtons.put(uuid, components);
        paginatedMenus.put(uuid, embeds);
        //Add the updated buttons and disable them after 10 minutes
        action.setActionRow(components).queue(interactionHook -> disablePaginationButtons(uuid, interactionHook));
    }

    /**
     * Create and send the paginated buttons
     *
     * @param uuid the user ID + random UUID
     * @param maxPages the maximum number of pages on the menu
     * @return a list of the paginated buttons
     */
    private static List<Button> getPaginationButtons(String uuid, int maxPages) {
        return Arrays.asList(
                Button.primary(uuid + ":pagination:first", Emoji.fromUnicode("\u23EA")).asDisabled(),
                Button.primary(uuid + ":pagination:prev", Emoji.fromUnicode("\u25C0")).asDisabled(),
                Button.secondary("pagination:page:0", "1/" + maxPages).asDisabled(),
                Button.primary(uuid + ":pagination:next", Emoji.fromUnicode("\u25B6")),
                Button.primary(uuid + ":pagination:last", Emoji.fromUnicode("\u23E9"))
        );
    }

    /**
     * Update the pagination buttons
     *
     * @param uuid the user ID + random UUID
     * @param arg the argument of the pagination button (E.g, "next", "prev", "first", "last" )
     * @param event the ButtonInteractionEvent to edit the button components of
     */
    public static void updatePaginationButtons(String uuid, String arg, ButtonInteractionEvent event) {
        List<Button> components = paginationButtons.get(uuid);
        List<MessageEmbed> embeds = paginatedMenus.get(uuid);
        if (arg.equals("next")) {
            int page = Integer.parseInt(components.get(2).getId().split(":")[2]) + 1;
            if (page < embeds.size()) {
                // Update buttons
                components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                components.set(1, components.get(1).asEnabled());
                components.set(0, components.get(0).asEnabled());
                if (page == embeds.size() - 1) {
                    components.set(3, components.get(3).asDisabled());
                    components.set(4, components.get(4).asDisabled());
                }
                paginationButtons.put(uuid, components);
                event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
            }
        } else if (arg.equals("prev")) {
            int page = Integer.parseInt(components.get(2).getId().split(":")[2]) - 1;
            if (page >= 0) {
                // Update buttons
                components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                components.set(3, components.get(3).asEnabled());
                components.set(4, components.get(4).asEnabled());
                if (page == 0) {
                    components.set(1, components.get(1).asDisabled());
                    components.set(0, components.get(0).asDisabled());
                }
                paginationButtons.put(uuid, components);
                event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
            }
        } else if (arg.equals("first")) {
            int page = 0;
            //Update buttons
            components.set(0, components.get(0).asDisabled());
            components.set(1, components.get(1).asDisabled());
            components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
            components.set(3, components.get(3).asEnabled());
            components.set(4, components.get(4).asEnabled());
            paginationButtons.put(uuid, components);
            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
        } else if (arg.equals("last")) {
            int page = embeds.size() - 1;
            //Update buttons
            components.set(4, components.get(4).asDisabled());
            components.set(3, components.get(3).asDisabled());
            components.set(2, components.get(2).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
            components.set(1, components.get(1).asEnabled());
            components.set(0, components.get(0).asEnabled());
            paginationButtons.put(uuid, components);
            event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
        }
    }

    /**
     * Disable pagination buttons after 10 minutes
     *
     * @param uuid the user ID + random UUID of the buttons to disable
     * @param hook The message hook pointing to the original message
     */
    public static void disablePaginationButtons(String uuid, InteractionHook hook) {
        Runnable task = () -> {
            List<Button> actionRow = paginationButtons.get(uuid);
            List<Button> newActionRow = new ArrayList<>();
            for (Button button : actionRow) {
                newActionRow.add(button.asDisabled());
            }
            hook.editOriginalComponents(ActionRow.of(newActionRow)).queue();
            paginationButtons.remove(uuid);
            paginatedMenus.remove(uuid);
        };
        ButtonListener.scheduledExecutor.schedule(task, 10, TimeUnit.MINUTES);
    }
}
