package buckythebadgerbot;

import buckythebadgerbot.commands.CommandManager;
import buckythebadgerbot.services.APIHandler;
import buckythebadgerbot.listeners.ButtonListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class for the Discord Bot
 * Initializes shard manager, threadpool, and listeners
 */
public class BuckyTheBadgerBot {

    public APIHandler madGradesClient;
    public APIHandler RMPClient;
    public APIHandler client;
    public ExecutorService service;
    public final @NotNull ButtonListener buttonListener;
    public @NotNull final Dotenv config;
    public @NotNull final ShardManager shardManager;

    public BuckyTheBadgerBot() throws LoginException {

        //Load environment variables
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");

        //Setup HTTP tools
        madGradesClient = new APIHandler(config.get("MADGRADES_TOKEN"));
        RMPClient = new APIHandler(config.get("RMP_TOKEN"));
        client = new APIHandler();

        //Setup threadpool
        //NOTE: May choose to setup a certain amount of threads in the future
        service = Executors.newCachedThreadPool();

        //Build shard manager
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("/help"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_PRESENCES);
        builder.addEventListeners(new CommandManager(this));
        shardManager = builder.build();

        //Register listeners
        buttonListener = new ButtonListener(this);
        shardManager.addEventListener(buttonListener);

    }

    public Dotenv getConfig(){
        return config;
    }

    public ShardManager getShardManager(){
        return shardManager;
    }

    /**
     * Initializes BuckyTheBadgerBot
     * @param args param is ignored
     */
    public static void main(String[] args) {
        try {
            BuckyTheBadgerBot bot = new BuckyTheBadgerBot();
        } catch (LoginException e){
            System.out.println("ERROR: bot token is invalid");
        }
    }
}
