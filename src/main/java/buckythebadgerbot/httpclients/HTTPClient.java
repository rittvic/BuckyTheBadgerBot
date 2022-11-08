package buckythebadgerbot.httpclients;

import buckythebadgerbot.pojo.Professor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A dedicated HTTP Client class to handle all HTTP requests for the bot
 * Uses the built-in java.net.http HttpClient.
 * NOTE: May decide to switch to OkHttp library for simplifications.
 * Parses JSON responses using `Jakarta JSON Processing` library
 */
public class HTTPClient {

    //Madgrade URL
    private static final String BASE_URL = "https://api.madgrades.com";
    //RMP URL
    private static final String BASE_URL1 = "https://www.ratemyprofessors.com/graphql";
    //Recwell (gym) URL
    private static final String BASE_URL2 = "https://goboardapi.azurewebsites.net/api/FacilityCount/GetCountsByAccount?AccountAPIKey=7938FC89-A15C-492D-9566-12C961BC1F27";
    //Dining hall URL
    private static final String BASE_URL3 = "https://wisc-housingdining.nutrislice.com/menu/api/weeks/school/";

    //The standard timestamp format
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final String apiKey;
    private final HttpClient httpClient;

    /**
     * Creates an instance of HTTP Client
     * @param apiKey the API token
     */
    public HTTPClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public HTTPClient(){
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
        String url = BASE_URL + "/v1/courses?query=" + URLEncoder.encode(courseAndNumber, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.println(values);
            return values;
        }
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();
        try {
            values.add(jsonObject.asJsonObject().getJsonArray("results").getJsonObject(0).getJsonString("uuid").toString().replaceAll("\"", ""));
            values.add(jsonObject.asJsonObject().getJsonArray("results").getJsonObject(0).getJsonNumber("number").toString());
            values.add(jsonObject.asJsonObject().getJsonArray("results").getJsonObject(0).getJsonArray("subjects").getJsonObject(0).getJsonString("code").toString().replaceAll("\"", ""));
            //replace certain characters to best match the abbreviations from madgrades with guide.wisc.edu
            values.add(jsonObject.asJsonObject().getJsonArray("results").getJsonObject(0).getJsonArray("subjects").getJsonObject(0).getJsonString("abbreviation").toString().
                    replaceAll(" ", "_").
                    replaceAll("-","_").
                    replaceAll("&_","").
                    replaceAll("&","_").
                    replaceAll("\"", "").toLowerCase());
            values.add(jsonObject.asJsonObject().getJsonArray("results").getJsonObject(0).getJsonString("name").toString().replaceAll("\"", ""));

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
        String url = BASE_URL + "/v1/courses/" + ID + "/grades";
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return String.valueOf(values);
        }
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();

        //Parse through the JSON for the number of A,AB,B,BC,C,D,F grade points
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("aCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("abCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("bCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("bcCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("cCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("dCount").intValue());
        values.add(jsonObject.asJsonObject().getJsonObject("cumulative").getJsonNumber("fCount").intValue());

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
    public ArrayList<JsonObject> courseQuery(String course) {
        ArrayList<JsonObject> results = new ArrayList<>();
        String url = BASE_URL + "/v1/courses?query=" + URLEncoder.encode(course, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().header("Authorization", "Token " + this.apiKey).GET().uri(URI.create(url)).build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            return results;
        }
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();

        int numResults = jsonObject.asJsonObject().getJsonArray("results").size();

        //If total number of results exceeds ten, fetch only top ten results. Else fetch all the results.
        if (numResults >= 10) {
            for (int i = 0; i < 10; i++) {
                results.add(jsonObject.asJsonObject().getJsonArray("results").get(i).asJsonObject());
            }
        } else if (numResults == 0) {
            return results;
        } else {
            for (int i = 0; i < (numResults); i++) {
                results.add(jsonObject.asJsonObject().getJsonArray("results").get(i).asJsonObject());
            }
        }
        return results;
    }

    /**
     * Fetches information about a professor
     * Sends two POST requests to ratemyprofessors.com/graphql with a Bearer token
     * NOTE: The request payloads are extremely long
     *
     * @param profName the name of the professor
     * @return A Professor object with fetched information
     */
    public Professor profInfo(String profName) {
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
                .uri(URI.create(BASE_URL1)).build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return new Professor(false);
        }
        ArrayList<String> profInformation = new ArrayList<>();
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();

        //Pick the first UW-Madison professor that shows up in the results. If none are there, it is set to the very first result (also by default).
        int numProfs = jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").size();
        int node = 0;
        for (int i = 0; i < numProfs; i++){
            String school = jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").getJsonObject(i).
                    getJsonObject("node").getJsonObject("school").getJsonString("name").toString().replace("\"", "");
            if (school.equalsIgnoreCase("University of Wisconsin - Madison")){
                node = i;
            }
        }

        //Check to see if the professor exists. If it does, then fetch the id, legacy id and wouldTakeAgainPercent
        try {
            profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").getJsonObject(node).
                    getJsonObject("node").getJsonObject("school").getJsonString("name").toString().replace("\"", ""));
            profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").getJsonObject(node).
                    getJsonObject("node").getJsonString("id").toString().replace("\"", ""));
            profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").getJsonObject(node).
                    getJsonObject("node").getJsonNumber("legacyId").toString().replace("\"", ""));
            profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("search").getJsonObject("teachers").getJsonArray("edges").getJsonObject(node).
                    getJsonObject("node").getJsonNumber("wouldTakeAgainPercent").toString());
        } catch (IndexOutOfBoundsException e) {
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
                .uri(URI.create(BASE_URL1)).build();
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return new Professor(false);
        }
        reader = Json.createReader(new StringReader(response.body()));
        jsonObject = reader.readObject();

