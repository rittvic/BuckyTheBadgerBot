package buckythebadgerbot.data.database;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.data.database.repositories.impl.CoursesRepository;
import buckythebadgerbot.data.database.repositories.Repository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private final BuckyTheBadgerBot bot;
    private static final HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String CONTAINER_NAME = "buckythebadgerbot-db";
    private static final String PORT = "5432";
    private static final Map<String, Repository> repositoriesMap = new HashMap<>();

    public Database(BuckyTheBadgerBot bot) {
        this.bot = bot;
        mapRepository(
                new CoursesRepository(bot)
        );
        this.connect();
    }

    public void connect() {
        config.setJdbcUrl("jdbc:postgresql://" + CONTAINER_NAME + ":" + PORT + "/" + bot.getConfig().get("POSTGRES_DB"));
        config.setUsername(bot.getConfig().get("POSTGRES_USER"));
        config.setPassword(bot.getConfig().get("POSTGRES_PASSWORD"));
        config.setDriverClassName("org.postgresql.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        logger.info("Successfully connected to database!");
    }

    private void mapRepository(Repository ...repositories) {
        for (Repository repository : repositories) {
            repositoriesMap.put(repository.name, repository);
        }
    }

    public Repository getRepository(String repositoryName) throws SQLException {
        if (repositoriesMap.containsKey(repositoryName)) {
            return repositoriesMap.get(repositoryName);
        } else {
            throw new SQLException("Table '" + repositoryName + "' does not exist!");
        }
    }

    public Connection getConnectionFromPool() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        dataSource.close();
    }


}
