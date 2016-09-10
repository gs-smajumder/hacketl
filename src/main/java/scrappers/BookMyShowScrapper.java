package scrappers;

import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlListItem;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import loader.ESLoader;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by samujjal on 07/09/16.
 */
public class BookMyShowScrapper {
    static Logger LOGGER = LoggerFactory.getLogger(BookMyShowScrapper.class);
    static String INDEX_NAME = "moviedb";
    static String MOVIE_INDEX_TYPE = "movie";
    static String THEATRE_INDEX_TYPE = "theatre";
    static String THEATRESHOW_INDEX_TYPE = "event";
    static WebClient client;
    static ESLoader esLoader;
    public static void main(String[] args) {
        try {
            LoadToElastic();
        } catch (UnknownHostException e) {
            LOGGER.error("Malfunction:  ", e);
        } catch (IOException e) {
            LOGGER.error("IOError:  ", e);
        }
    }

    private static void LoadToElastic() throws IOException {
        startup();
        JSONObject json = new JSONObject(readJSONFile()); //new JSONObject(grabWebsiteData());

        JSONArray movies = json.getJSONObject("moviesData").getJSONObject("BookMyShow").getJSONArray("arrEvents");
        for (int i = 0; i < movies.length(); i++){
            JSONArray movieChildElements = movies.getJSONObject(i).getJSONArray("ChildEvents");
            for (int j = 0; j < movieChildElements.length(); j++) {
                String eventCode = movieChildElements.getJSONObject(j).getString("EventCode");
                movieChildElements.getJSONObject(j).put("imgurl", String.format("http://in.bmscdn.com/events/Large/%s.jpg",eventCode));
                esLoader.insertDocument(movieChildElements.get(j).toString(), INDEX_NAME, MOVIE_INDEX_TYPE);
            }

        }

        JSONArray theatres = json.getJSONObject("cinemas").getJSONObject("BookMyShow").getJSONArray("aiVN");
        for (int i = 0; i < theatres.length(); i++){
            JSONObject theatreShows = new JSONObject();
            theatreShows.put("VenueCode", theatres.getJSONObject(i).getString("VenueCode"));
            theatreShows.put("VenueName", theatres.getJSONObject(i).getString("VenueName"));
            theatreShows.put("ShowTimes", getMoviesShowTimes(theatres.getJSONObject(i).getString("VenueCode"), theatres.getJSONObject(i).getString("VenueName")));
            esLoader.insertDocument(theatreShows.toString(), INDEX_NAME, THEATRESHOW_INDEX_TYPE);
            Map location = new HashMap();
            location.put("lat", theatres.getJSONObject(i).getDouble("VenueLatitude"));
            location.put("lon", theatres.getJSONObject(i).getDouble("VenueLongitude"));
            theatres.getJSONObject(i).put("location", location);
            esLoader.insertDocument(theatres.get(i).toString(), INDEX_NAME, THEATRE_INDEX_TYPE);
        }
        shutdown();
    }

    private static String grabWebsiteData() {
        client.getOptions().setCssEnabled(true);
        client.getOptions().setJavaScriptEnabled(true);
        client.getCookieManager().setCookiesEnabled(true);
        try {
            String searchUrl = "https://in.bookmyshow.com/bengaluru";
            HtmlPage page = client.getPage(searchUrl);
            UnexpectedPage tpage = client.getPage("https://in.bookmyshow.com/serv/getData?cmd=QUICKBOOK&type=MT&getSeenData=1&getRecommendedData=1&_=1473239978807");
//            List<?> objects = page.getByXPath("//*/div[@class='result-text']");
            return tpage.getWebResponse().getContentAsString("UTF-8");
        } catch (Exception e) {
            LOGGER.error("BookMyShow errored on data load", e);
        }
        return null;
    }

    private static JSONObject getMoviesShowTimes(String venueCode, String venueName){
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        Map<String, List<String>> movieShowtimes = new HashMap();
        try {
            String[] tokens = venueName.split(",|:|\\s+|\\.|/|\\(|\\)");
            LocalDate date = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String localdate = date.format(formatter);
            String searchUrl = String.format("https://in.bookmyshow.com/bengaluru/cinemas/%s/%s/%s", String.join("-", tokens), venueCode, localdate);

            HtmlPage page = client.getPage(searchUrl);
            List<?> movies = page.getByXPath("//*/ul[@id='showEvents']/li[@class='list']");

            for (int i = 0; i < movies.size(); i++) {
                List<String> showtimelist =new ArrayList<String>();
                movieShowtimes.put(((HtmlAnchor)(((HtmlListItem)movies.get(i)).getByXPath(".//span[@class='__name']/a")).get(0)).getAttribute("href").split("\\/")[3], showtimelist);
                List<?> showtimes = ((HtmlListItem) movies.get(i)).getByXPath(".//div[@class='_available']");
                for (int j = 0; j < showtimes.size(); j++) {
                    String showtime = ((HtmlAnchor)((HtmlDivision)showtimes.get(j)).getByXPath(".//a").get(0)).getFirstChild().getNodeValue();
                    showtimelist.add(showtime.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.error("BookMyShow errored on data load", e);
        }
        return new JSONObject(movieShowtimes);
    }

    private static void startup() throws UnknownHostException {
        esLoader = new ESLoader();
        esLoader.startup();
        client = new WebClient();
    }

    private static void shutdown(){
        if(client != null){
            client.close();
        }
        if(esLoader != null){
            esLoader.shutdown();
        }
    }

    private static String readJSONFile() throws IOException {
        String file = "/masterdata.json";
        LOGGER.info("Loading file {}", file);
        InputStream inputStream = BookMyShowScrapper.class.getResourceAsStream(file);
        byte[] data = IOUtils.toByteArray(inputStream);
        return new String(data);
    }
}
