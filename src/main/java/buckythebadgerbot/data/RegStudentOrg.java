package buckythebadgerbot.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegStudentOrg {

    //to parse HTML strings
    private Source htmlSource;
    private Segment segment;
    private Renderer htmlRender;

    @JsonProperty("@search.score")
    private double searchScore;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("InstitutionId")
    private int institutionId;

    @JsonProperty("ParentOrganizationId")
    private int parentOrganizationId;

    @JsonProperty("BranchId")
    private int branchId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ShortName")
    private String shortName;

    @JsonProperty("WebsiteKey")
    private String websiteKey;

    @JsonProperty("ProfilePicture")
    private String profilePictureLink;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Summary")
    private String summary;

    @JsonProperty("CategoryIds")
    private String[] categoryIds;

    @JsonProperty("CategoryNames")
    private String[] categoryNames;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Visibility")
    private String visibility;

    public double getSearchScore() {
        return searchScore;
    }

    public String getId() {
        return id;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public int getParentOrganizationId() {
        return parentOrganizationId;
    }

    public int getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getWebsiteKey() {
        return websiteKey;
    }

    public String getWebsite(){
        return "https://win.wisc.edu/organization/" + websiteKey;
    }

    public String getProfilePictureLink() {
        if (profilePictureLink == null) {
            return null;
        }
        return "https://se-images.campuslabs.com/clink/images/" + profilePictureLink + "?preset=med-sq";
    }

    public String getDescription() {
        if (this.description == null) {
            return null;
        }
        this.htmlSource = new Source(description);
        this.segment = new Segment(htmlSource,0,htmlSource.length());
        this.htmlRender = new Renderer(segment);
        return htmlRender.toString();
    }

    public String getSummary() {
        return summary;
    }

    public String[] getCategoryIds() {
        return categoryIds;
    }

    public String[] getCategoryNames() {
        return categoryNames;
    }

    public String getStatus() {
        return status;
    }

    public String getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return "RegStudentOrg{" +
                "searchScore=" + searchScore +
                ", id='" + id + '\'' +
                ", institutionId=" + institutionId +
                ", parentOrganizationId=" + parentOrganizationId +
                ", branchId=" + branchId +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", websiteKey='" + websiteKey + '\'' +
                ", profilePictureLink='" + profilePictureLink + '\'' +
                ", description='" + description + '\'' +
                ", summary='" + summary + '\'' +
                ", categoryIds=" + Arrays.toString(categoryIds) +
                ", categoryNames=" + Arrays.toString(categoryNames) +
                ", status='" + status + '\'' +
                ", visibility='" + visibility + '\'' +
                '}';
    }
}
