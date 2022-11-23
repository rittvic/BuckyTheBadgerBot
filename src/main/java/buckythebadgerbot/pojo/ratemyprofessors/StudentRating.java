package buckythebadgerbot.pojo.ratemyprofessors;

import buckythebadgerbot.utility.enums.StudentRatingQualityImage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * StudentRating object (POJO) deserialized from JSON
 * Represents a student rating on RateMyProfessor.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentRating {

    private String course;

    @JsonProperty("isForCredit")
    private boolean forCredit;

    @JsonProperty("attendanceMandatory")
    private String attendance;

    @JsonProperty("wouldTakeAgain")
    private int wouldTakeAgain;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("textbookUse")
    private int textbookUse;

    @JsonProperty("date")
    private String date;

    @JsonProperty("clarityRating")
    private int quality;

    private String ratingQuality;

    @JsonProperty("ratingTags")
    private String tags;

    @JsonProperty("difficultyRating")
    private int difficulty;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("thumbsUpTotal")
    private int thumbsUpTotal;

    @JsonProperty("thumbsDownTotal")
    private int thumbsDownTotal;

    //Getters
    public String getCourse() {
        return course;
    }

    public String isForCredit() {
        if (forCredit) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public String getAttendance() {
        if (this.attendance.isEmpty()){
            return "N/A";
        } else {
            return this.attendance;
        }
    }

    public String getWouldTakeAgain() {
        if (wouldTakeAgain == 0) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public String getGrade() {
        if (grade.isEmpty()) {
            return "N/A";
        } else {
            return grade;
        }
    }

    public String getTextbookUse() {
        if (textbookUse >= 0 && textbookUse <= 2) {
            return "No";
        } else if (textbookUse >= 3) {
            return "Yes";
        } else {
            return "N/A";
        }
    }

    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        //Convert the date to epoch
        try {
            //strip "+0000 UTC" to get the format: yyyy-MM-dd HH:mm:ss
            String formattedDate = this.date.substring(0, date.length() - 10);
            Date date = sdf.parse(formattedDate);
            long epoch = date.getTime() / 1000;
            return String.valueOf(epoch);
        } catch (ParseException e) {
            return this.date;
        }
    }

    public int getQuality() {
        return this.quality;
    }

    public StudentRatingQualityImage getRatingQuality() {
        if (this.quality == 1 || this.quality == 2) {
            return StudentRatingQualityImage.AWFUL;
        } else if (this.quality == 3) {
            return StudentRatingQualityImage.AVERAGE;
        } else {
            return StudentRatingQualityImage.AWESOME;
        }
    }

    public String[] getTags() {
        if (tags.isEmpty()) {
            return null;
        } else {
            return tags.split("--");
        }
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getComment() {
        return comment;
    }

    public int getThumbsUpTotal() {
        return thumbsUpTotal;
    }

    public int getThumbsDownTotal() {
        return thumbsDownTotal;
    }

    //Setters
    public void setCourse(String course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "StudentRating{" +
                "forCredit=" + forCredit +
                ", attendance='" + attendance + '\'' +
                ", wouldTakeAgain=" + wouldTakeAgain +
                ", grade='" + grade + '\'' +
                ", textbookUse=" + textbookUse +
                ", date='" + date + '\'' +
                ", imageUrl=" + quality +
                ", tags='" + tags + '\'' +
                ", difficulty=" + difficulty +
                ", comment='" + comment + '\'' +
                ", thumbsUpTotal=" + thumbsUpTotal +
                ", thumbsDownTotal=" + thumbsDownTotal +
                '}';
    }
}
