package buckythebadgerbot.data;

public class Course {

    private String crosslistedSubjectsWithNumber;
    private String subjectAbbrev;
    private String fullSubjectName;
    private String number;
    private String title;
    private String description;
    private Double cumulativeGpa;
    private String credits;
    private String requisites;
    private String courseDesignation;
    private String repeatable;
    private String lastTaught;

    public void setCrosslistedSubjectsWithNumber(String crosslistedSubjectsWithNumber) {
        this.crosslistedSubjectsWithNumber = crosslistedSubjectsWithNumber;
    }

    public void setSubjectAbbrev(String subjectAbbrev) {
        this.subjectAbbrev = subjectAbbrev;
    }

    public void setFullSubjectName(String fullSubjectName) {
        this.fullSubjectName = fullSubjectName;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCumulativeGpa(Double cumulativeGpa) {
        this.cumulativeGpa = cumulativeGpa;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public void setRequisites(String requisites) {
        this.requisites = requisites;
    }

    public void setCourseDesignation(String courseDesignation) {
        this.courseDesignation = courseDesignation;
    }

    public void setRepeatable(String repeatable) {
        this.repeatable = repeatable;
    }

    public void setLastTaught(String lastTaught) {
        this.lastTaught = lastTaught;
    }

    public String getCrosslistedSubjectsWithNumber() {
        return crosslistedSubjectsWithNumber;
    }

    public String getSubjectAbbrev() {
        return subjectAbbrev;
    }

    public String getFullSubjectName() {
        return fullSubjectName;
    }

    public String getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Double getCumulativeGpa() {
        return cumulativeGpa;
    }

    public String getCredits() {
        return credits;
    }

    public String getRequisites() {
        return requisites;
    }

    public String getCourseDesignation() {
        return courseDesignation;
    }

    public String getRepeatable() {
        return repeatable;
    }

    public String getLastTaught() {
        return lastTaught;
    }

    @Override
    public String toString() {
        return "Course{" +
                "crosslistedSubjectsWithNumber='" + crosslistedSubjectsWithNumber + '\'' +
                ", subjectAbbrev='" + subjectAbbrev + '\'' +
                ", fullSubjectName='" + fullSubjectName + '\'' +
                ", number='" + number + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", cumulativeGpa=" + cumulativeGpa +
                ", credits='" + credits + '\'' +
                ", requisites='" + requisites + '\'' +
                ", courseDesignation='" + courseDesignation + '\'' +
                ", repeatable='" + repeatable + '\'' +
                ", lastTaught='" + lastTaught + '\'' +
                '}';
    }
}
