package buckythebadgerbot.httpclients;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * A dedicated web-scraper class
 * Mainly used to scrape the following information of a course from guide.wisc.edu:
 * Course Title, Credits, Description, Requisites, Designation, Repeatable for Credit, and Last Taught.
 * Uses JSoup - HTML parser library
 * NOTE: May choose to pre web-scrape all courses and store in a DB to significantly lower response time
 */
public class Scraper {
    private static final String BASE_URL = "https://guide.wisc.edu/courses/";

    /**
     * A static method to scrape the course information
     * @param courseNumber the course number (e.g., 300)
     * @param courseSubject the course subject (e.g., CS)
     * @return an ArrayList of the course information
     */
    public static ArrayList<String> scrapeThis(String courseNumber, String courseSubject) {
        ArrayList<String> courseInformation = new ArrayList<>();
        String url = BASE_URL + URLEncoder.encode(courseSubject, StandardCharsets.UTF_8);

        //NOTE: Should probably check if it's necessary to add any protections to avoid inadvertently DDOSing the url host.
        Document doc;
        try {
            doc = Jsoup.connect(url).timeout(6000).get();
        } catch(IOException e){
            return courseInformation;
        }
        doc.select("br").append("replace");
        Elements body = doc.select("div.sc_sccoursedescs");
        for(Element e : body.select("div.courseblock")){
                if (e.select("p.courseblocktitle").text().contains(courseNumber)) {
                    try {
                        //Course Title
                        courseInformation.add(e.select("p.courseblocktitle").text());
                        //Course Credits
                        courseInformation.add(e.select("p.courseblockcredits").text());
                        //Course Description
                        courseInformation.add(e.select("p.courseblockdesc").text());
                        if (((e.select("div.cb-extras").select("p.courseblockextra").select("span.cbextra-data")).size() == 4)) {
                            for (int i = 0; i < 4; i++) {
                                courseInformation.add(e.select("div.cb-extras").select("p.courseblockextra").select("span.cbextra-data").get(i).text());
                            }
                        } else {
                            //Requisites:
                            courseInformation.add(e.select("div.cb-extras").select("p.courseblockextra").select("span.cbextra-data").get(0).text());
                            //Course Designation:
                            courseInformation.add("None replace");
                            //Repeatable for Credit:
                            courseInformation.add(e.select("div.cb-extras").select("p.courseblockextra").select("span.cbextra-data").get(1).text());
                            //Last Taught:
                            courseInformation.add(e.select("div.cb-extras").select("p.courseblockextra").select("span.cbextra-data").get(2).text());
                        }
                    } catch(IndexOutOfBoundsException oop){
                        return courseInformation;
                    }
                }
        }
        return courseInformation;
    }
}