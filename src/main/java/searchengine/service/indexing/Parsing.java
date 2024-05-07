package searchengine.service.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionToSite;
import searchengine.model.site.Site;
import searchengine.model.site.Status;
import searchengine.service.indexing.sitemap.SiteMapService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Parsing extends RecursiveAction {

    private
    Site site;
    private String link;
    private ConnectionToSite connectionToSite;
    private SiteMapService siteMapService;

    @Getter
    private static volatile boolean stop;
    private static final String VALID_URL = ".*\\.(js|css|jpg|pdf|jpeg|gif|zip|tar|jar|gz|svg|ppt|pptx|php|png)($|\\?.*)";


    @Override
    protected void compute() {
        try {
            Connection.Response response = getResponse(link);
            int statusCode = response.statusCode();
            Document doc = response.parse();

            if (statusCode < 400) {
                String path = getPath(link);
                if ((!siteMapService.exists(path, site) && link.contains(site.getUrl())) && !stop) {
                    siteMapService.createSiteMap(doc, statusCode, path, site);
                    parse(doc);
                }
            }
        } catch (IOException exception) {
            log.error(exception.toString());
            site.setLastError("Ошибка индексации");
            site.setStatus(Status.FAILED);
            siteMapService.updateSite(site);
        }
    }

    public Connection.Response getResponse(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(connectionToSite.getUser_agent())
                .referrer(connectionToSite.getReferer())
                .ignoreHttpErrors(true)
                .execute();
    }

    public static void setStop(boolean stop) {
        Parsing.stop = stop;
    }

    public String getPath(String url) {
        String path = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length());
        return path.isEmpty() ? "/" : path;
    }

    private void parse(Document doc) {
        Elements elements = doc.select("a[href]");
        List<Parsing> subTasks = new ArrayList<>();

        for (Element element : elements) {
            String url = element.attr("abs:href");

            if (stop || url.isEmpty() || !url.contains(site.getUrl()) || url.matches(VALID_URL) || url.contains("#") || url.contains("vk.com")) {
                continue;
            }

            String path = getPath(url);
            if (!siteMapService.exists(path, site)) {
                Parsing task = new Parsing(site, url, connectionToSite, siteMapService);
                task.fork();
                subTasks.add(task);
            }
        }

        for (Parsing task : subTasks) {
            task.join();
        }
    }
}