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
                course.setSubjectAbbrev(rs.getString(1));
                course.setFullSubjectName(rs.getString(2));
                course.setNumber(rs.getString(3));
                course.setTitle(rs.getString(4));
                //Null decimal values get auto-converted to 0.0, so we have to manually set it to null if it is actually null by checking with Object type
                course.setCumulativeGpa(rs.getObject(5) == null ? null : rs.getDouble(5));
                course.setCredits(rs.getString(6));
                course.setDescription(rs.getString(7));
                course.setRequisites(rs.getString(8));
                course.setCourseDesignation(rs.getString(9));
                course.setRepeatable(rs.getString(10));
                course.setLastTaught(rs.getString(11));
                course.setCrosslistSubjects(rs.getString(12));
                courses.add(course);
            }
            return courses;
        }
    }
}
