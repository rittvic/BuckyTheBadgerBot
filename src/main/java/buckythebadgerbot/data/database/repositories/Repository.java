package buckythebadgerbot.data.database.repositories;

import buckythebadgerbot.BuckyTheBadgerBot;

import java.sql.SQLException;
import java.util.List;

public abstract class Repository <T> {
    public BuckyTheBadgerBot bot;
    public String name;

    public Repository(BuckyTheBadgerBot bot){
        this.bot = bot;
    }

    public abstract List<T> read(String query) throws SQLException;
}
