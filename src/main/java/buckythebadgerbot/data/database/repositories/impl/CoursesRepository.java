package buckythebadgerbot.data.database.repositories.impl;

import buckythebadgerbot.BuckyTheBadgerBot;
import buckythebadgerbot.commands.impl.uwmadison.SearchCommand;
import buckythebadgerbot.data.Course;
import buckythebadgerbot.data.database.repositories.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CoursesRepository extends Repository<Course> {

    public CoursesRepository(BuckyTheBadgerBot bot) {
        super(bot);
        this.name = "courses";
    }

    @Override
    public List<Course> read(String query) throws SQLException {
        try (Connection connection = bot.getDatabase().getConnectionFromPool();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            List<Course> courses = new ArrayList<>();
            while (rs.next()) {
                Course course = new Course();
                //NOTE: PostgreSQL uses 1-indexed for some reason...
                course.setCrosslistedSubjectsWithNumber(rs.getString(1));
                course.setSubjectAbbrev(rs.getString(2));
                course.setFullSubjectName(rs.getString(3));
                course.setNumber(rs.getString(4));
                course.setTitle(rs.getString(5));
                course.setCumulativeGpa(rs.getDouble(6));
                course.setCredits(rs.getString(7));
                course.setDescription(rs.getString(8));
                course.setRequisites(rs.getString(9));
                course.setCourseDesignation(rs.getString(10));
                course.setRepeatable(rs.getString(11));
                course.setLastTaught(rs.getString(12));
                courses.add(course);
            }
            return courses;
        }
    }
}
