package buckythebadgerbot.services;

import buckythebadgerbot.pojo.Professor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A dedicated API Handler class to handle all HTTP requests for the bot
 * Uses the built-in java.net.http HttpClient.
 * NOTE: May decide to switch to OkHttp library for simplifications.
 * Parses JSON responses using `Jakarta JSON Processing` library
 */
public class APIHandler {

    //Madgrade URL
    private static final String MADGRADES_URL = "https://api.madgrades.com";
    //RMP URL
    private static final String RMP_URL = "https://www.ratemyprofessors.com/graphql";
    //Recwell (gym) URL
    private static final String RECWELL_URL = "https://goboardapi.azurewebsites.net/api/FacilityCount/GetCountsByAccount?AccountAPIKey=7938FC89-A15C-492D-9566-12C961BC1F27";
    //Dining hall URL
    private static final String DINING_URL = "https://wisc-housingdining.nutrislice.com/menu/api/weeks/school/";

    //The standard timestamp format
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final String apiKey;
    private final HttpClient httpClient;

    /**
     * Creates an instance of HTTP Client
     * @param apiKey the API token
     */
    public APIHandler(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public APIHandler() {
        this.apiKey = null;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches course information using the query param
     * Uses madgrades API with Authorization token
     * @param courseAndNumber course subject and number
     * @return an ArrayList of the following course information: uuid, number, code, abbreviation
     */
    public ArrayList<String> courseLookUp(String courseAndNumber) {
        ArrayList<String> values = new ArrayList<>();
        String url = MADGRADES_URL + "/v1/courses?query=" + URLEncoder.encode(courseAndNumber, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();

        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            System.out.println(values);
            return values;
        }

        try {
            values.add(jsonNode.withArray("results").get(0).get("uuid").asText());
            values.add(jsonNode.withArray("results").get(0).get("number").asText());
            values.add(jsonNode.withArray("results").get(0).withArray("subjects").get(0).get("code").asText());
            //replace certain characters to best match the abbreviations from madgrades with guide.wisc.edu
            values.add(jsonNode.withArray("results").get(0).withArray("subjects").get(0).get("abbreviation").asText()
                    .replaceAll(" ", "_")
                    .replaceAll("-", "_")
                    .replaceAll("&_", "")
                    .replaceAll("&", "_").toLowerCase());
            values.add(jsonNode.withArray("results").get(0).get("name").asText());
        } catch (RuntimeException e) {
            return values;
        }
        return values;
    }

    /**
     * Fetches the cumulative grade points of a course over the data collected by madgrades
     * Uses madgrades API with Authorization token
     * @param ID the course uuid
     * @return the cumulative GPA of the course
     */
    public String courseAverageGPA(String ID) {
        ArrayList<Integer> values = new ArrayList<>();
        String url = MADGRADES_URL + "/v1/courses/" + ID + "/grades";
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();

        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            return String.valueOf(values);
        }

        //Parse through the JSON for the number of A,AB,B,BC,C,D,F grade points
        try {
            values.add(jsonNode.get("cumulative").get("aCount").asInt());
            values.add(jsonNode.get("cumulative").get("abCount").asInt());
            values.add(jsonNode.get("cumulative").get("bCount").asInt());
            values.add(jsonNode.get("cumulative").get("bcCount").asInt());
            values.add(jsonNode.get("cumulative").get("cCount").asInt());
            values.add(jsonNode.get("cumulative").get("dCount").asInt());
            values.add(jsonNode.get("cumulative").get("fCount").asInt());
        } catch (Exception e) {
            return String.valueOf(values);
        }

        //Calculate cumulative GPA
        int totalCount = 0;
        double cumulativeGPA;
        for (Integer number : values) {
            totalCount += number;
        }
        cumulativeGPA = (((values.get(0) * 4) + (values.get(1) * 3.5) + (values.get(2) * 3) + (values.get(3) * 2.5) + (values.get(4) * 2) + (values.get(5) * 1) + (values.get(6) * 0)) / totalCount);
        return String.valueOf(Math.floor(cumulativeGPA * 100) / 100);
    }

    /**
     * Queries the top ten results (or all if total number of results are less than 10) from api.madgrades.com
     * @param course the course to query through
     * @return an ArrayList of JsonObjects that contain the results' course name, number, and subject
     */
    public List<JsonNode> courseQuery(String course) {
        String url = MADGRADES_URL + "/v1/courses?query=" + URLEncoder.encode(course, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();

        HttpResponse<String> response;
        ObjectMapper objectMapper;
        ArrayNode results;
        ArrayList<JsonNode> courseResults = new ArrayList<>();
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            results = objectMapper.readTree(response.body()).withArray("results");
        } catch (InterruptedException | IOException e) {
            return courseResults;
        }

        //If there are more than 10 total results, only add the first 10. Otherwise, add all results.
        if (results.size() >= 10){
            for (int i = 0; i < 10; i++){
                courseResults.add(results.get(i));
            }
        } else if (results.size() == 0){
            return courseResults;
        } else {
            for (int i = 0; i < results.size(); i++){
                courseResults.add(results.get(i));
            }
        }

        return courseResults;
    }

    /**
     * Fetches information about a professor
     * Sends two POST requests to ratemyprofessors.com/graphql with a Bearer token
     * NOTE: The request payloads are extremely long
     *
     * @param profName the name of the professor
     * @return A Professor object with fetched information
     */
    public Professor profLookup(String profName) {
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Basic " + this.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"query TeacherSearchResultsPageQuery(\\n " +
                        " $query: TeacherSearchQuery!\\n  $schoolID: ID\\n) {\\n  search: newSearch {\\n    ...TeacherSearchPagination_search_1ZLmLD\\n  }\\n  school: node(id: $schoolID) {\\n   " +
                        " __typename\\n    ... on School {\\n      name\\n    }\\n    id\\n  }\\n}\\n\\nfragment TeacherSearchPagination_search_1ZLmLD on newSearch {\\n  teachers(query: $query, " +
                        "first: 8, after: \\\"\\\") {\\n    didFallback\\n    edges {\\n      cursor\\n      node {\\n        ...TeacherCard_teacher\\n        id\\n        __typename\\n     " +
                        " }\\n    }\\n    pageInfo {\\n      hasNextPage\\n      endCursor\\n    }\\n    resultCount\\n    filters {\\n      field\\n      options {\\n        value\\n        id\\n   " +
                        "   }\\n    }\\n  }\\n}\\n\\nfragment TeacherCard_teacher on Teacher {\\n  id\\n  legacyId\\n  avgRating\\n  numRatings\\n  ...CardFeedback_teacher\\n  ...CardSchool_teacher\\n " +
                        " ...CardName_teacher\\n  ...TeacherBookmark_teacher\\n}\\n\\nfragment CardFeedback_teacher on Teacher {\\n  wouldTakeAgainPercent\\n  avgDifficulty\\n}\\n\\nfragment CardSchool_teacher on Teacher {\\n" +
                        "  department\\n  school {\\n    name\\n    id\\n  }\\n}\\n\\nfragment CardName_teacher on Teacher {\\n  firstName\\n  lastName\\n}\\n\\nfragment TeacherBookmark_teacher on Teacher {\\n  id\\n  " +
                        "isSaved\\n}\\n\",\"variables\":{\"query\":{\"text\":\"" + profName + "\",\"schoolID\":\"U2Nob29sLTE4NDE4\",\"fallback\":true,\"departmentID\":null},\"schoolID\":\"U2Nob29sLTE4NDE4\"}}"))
                .uri(URI.create(RMP_URL)).build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            return new Professor(false);
        }
        ArrayList<String> profInformation = new ArrayList<>();
        try {
            //Pick the first UW-Madison professor that shows up in the results. If none are there, it is set to the very first result (also by default).
            int numResults = jsonNode.get("data").get("search").get("teachers").withArray("edges").size();
            int node = 0;
            for (int i = 0; i < numResults; i++) {
                String school = jsonNode.get("data").get("search").get("teachers").withArray("edges").get(i).get("node").get("school").get("name").asText();
                if (school.equalsIgnoreCase("University of Wisconsin - Madison")) {
                    node = i;
                }
            }
            //Check to see if the professor exists. If it does, then fetch the id, legacy id and wouldTakeAgainPercent
            profInformation.add(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("school").get("name").asText());
            profInformation.add(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("id").asText());
            profInformation.add(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("legacyId").asText());
            profInformation.add(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("wouldTakeAgainPercent").asText());
        } catch (NullPointerException e) {
            return new Professor(false);
        }

