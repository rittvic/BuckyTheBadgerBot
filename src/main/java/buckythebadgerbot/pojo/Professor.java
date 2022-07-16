package buckythebadgerbot.pojo;

import java.util.List;

/**
 * Professor object (POJO) for storing the JSON responses (within profInfo() method in HTTPClient.java)
 */
public class Professor {
    private String regularId;
    private String legacyId;
    private double avgRating;
    private double avgDifficulty;
    private int numRating;
    private String firstName;
    private String lastName;
    private String department;
    private double wouldTakeAgainPercent;
    private List<String> topFiveReviews;
    private List<String> coursesTaught;
    private boolean doesExist;

    public Professor(boolean doesExist, String regularId, String legacyId, double wouldTakeAgainPercent, double avgRating, double avgDifficulty, int numRating, String firstName, String lastName, String department, List<String> topFiveReviews, List<String> coursesTaught){
        this.doesExist = doesExist;
        this.regularId = regularId;
        this.legacyId = legacyId;
        this.avgRating = avgRating;
        this.avgDifficulty = avgDifficulty;
        this.numRating = numRating;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.wouldTakeAgainPercent = wouldTakeAgainPercent;
        this.topFiveReviews = topFiveReviews;
        this.coursesTaught = coursesTaught;
    }

    public Professor(boolean doesExist){
        this.doesExist = doesExist;

    }

    public String getRegularId() {
        return regularId;
    }

    public String getLegacyId() {
        return legacyId;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public double getAvgDifficulty() {
        return avgDifficulty;
    }

    public int getNumRating() {
        return numRating;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDepartment() {
        return department;
    }

    public int getTakeAgainPercentage() {
        return (int)Math.ceil(this.wouldTakeAgainPercent);
    }

    public List<String> getTopFiveReviews() {
        return topFiveReviews;
    }

    public List<String> getCoursesTaught() {
        return coursesTaught;
    }

    public boolean getDoesExist() {
        return doesExist;
    }

    @Override
    public String toString() {
        return "Professor{" +
                "It exists='" + doesExist + '\'' +
                "Regular ID='" + regularId + '\'' +
                "Legacy ID='" + legacyId + '\'' +
                "avgRating=" + avgRating +
                ", avgDifficulty=" + avgDifficulty +
                ", numRating=" + numRating +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", department='" + department + '\'' +
                ", wouldTakeAgainPercent=" + wouldTakeAgainPercent +
                ", topFiveReviews=" + topFiveReviews +
                ", courses taught=" + coursesTaught +
                '}';
    }
}