        //Fetch the basic information of the professor
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonNumber("avgRating").toString());
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonNumber("avgDifficulty").toString());
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonNumber("numRatings").toString());
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonString("firstName").toString().replace("\"", ""));
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonString("lastName").toString().replace("\"", ""));
        profInformation.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonString("department").toString().replace("\"", ""));

        //Fetch the top five reviews (or all if the total reviews is less than five) of the professor and store it in a List
        ArrayList<JsonObject> profReviews = new ArrayList<>();
        int numReviews = jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonArray("teacherRatingTags").size();
        for (int i = 0; i < (numReviews); i++) {
            profReviews.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonArray("teacherRatingTags").getJsonObject(i));
        }

        List<String> topFiveReviews;
        if (numReviews >= 5) {
            topFiveReviews = profReviews.stream()
                    .sorted(Comparator.comparingInt((JsonObject obj) -> obj.getInt("tagCount")).reversed())
                    .map((JsonObject obj) -> obj.getString("tagName")).toList().subList(0, 5);
        } else {
            topFiveReviews = profReviews.stream()
                    .sorted(Comparator.comparingInt((JsonObject obj) -> obj.getInt("tagCount")).reversed())
                    .map((JsonObject obj) -> obj.getString("tagName")).toList();
        }

        //Fetch the courses taught by the professor and store it in a List
        ArrayList<JsonObject> profCourses = new ArrayList<>();
        int numCourses = jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonArray("courseCodes").size();
        for(int i = 0; i < (numCourses); i++){
            profCourses.add(jsonObject.asJsonObject().getJsonObject("data").getJsonObject("node").getJsonArray("courseCodes").getJsonObject(i));
        }

        List<String> sortedProfCourses;
        if(numCourses >= 10){
            sortedProfCourses = profCourses.stream()
                    .sorted(Comparator.comparingInt((JsonObject obj) -> obj.getInt("courseCount")).reversed())
                    .map((JsonObject obj) -> obj.getString("courseName")).toList().subList(0,10);
        } else{
            sortedProfCourses = profCourses.stream()
                    .sorted(Comparator.comparingInt((JsonObject obj) -> obj.getInt("courseCount")).reversed())
                    .map((JsonObject obj) -> obj.getString("courseName")).toList();
        }


        //If the professor exists, but doesn't teach at UW-Madison, returns the Professor object saying so
        if(!profInformation.get(0).equalsIgnoreCase("University of Wisconsin - Madison")){
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
    public ArrayList<HashMap<String, String>> gymLookup(){
        ArrayList<HashMap<String, String>> gymInformation = new ArrayList<>();

        //Make GET request
        HttpRequest request = HttpRequest.newBuilder().GET().timeout(Duration.ofSeconds(5)).uri(URI.create(BASE_URL2)).build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return null;
        }

        //Parse the JSON response
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        //Sort the JSON Array based on "LastCount" int value in descending order
        List<JsonValue> descendingJsonList = reader.readArray().stream()
                .sorted(Comparator.comparingInt( (JsonValue count) -> count.asJsonObject().getJsonNumber("LastCount").intValue())
                        .reversed()).toList();

        //Set up HashMap for the two facilities
        LinkedHashMap<String, String> nickFacility = new LinkedHashMap<>();
        LinkedHashMap<String, String> shellFacility = new LinkedHashMap<>();


        //Iterate through the list of JSON values
        for (JsonValue equipment : descendingJsonList) {
            //Store the respective information
            String facilityName = String.valueOf(equipment.asJsonObject().getJsonString("FacilityName")).replaceAll("\"", "");
            String locationName =  String.valueOf(equipment.asJsonObject().getJsonString("LocationName")).replaceAll("\"", "");
            String currentCount = String.valueOf(equipment.asJsonObject().asJsonObject().getJsonNumber("LastCount"));
            String totalCapacity = String.valueOf(equipment.asJsonObject().asJsonObject().getJsonNumber("TotalCapacity"));
            String lastUpdatedTime;
            try{
                //Convert the standard timestamp (GMT-5) to unix timestamp (epoch)
                Date dt = sdf.parse(String.valueOf(equipment.asJsonObject().asJsonObject().getJsonString("LastUpdatedDateAndTime")).replaceAll("\"", ""));

                //We have to add five hours since the prod bot treats it as GMT-10
                long epoch = dt.getTime() + TimeUnit.HOURS.toMillis(5);

                lastUpdatedTime = String.valueOf((int)(epoch/1000));
            } catch (ParseException parseException){
                return null;
            }

            //Add the locations to the respective hashmap
            if (facilityName.equals("Nicholas Recreation Center")){
                nickFacility.put(facilityName + "|" + locationName, "Usage: `" +  currentCount + "/" + totalCapacity + "`" + "\n"
                        + "Last updated: " + "<t:" + lastUpdatedTime + ">");
            } else if (facilityName.equals("Shell")){
                shellFacility.put(facilityName + "|" + locationName, "Usage: `" +  currentCount + "/" + totalCapacity + "`" + "\n"
                        + "Last updated: " +  "<t:" + lastUpdatedTime + ">");
            }
        }

        //add every hashmap to the ArrayList
        gymInformation.add(nickFacility);
        gymInformation.add(shellFacility);

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
        String date = "\""+ LocalDate.now(TimeZone.getTimeZone("US/Central").toZoneId())+"\"";

        //Call the API
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL3+diningMarket+"/menu-type/"+menuType+"/"
                        +date.replaceAll("-","/").replaceAll("\"","")+"/"))
                .build();
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        JsonReader reader = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();

        //Parse the JSON response
        try{
            //Get the total number of days in the menu week (usually 7)
            int numDays = jsonObject.asJsonObject().getJsonArray("days").size();

            //Declare JsonObject day
            JsonObject day = null;
            //Iterate through every day of week in the menu
            for (int i = 0; i < numDays; i++){
                //Check if the day matches with the passed day
                if (jsonObject.asJsonObject().getJsonArray("days").get(i).asJsonObject().getJsonString("date").toString().equals(date)){
                    //Assign JsonObject day to the matched day and break out of the loop
                    day = jsonObject.asJsonObject().getJsonArray("days").get(i).asJsonObject();
                    break;
                }
            }

            //LinkedHashMap in the format of {stationID-0stationName-0foodCategory = String containing all the items in the station and its category
            LinkedHashMap<String,String> stations = new LinkedHashMap<>();

            //HashMap to retrieve the stationID-0stationName-0foodCategory key from above HashMap by getting the station ID alone
            HashMap<String,String> stationsKey = new HashMap<>();

            //Get total number of stations
            int numStations = day.asJsonObject().getJsonObject("menu_info").size();

            //Iterate through every station
            for (Map.Entry<String,JsonValue> entry : day.asJsonObject().getJsonObject("menu_info").entrySet()){
                //Obtain station ID (menu_ID)
                String stationID = entry.getKey();
                //Obtain station name
                String stationName = entry.getValue().asJsonObject().getJsonObject("section_options").getString("display_name");
                //Store the station ID associated with the respective key format
                stationsKey.put(stationID,stationID + "-0" + stationName);
            }

            //Get total number of food items
            int numItems = day.getJsonArray("menu_items").size();

            //Iterate through every food item
            for (int i = 0; i < numItems; i++){
                //Check if the value type is not NULL
                if (day.getJsonArray("menu_items").get(i).asJsonObject().get("food").getValueType() != JsonValue.ValueType.NULL){
                    //Obtain station ID (menu_id) associated with the food item
                    String stationID = day.getJsonArray("menu_items").get(i).asJsonObject().getJsonNumber("menu_id").toString();
                    //Put Entree and Side categories first
                    stations.putIfAbsent(stationsKey.get(stationID) + "-0" + "Entree", null);
                    stations.putIfAbsent(stationsKey.get(stationID) + "-0" + "Side", null);
                    //Obtain name of the food item
                    String foodName = day.getJsonArray("menu_items").get(i).asJsonObject().get("food").asJsonObject().getString("name");
                    //Get the category of the food (entree or side)
                    String foodCategory = day.getJsonArray("menu_items").get(i).asJsonObject().get("food").asJsonObject().getString("food_category");
                    if (foodCategory.isBlank()){
                        foodCategory = "unknown";
                    }
                    //Uppercase the first letter of food category
                    foodCategory = foodCategory.substring(0,1).toUpperCase() + foodCategory.substring(1);
                    //Get the associated key with the station ID and add the food category to it
                    String key = stationsKey.get(stationID) + "-0" + foodCategory;
                    //Get the calories of the food
                    String calories = day.getJsonArray("menu_items").get(i).asJsonObject().get("food").asJsonObject().getJsonObject("rounded_nutrition_info").get("calories").toString().replace(".0","");
                    if (calories.equals("null")){
                        calories = "N/A";
                    }

                    //{stationID-0stationName-0foodCategory = String containing all the items in the station and its category
                    if (stations.get(key) == null){
                        stations.put(key,"\n" + ":arrow_forward: **" +  foodName + "**"  + "\n" + calories + " Cal");
                    } else {
                        stations.put(key,stations.get(key) + "\n" + ":arrow_forward: **" +  foodName + "**"  + "\n" + calories + " Cal");
                    }
                }
            }
            return stations;
            //Catch any null pointer exceptions and return a null hashmap
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