        //Sending a new request with id as parameter to fetch more information
        request = HttpRequest.newBuilder()
                .header("Authorization", "Basic " + this.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"query TeacherRatingsPageQuery(\\n  $id: ID!\\n) {\\n  node(id: $id) {\\n    __typename\\n    ... on Teacher {\\n      id\\n      legacyId\\n    " +
                        "  firstName\\n      lastName\\n      school {\\n        legacyId\\n        name\\n        id\\n      }\\n      lockStatus\\n      ...StickyHeader_teacher\\n      ...RatingDistributionWrapper_teacher" +
                        "\\n      ...TeacherMetaInfo_teacher\\n      ...TeacherInfo_teacher\\n      ...SimilarProfessors_teacher\\n      ...TeacherRatingTabs_teacher\\n    }\\n    id\\n  }\\n}\\n\\nfragment StickyHeader_teac" +
                        "her on Teacher {\\n  ...HeaderDescription_teacher\\n  ...HeaderRateButton_teacher\\n}\\n\\nfragment RatingDistributionWrapper_teacher on Teacher {\\n  ...NoRatingsArea_teacher\\n  ratingsDistribution {" +
                        "\\n    total\\n    ...RatingDistributionChart_ratingsDistribution\\n  }\\n}\\n\\nfragment TeacherMetaInfo_teacher on Teacher {\\n  legacyId\\n  firstName\\n  lastName\\n  department\\n  school {\\n    n" +
                        "ame\\n    city\\n    state\\n    id\\n  }\\n}\\n\\nfragment TeacherInfo_teacher on Teacher {\\n  id\\n  lastName\\n  numRatings\\n  ...RatingValue_teacher\\n  ...NameTitle_teacher\\n  ...TeacherTags_teach" +
                        "er\\n  ...NameLink_teacher\\n  ...TeacherFeedback_teacher\\n  ...RateTeacherLink_teacher\\n}\\n\\nfragment SimilarProfessors_teacher on Teacher {\\n  department\\n  relatedTeachers {\\n    legacyId\\n    ." +
                        "..SimilarProfessorListItem_teacher\\n    id\\n  }\\n}\\n\\nfragment TeacherRatingTabs_teacher on Teacher {\\n  numRatings\\n  courseCodes {\\n    courseName\\n    courseCount\\n  }\\n  ...RatingsList_t" +
                        "eacher\\n  ...RatingsFilter_teacher\\n}\\n\\nfragment RatingsList_teacher on Teacher {\\n  id\\n  legacyId\\n  lastName\\n  numRatings\\n  school {\\n    id\\n    legacyId\\n    name\\n    city\\n    " +
                        "state\\n    avgRating\\n    numRatings\\n  }\\n  ...Rating_teacher\\n  ...NoRatingsArea_teacher\\n  ratings(first: 20) {\\n    edges {\\n      cursor\\n      node {\\n        ...Rating_rating\\n        " +
                        "id\\n        __typename\\n      }\\n    }\\n    pageInfo {\\n      hasNextPage\\n      endCursor\\n    }\\n  }\\n}\\n\\nfragment RatingsFilter_teacher on Teacher {\\n  courseCodes {\\n    courseCount\\n " +
                        "   courseName\\n  }\\n}\\n\\nfragment Rating_teacher on Teacher {\\n  ...RatingFooter_teacher\\n  ...RatingSuperHeader_teacher\\n  ...ProfessorNoteSection_teacher\\n}\\n\\nfragment NoRatingsArea_teacher on" +
                        " Teacher {\\n  lastName\\n  ...RateTeacherLink_teacher\\n}\\n\\nfragment Rating_rating on Rating {\\n  comment\\n  flagStatus\\n  createdByUser\\n  teacherNote {\\n    id\\n  }\\n  ...RatingHeader_rating\\n  ...R" +
                        "atingSuperHeader_rating\\n  ...RatingValues_rating\\n  ...CourseMeta_rating\\n  ...RatingTags_rating\\n  ...RatingFooter_rating\\n  ...ProfessorNoteSection_rating\\n}\\n\\nfragment RatingHeader_rating on " +
                        "Rating {\\n  date\\n  class\\n  helpfulRating\\n  clarityRating\\n  isForOnlineClass\\n}\\n\\nfragment RatingSuperHeader_rating on Rating {\\n  legacyId\\n}\\n\\nfragment RatingValues_rating on Rating {" +
                        "\\n  helpfulRating\\n  clarityRating\\n  difficultyRating\\n}\\n\\nfragment CourseMeta_rating on Rating {\\n  attendanceMandatory\\n  wouldTakeAgain\\n  grade\\n  textbookUse\\n  isForOnlineClass\\n  isF" +
                        "orCredit\\n}\\n\\nfragment RatingTags_rating on Rating {\\n  ratingTags\\n}\\n\\nfragment RatingFooter_rating on Rating {\\n  id\\n  comment\\n  adminReviewedAt\\n  flagStatus\\n  legacyId\\n  thumbsUpT" +
                        "otal\\n  thumbsDownTotal\\n  thumbs {\\n    userId\\n    thumbsUp\\n    thumbsDown\\n    id\\n  }\\n  teacherNote {\\n    id\\n  }\\n}\\n\\nfragment ProfessorNoteSection_rating on Rating {\\n  teacherNo" +
                        "te {\\n    ...ProfessorNote_note\\n    id\\n  }\\n  ...ProfessorNoteEditor_rating\\n}\\n\\nfragment ProfessorNote_note on TeacherNotes {\\n  comment\\n  ...ProfessorNoteHeader_note\\n  ...ProfessorNoteFo" +
                        "oter_note\\n}\\n\\nfragment ProfessorNoteEditor_rating on Rating {\\n  id\\n  legacyId\\n  class\\n  teacherNote {\\n    id\\n    teacherId\\n    comment\\n  }\\n}\\n\\nfragment ProfessorNoteHeader_not" +
                        "e on TeacherNotes {\\n  createdAt\\n  updatedAt\\n}\\n\\nfragment ProfessorNoteFooter_note on TeacherNotes {\\n  legacyId\\n  flagStatus\\n}\\n\\nfragment RateTeacherLink_teacher on Teacher {\\n  legac" +
                        "yId\\n  numRatings\\n  lockStatus\\n}\\n\\nfragment RatingFooter_teacher on Teacher {\\n  id\\n  legacyId\\n  lockStatus\\n  isProfCurrentUser\\n}\\n\\nfragment RatingSuperHeader_teacher on Teacher {\\n" +
                        "  firstName\\n  lastName\\n  legacyId\\n  school {\\n    name\\n    id\\n  }\\n}\\n\\nfragment ProfessorNoteSection_teacher on Teacher {\\n  ...ProfessorNote_teacher\\n  ...ProfessorNoteEditor_teacher\\" +
                        "n}\\n\\nfragment ProfessorNote_teacher on Teacher {\\n  ...ProfessorNoteHeader_teacher\\n  ...ProfessorNoteFooter_teacher\\n}\\n\\nfragment ProfessorNoteEditor_teacher on Teacher {\\n  id\\n}\\n\\nfragm" +
                        "ent ProfessorNoteHeader_teacher on Teacher {\\n  lastName\\n}\\n\\nfragment ProfessorNoteFooter_teacher on Teacher {\\n  legacyId\\n  isProfCurrentUser\\n}\\n\\nfragment SimilarProfessorListItem_teacher on R" +
                        "elatedTeacher {\\n  legacyId\\n  firstName\\n  lastName\\n  avgRating\\n}\\n\\nfragment RatingValue_teacher on Teacher {\\n  avgRating\\n  numRatings\\n  ...NumRatingsLink_teacher\\n}\\n\\nfragment NameTitle_teach" +
                        "er on Teacher {\\n  id\\n  firstName\\n  lastName\\n  department\\n  school {\\n    legacyId\\n    name\\n    id\\n  }\\n  ...TeacherDepartment_teacher\\n  ...TeacherBookmark_teacher\\n}\\n\\nfragment TeacherTags" +
                        "_teacher on Teacher {\\n  lastName\\n  teacherRatingTags {\\n    legacyId\\n    tagCount\\n    tagName\\n    id\\n  }\\n}\\n\\nfragment NameLink_teacher on Teacher {\\n  isProfCurrentUser\\n  legacyId\\n  lastName" +
                        "\\n}\\n\\nfragment TeacherFeedback_teacher on Teacher {\\n  numRatings\\n  avgDifficulty\\n  wouldTakeAgainPercent\\n}\\n\\nfragment TeacherDepartment_teacher on Teacher {\\n  department\\n  school {\\n    legacyI" +
                        "d\\n    name\\n    id\\n  }\\n}\\n\\nfragment TeacherBookmark_teacher on Teacher {\\n  id\\n  isSaved\\n}\\n\\nfragment NumRatingsLink_teacher on Teacher {\\n  numRatings\\n  ...RateTeacherLink_teacher\\n}\\n\\nf" +
                        "ragment RatingDistributionChart_ratingsDistribution on ratingsDistribution {\\n  r1\\n  r2\\n  r3\\n  r4\\n  r5\\n}\\n\\nfragment HeaderDescription_teacher on Teacher {\\n  id\\n  firstName\\n  lastName\\n  depar" +
                        "tment\\n  school {\\n    legacyId\\n    name\\n    id\\n  }\\n  ...TeacherTitles_teacher\\n  ...TeacherBookmark_teacher\\n}\\n\\nfragment HeaderRateButton_teacher on Teacher {\\n  ...RateTeacherLink_teacher\\n}\\" +
                        "n\\nfragment TeacherTitles_teacher on Teacher {\\n  department\\n  school {\\n    legacyId\\n    name\\n    id\\n  }\\n}\\n\",\"variables\":{\"id\":\"" + profInformation.get(1) + "\"}}"))
                .uri(URI.create(RMP_URL)).build();

        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            return new Professor(false);
        }

        List<String> topFiveReviews;
        List<String> sortedProfCourses;
        try {
            //Fetch the basic information of the professor
            profInformation.add(jsonNode.get("data").get("node").get("avgRating").asText());
            profInformation.add(jsonNode.get("data").get("node").get("avgDifficulty").asText());
            profInformation.add(jsonNode.get("data").get("node").get("numRatings").asText());
            profInformation.add(jsonNode.get("data").get("node").get("firstName").asText());
            profInformation.add(jsonNode.get("data").get("node").get("lastName").asText());
            profInformation.add(jsonNode.get("data").get("node").get("department").asText());

            //Fetch the top five reviews (or all if the total reviews is less than five) of the professor and store it in a List
            ArrayNode teacherRatingTags = jsonNode.get("data").get("node").withArray("teacherRatingTags");
            ArrayList<JsonNode> profReviews = new ArrayList<>();
            for (JsonNode ratingTag : teacherRatingTags) {
                profReviews.add(ratingTag);
            }
            topFiveReviews = profReviews.stream()
                    .sorted(Comparator.comparingInt((JsonNode obj) -> obj.get("tagCount").asInt()).reversed())
                    .map((JsonNode obj) -> obj.get("tagName").asText()).toList();
            if (topFiveReviews.size() >= 5) {
                topFiveReviews = topFiveReviews.subList(0, 5);
            }

            //Fetch the courses taught by the professor and store it in a List
            ArrayNode teacherCourses = jsonNode.get("data").get("node").withArray("courseCodes");
            ArrayList<JsonNode> profCourses = new ArrayList<>();
            for (JsonNode courseTaught : teacherCourses) {
                profCourses.add(courseTaught);
            }
            sortedProfCourses = profCourses.stream()
                    .sorted(Comparator.comparingInt((JsonNode obj) -> obj.get("courseCount").asInt()).reversed())
                    .map((JsonNode obj) -> obj.get("courseName").asText()).toList();
            if (sortedProfCourses.size() >= 10) {
                sortedProfCourses = sortedProfCourses.subList(0, 10);
            }
        } catch (Exception e) {
            return new Professor(false);
        }

        //If the professor exists, but doesn't teach at UW-Madison, returns the Professor object saying so
        if (!profInformation.get(0).equalsIgnoreCase("University of Wisconsin - Madison")) {
            return new Professor(true, true, profInformation.get(7), profInformation.get(8));

            //Return Professor object with the all the fetched information
        } else {
            return new Professor(true, profInformation.get(1), profInformation.get(2), Double.parseDouble(profInformation.get(3)), Double.parseDouble(profInformation.get(4)), Double.parseDouble(profInformation.get(5)),
                    Integer.parseInt(profInformation.get(6)), profInformation.get(7), profInformation.get(8), profInformation.get(9), topFiveReviews, sortedProfCourses);
        }
    }

    /**
     * Fetch live usage of every gym facility/location
     * @return An ArrayList of HashMaps for every main facility
     */
    public ArrayList<HashMap<String, String>> gymLookup() {
        HttpRequest request = HttpRequest.newBuilder().GET().timeout(Duration.ofSeconds(5)).uri(URI.create(RECWELL_URL)).build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        List<JsonNode> gymEquipments;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            gymEquipments = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException | InterruptedException e) {
            return null;
        }
        ArrayList<HashMap<String, String>> gymInformation = new ArrayList<>();
        try {
            //Sort the equipments based on "LastCount" value in descending order
            gymEquipments = gymEquipments.stream().sorted(Comparator.comparingInt((JsonNode obj) -> obj.get("LastCount").asInt()).reversed()).toList();

            //Set up HashMaps for the two facilities
            LinkedHashMap<String, String> nickFacility = new LinkedHashMap<>();
            LinkedHashMap<String, String> shellFacility = new LinkedHashMap<>();

            //Iterate through the list and store the respective information
            for (JsonNode equipment : gymEquipments) {
                String facilityName = equipment.get("FacilityName").asText();
                String locationName = equipment.get("LocationName").asText();
                String currentCount = equipment.get("LastCount").asText();
                String totalCapacity = equipment.get("TotalCapacity").asText();
                String lastUpdatedTime;

                //Convert the standard timestamp (GMT-5) to unix timestamp (epoch)
                Date dt = sdf.parse(equipment.get("LastUpdatedDateAndTime").asText());
                //We have to add six hours since the prod bot is not based in UTC-5
                long epoch = dt.getTime() + TimeUnit.HOURS.toMillis(6);
                lastUpdatedTime = String.valueOf((int) (epoch / 1000));

                //Add the locations to the respective hashmap
                if (facilityName.equals("Nicholas Recreation Center")) {
                    nickFacility.put(facilityName + "|" + locationName, "Usage: `" + currentCount + "/" + totalCapacity + "`" + "\n"
                            + "Last updated " + "<t:" + lastUpdatedTime + ":R>");
                } else if (facilityName.equals("Shell")) {
                    shellFacility.put(facilityName + "|" + locationName, "Usage: `" + currentCount + "/" + totalCapacity + "`" + "\n"
                            + "Last updated " + "<t:" + lastUpdatedTime + ":R>");
                }
            }

            gymInformation.add(nickFacility);
            gymInformation.add(shellFacility);
        } catch (Exception e) {
            return null;
        }
        return gymInformation;
    }

    /**
     * Fetch a menu containing the food items of every food station at the passed in dining market
     * @param diningMarket the dining market to fetch the menu from
     * @param menuType the type of menu (Breakfast, Lunch, or Dinner)
     * @return HashMap containing every food station and its sub-categories matching with all food items in the sub-category
     */
    public HashMap<String,String> diningMenuLookup(String diningMarket, String menuType){

        //Get the current date in CST
        String date = "\"" + LocalDate.now(TimeZone.getTimeZone("US/Central").toZoneId()) + "\"";

        //Call the API
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(DINING_URL + diningMarket + "/menu-type/" + menuType + "/"
                        + date.replaceAll("-", "/").replaceAll("\"", "") + "/"))
                .build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        try {
            ArrayNode days = jsonNode.withArray("days");
            JsonNode day = null;
            //Iterate through every day until it reaches the specified date
            for (JsonNode dayOfWeek : days) {
                if (dayOfWeek.get("date").toString().equals(date)) {
                    day = dayOfWeek;
                    break;
                }
            }

            //LinkedHashMap in the format of {stationID-0stationName-0foodCategory = String containing all the items in the station and its category
            LinkedHashMap<String, String> stations = new LinkedHashMap<>();

            //HashMap to retrieve the stationID-0stationName-0foodCategory key from above HashMap by getting the station ID alone
            HashMap<String, String> stationsKey = new HashMap<>();

            //Iterate through every station and store its ID and name into the HashMap
            Map<String, JsonNode> menuInfo = objectMapper.convertValue(day.get("menu_info"), new TypeReference<>() {});
            for (Map.Entry<String, JsonNode> entry : menuInfo.entrySet()) {
                String stationID = entry.getKey();
                String stationName = entry.getValue().get("section_options").get("display_name").asText();
                stationsKey.put(stationID, stationID + "-0" + stationName);
            }

            ArrayNode menuItems = day.withArray("menu_items");
            JsonNode food;
            //Iterate through every food item and obtain the following information:
            //Food Station ID, Food Name, Food Category, Food Calories,
            for (JsonNode menuItem : menuItems) {
                if (!menuItem.get("food").isNull()) {
                    food = menuItem.get("food");
                    String stationID = menuItem.get("menu_id").asText();
                    stations.putIfAbsent(stations.get(stationID) + "-0" + "Entree", null);
                    stations.putIfAbsent(stations.get(stationID) + "-0" + "Side", null);
                    String foodName = food.get("name").asText();
                    String foodCategory = food.get("food_category").asText().isBlank() ? "Unknown" : food.get("food_category").asText();
                    foodCategory = foodCategory.substring(0, 1).toUpperCase() + foodCategory.substring(1);
                    String key = stationsKey.get(stationID) + "-0" + foodCategory;
                    String calories = food.get("rounded_nutrition_info").get("calories").asText().replace(".0", "").replace("null", "N/A");

                    //{stationID-0stationName-0foodCategory = String containing all the items in the station and its category
                    if (stations.get(key) == null){
                        stations.put(key,"\n" + "\u25B6 **" +  foodName + "**"  + "\n" + calories + " Cal");
                    } else {
                        stations.put(key,stations.get(key) + "\n" + "\u25B6 **" +  foodName + "**"  + "\n" + calories + " Cal");
                    }
                }
            }
            return stations;
            //Catch any null pointer exceptions and return a null hashmap
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
