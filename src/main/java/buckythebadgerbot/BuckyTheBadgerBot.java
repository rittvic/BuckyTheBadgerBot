package buckythebadgerbot;

import buckythebadgerbot.commands.CommandManager;
import buckythebadgerbot.data.database.Database;
import buckythebadgerbot.listeners.StringSelectListener;
import buckythebadgerbot.listeners.ButtonListener;
import buckythebadgerbot.services.impl.DiningMenuService;
import buckythebadgerbot.services.impl.GymService;
import buckythebadgerbot.services.impl.RMPService;
import com.zaxxer.hikari.pool.HikariPool;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main class for the Discord Bot
 * Initializes shard manager, threadpool, and listeners
 */
public class BuckyTheBadgerBot {

    public RMPService rateMyProfessorClient;
    public DiningMenuService diningMenuClient;
    public GymService gymClient;
    public ExecutorService service;
    public final @NotNull ButtonListener buttonListener;
    public final @NotNull StringSelectListener stringSelectListener;
    public @NotNull final Dotenv config;
    public @NotNull final ShardManager shardManager;
    public Database database;
    private static final Logger logger = LoggerFactory.getLogger(BuckyTheBadgerBot.class);

    //To implement anti-spam measurement for certain trigger events
    //Synchronized the LinkedHashMap to allow concurrent modifications
    //Follows the format {"UUID:buttonName"=timestamp}
    public static final Map<String, Long> coolDownChecker = Collections.synchronizedMap(new LinkedHashMap<>());

    //Scheduler primarily for disabling buttons after a set period of time
    public static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(20);
    public BuckyTheBadgerBot() throws LoginException, SQLException {

        //Load environment variables
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");

        //Setup HTTP tools
        rateMyProfessorClient = new RMPService(config.get("RMP_TOKEN"));
        diningMenuClient = new DiningMenuService();
        gymClient = new GymService(config.get("RECWELL_TOKEN"));

        //Setup threadpool
        //NOTE: May choose to setup a certain amount of threads in the future
        service = Executors.newCachedThreadPool();

        //Setup database connection (optional)
        try {
            database = new Database(this);
        } catch (HikariPool.PoolInitializationException e) {
            logger.error("Unable to connect to the database! Moving on...");
        }

        //Build shard manager
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.playing("/help"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_PRESENCES);
        builder.addEventListeners(new CommandManager(this));
        shardManager = builder.build();

        //Register event listeners
        buttonListener = new ButtonListener(this);
        stringSelectListener = new StringSelectListener(this);
        shardManager.addEventListener(buttonListener,stringSelectListener);

    }

    @NotNull
    public Dotenv getConfig(){
        return config;
    }

    @NotNull
    public Database getDatabase(){
        return database;
    }

    @NotNull
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
        } catch (LoginException | SQLException e){
            logger.error("Something went wrong with registering the bot! {}", e.toString());
        }
    }
}
