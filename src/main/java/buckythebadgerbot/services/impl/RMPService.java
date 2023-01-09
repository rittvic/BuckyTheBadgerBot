package buckythebadgerbot.services.impl;

import buckythebadgerbot.data.Professor;
import buckythebadgerbot.data.StudentRating;
import buckythebadgerbot.services.APIService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RMPService extends APIService {
    private static final String BASE_URL = "https://www.ratemyprofessors.com/graphql";

    private static final Logger logger = LoggerFactory.getLogger(RMPService.class);

    public RMPService(String apiKey) {
        super(apiKey);
    }

    /**
     * Fetches information about a professor
     * Sends two POST requests to ratemyprofessors.com/graphql with a Bearer token
     * NOTE: The request payloads are extremely long
     *
     * @param profName the name of the professor
     * @return A Professor object with fetched information
     */
    public Professor getProf(String profName) {
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
                .uri(URI.create(BASE_URL)).build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        Professor prof = new Professor();
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong with the API request! {}",e.toString());
            prof.setDoesExist(false);
            return prof;
        }
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
            prof.setSchool(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("school").get("name").asText());
            prof.setRegularId(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("id").asText());
            prof.setLegacyId(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("legacyId").asText());
            prof.setWouldTakeAgainPercent(jsonNode.get("data").get("search").get("teachers").withArray("edges").get(node).get("node").get("wouldTakeAgainPercent").asDouble());

        } catch (NullPointerException e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            prof.setDoesExist(false);
            return prof;
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
                        "n\\nfragment TeacherTitles_teacher on Teacher {\\n  department\\n  school {\\n    legacyId\\n    name\\n    id\\n  }\\n}\\n\",\"variables\":{\"id\":\"" + prof.getRegularId() + "\"}}"))
                .uri(URI.create(BASE_URL)).build();

        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong with the API request! {}",e.toString());
            prof.setDoesExist(false);
            return prof;
        }

        List<String> topFiveTags;
        List<String> sortedProfCourses;
        try {
            //Fetch the basic information of the professor
            prof.setAvgRating(jsonNode.get("data").get("node").get("avgRating").asDouble());
            prof.setAvgDifficulty(jsonNode.get("data").get("node").get("avgDifficulty").asDouble());
            prof.setNumRating(jsonNode.get("data").get("node").get("numRatings").asInt());
            prof.setFirstName(jsonNode.get("data").get("node").get("firstName").asText());
            prof.setLastName(jsonNode.get("data").get("node").get("lastName").asText());
            prof.setDepartment(jsonNode.get("data").get("node").get("department").asText());

            //If the professor exists, but doesn't teach at UW-Madison, set fallback and does exist to true, and return the professor
            if (!prof.getSchool().equalsIgnoreCase("University of Wisconsin - Madison")) {
                prof.setFallback(true);
                prof.setDoesExist(true);
                return prof;
            }

            //Fetch the top five reviews (or all if the total reviews is less than five) of the professor and store it in a List
            ArrayNode teacherRatingTags = jsonNode.get("data").get("node").withArray("teacherRatingTags");
            ArrayList<JsonNode> profReviews = new ArrayList<>();
            for (JsonNode ratingTag : teacherRatingTags) {
                profReviews.add(ratingTag);
            }
            topFiveTags = profReviews.stream()
                    .sorted(Comparator.comparingInt((JsonNode obj) -> obj.get("tagCount").asInt()).reversed())
                    .map((JsonNode obj) -> obj.get("tagName").asText()).toList();
            if (topFiveTags.size() >= 5) {
                topFiveTags = topFiveTags.subList(0, 5);
            }
            prof.setTopFiveTags(topFiveTags);

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
            prof.setCoursesTaught(sortedProfCourses);

        } catch (Exception e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            prof.setDoesExist(false);
            return prof;
        }
        return prof;
    }

    /**
     * Fetches a list of student ratings on a particular course taught by a professor
     * @param profRegularId the id of the professor
     * @param course the course to fetch student ratings from
     * @return a list of StudentRating objects
     */
    public ArrayList<StudentRating> getStudentRatings (String profRegularId, String course){
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Basic " + this.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"query RatingsListQuery(\\n  $count: Int!\\n  $id: ID!\\n  $courseFilter: String\\n  " +
                        "$cursor: String\\n) {\\n  node(id: $id) {\\n    __typename\\n    ... on Teacher {\\n      ...RatingsList_teacher_4pguUW\\n    }\\n    " +
                        "id\\n  }\\n}\\n\\nfragment RatingsList_teacher_4pguUW on Teacher {\\n  id\\n  legacyId\\n  lastName\\n  numRatings\\n  school {\\n    id\\n   " +
                        " legacyId\\n    name\\n    city\\n    state\\n    avgRating\\n    numRatings\\n  }\\n  ...Rating_teacher\\n  ...NoRatingsArea_teacher\\n  ratings(first:" +
                        " $count, after: $cursor, courseFilter: $courseFilter) {\\n    edges {\\n      cursor\\n      node {\\n        ...Rating_rating\\n        id\\n      " +
                        "  __typename\\n      }\\n    }\\n    pageInfo {\\n      hasNextPage\\n      endCursor\\n    }\\n  }\\n}\\n\\nfragment Rating_teacher on Teacher {\\n " +
                        " ...RatingFooter_teacher\\n  ...RatingSuperHeader_teacher\\n  ...ProfessorNoteSection_teacher\\n}\\n\\nfragment NoRatingsArea_teacher on Teacher {\\n " +
                        " lastName\\n  ...RateTeacherLink_teacher\\n}\\n\\nfragment Rating_rating on Rating {\\n  comment\\n  flagStatus\\n  createdByUser\\n  teacherNote {\\n  " +
                        "  id\\n  }\\n  ...RatingHeader_rating\\n  ...RatingSuperHeader_rating\\n  ...RatingValues_rating\\n  ...CourseMeta_rating\\n  ...RatingTags_rating\\n  " +
                        "...RatingFooter_rating\\n  ...ProfessorNoteSection_rating\\n}\\n\\nfragment RatingHeader_rating on Rating {\\n  date\\n  class\\n  helpfulRating\\n  " +
                        "clarityRating\\n  isForOnlineClass\\n}\\n\\nfragment RatingSuperHeader_rating on Rating {\\n  legacyId\\n}\\n\\nfragment RatingValues_rating on Rating {\\n  " +
                        "helpfulRating\\n  clarityRating\\n  difficultyRating\\n}\\n\\nfragment CourseMeta_rating on Rating {\\n  attendanceMandatory\\n  wouldTakeAgain\\n  grade\\n" +
                        "  textbookUse\\n  isForOnlineClass\\n  isForCredit\\n}\\n\\nfragment RatingTags_rating on Rating {\\n  ratingTags\\n}\\n\\nfragment RatingFooter_rating on Rating" +
                        " {\\n  id\\n  comment\\n  adminReviewedAt\\n  flagStatus\\n  legacyId\\n  thumbsUpTotal\\n  thumbsDownTotal\\n  thumbs {\\n    userId\\n    thumbsUp\\n    thumbsDown\\n" +
                        "    id\\n  }\\n  teacherNote {\\n    id\\n  }\\n}\\n\\nfragment ProfessorNoteSection_rating on Rating {\\n  teacherNote {\\n    ...ProfessorNote_note\\n    id\\n  }\\n " +
                        " ...ProfessorNoteEditor_rating\\n}\\n\\nfragment ProfessorNote_note on TeacherNotes {\\n  comment\\n  ...ProfessorNoteHeader_note\\n" +
                        "  ...ProfessorNoteFooter_note\\n}\\n\\nfragment ProfessorNoteEditor_rating on Rating {\\n  id\\n  legacyId\\n  class\\n  teacherNote {\\n " +
                        "   id\\n    teacherId\\n    comment\\n  }\\n}\\n\\nfragment ProfessorNoteHeader_note on TeacherNotes {\\n  createdAt\\n  updatedAt\\n}\\n\\nfragment " +
                        "ProfessorNoteFooter_note on TeacherNotes {\\n  legacyId\\n  flagStatus\\n}\\n\\nfragment RateTeacherLink_teacher on Teacher {\\n  legacyId\\n  numRatings\\n" +
                        "  lockStatus\\n}\\n\\nfragment RatingFooter_teacher on Teacher {\\n  id\\n  legacyId\\n  lockStatus\\n  isProfCurrentUser\\n}\\n\\nfragment " +
                        "RatingSuperHeader_teacher on Teacher {\\n  firstName\\n  lastName\\n  legacyId\\n  school {\\n    name\\n    id\\n  }\\n}\\n\\nfragment ProfessorNoteSection_teacher on Teacher" +
                        " {\\n  ...ProfessorNote_teacher\\n  ...ProfessorNoteEditor_teacher\\n}\\n\\nfragment ProfessorNote_teacher on Teacher {\\n  ...ProfessorNoteHeader_teacher\\n  " +
                        "...ProfessorNoteFooter_teacher\\n}\\n\\nfragment ProfessorNoteEditor_teacher on Teacher {\\n  id\\n}\\n\\nfragment ProfessorNoteHeader_teacher on Teacher {\\n  " +
                        "lastName\\n}\\n\\nfragment ProfessorNoteFooter_teacher on Teacher {\\n  legacyId\\n  isProfCurrentUser\\n}\\n\"," +
                        "\"variables\":{\"count\":100,\"id\":\"" + profRegularId + "\",\"courseFilter\":\"" + course + "\",\"cursor\":null}}"))
                .uri(URI.create(BASE_URL)).build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong with the API request! {}",e.toString());
            return null;
        }
        ArrayList<StudentRating> studentRatings = new ArrayList<>();
        try {
            ArrayNode ratings = jsonNode.get("data").get("node").get("ratings").withArray("edges");
            //Deserialize every rating into a StudentRating object and add the object to the list
            StudentRating studentRating;
            for (JsonNode ratingData : ratings) {
                JsonNode rating = ratingData.get("node");
                studentRating = objectMapper.readValue(rating.toString(), StudentRating.class);
                studentRating.setCourse(course);
                studentRatings.add(studentRating);
            }
            return studentRatings;
        } catch (JsonProcessingException e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            return null;
        }
    }
}
