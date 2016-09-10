package scrappers;

import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import loader.ESLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * Created by samujjal on 07/09/16.
 */
public class BookMyShowScrapper {
    static Logger LOGGER = LoggerFactory.getLogger(BookMyShowScrapper.class);
    static String INDEX_NAME = "moviedb";
    static String MOVIE_INDEX_TYPE = "movie";
    static String THEATRE_INDEX_TYPE = "theatre";
    public static void main(String[] args) {
        try {
            LoadToElastic();
        } catch (UnknownHostException e) {
            LOGGER.error("Malfunction:  ", e);
        }
    }

    private static void LoadToElastic() throws UnknownHostException {
        JSONObject json = new JSONObject(grabWebsiteData());
        ESLoader esLoader = new ESLoader();
        esLoader.startup();

        JSONArray movies = json.getJSONObject("moviesData").getJSONObject("BookMyShow").getJSONArray("arrEvents");
        for (int i = 0; i < movies.length(); i++){
            esLoader.insertDocument(movies.get(i).toString(), INDEX_NAME, MOVIE_INDEX_TYPE);
        }

        JSONArray theatres = json.getJSONObject("cinemas").getJSONObject("BookMyShow").getJSONArray("aiVN");
        for (int i = 0; i < theatres.length(); i++){
            esLoader.insertDocument(theatres.get(i).toString(), INDEX_NAME, THEATRE_INDEX_TYPE);
        }
        esLoader.shutdown();
    }

    private static String grabWebsiteData() {
        WebClient client = new WebClient();
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
        } finally {
            client.close();
        }
        return null;
    }
}
