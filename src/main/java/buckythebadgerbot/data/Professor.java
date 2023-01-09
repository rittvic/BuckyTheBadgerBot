package buckythebadgerbot.data;

import java.util.List;

/**
 * Professor object (POJO) for storing the JSON responses
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
    private String school;
    private double wouldTakeAgainPercent;
    private List<String> topFiveTags;
    private List<String> coursesTaught;
    private boolean doesExist;
    private boolean fallback;

    public Professor() {
        this.doesExist = true;
        this.fallback = false;
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

    public String getSchool() {
        return school;
    }

    public double getWouldTakeAgainPercent() {
        return (int) Math.ceil(this.wouldTakeAgainPercent);
    }

    public List<String> getTopFiveTags() {
        return topFiveTags;
    }

    public List<String> getCoursesTaught() {
        return coursesTaught;
    }

    public boolean getDoesExist() {
        return doesExist;
    }

    public boolean getFallback() {
        return fallback;
    }

    public void setRegularId(String regularId) {
        this.regularId = regularId;
    }

    public void setLegacyId(String legacyId) {
        this.legacyId = legacyId;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public void setAvgDifficulty(double avgDifficulty) {
        this.avgDifficulty = avgDifficulty;
    }

    public void setNumRating(int numRating) {
        this.numRating = numRating;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public void setWouldTakeAgainPercent(double wouldTakeAgainPercent) {
        this.wouldTakeAgainPercent = wouldTakeAgainPercent;
    }

    public void setTopFiveTags(List<String> topFiveTags) {
        this.topFiveTags = topFiveTags;
    }

    public void setCoursesTaught(List<String> coursesTaught) {
        this.coursesTaught = coursesTaught;
    }

    public void setDoesExist(boolean doesExist) {
        this.doesExist = doesExist;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    @Override
    public String toString() {
        return "Professor{" +
                "regularId='" + regularId + '\'' +
                ", legacyId='" + legacyId + '\'' +
                ", avgRating=" + avgRating +
                ", avgDifficulty=" + avgDifficulty +
                ", numRating=" + numRating +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", department='" + department + '\'' +
                ", school='" + school + '\'' +
                ", wouldTakeAgainPercent=" + wouldTakeAgainPercent +
                ", topFiveTags=" + topFiveTags +
                ", coursesTaught=" + coursesTaught +
                ", doesExist=" + doesExist +
                ", fallback=" + fallback +
                '}';
    }
}
