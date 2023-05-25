package searchengine.services.indexing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionToSite;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class Parser extends RecursiveAction {

    private final Site site;
    private final String link;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ConnectionToSite connectionToSite;
    private static volatile boolean stop;


    public Parser(Site site, String link, ConnectionToSite connectionToSite, SiteRepository siteRepository, PageRepository pageRepository) {
        this.site = site;
        this.link = link;
        this.connectionToSite = connectionToSite;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(1000);

            Connection.Response response = Jsoup.connect(link)
                    .userAgent(connectionToSite.getUser_agent())
                    .referrer(connectionToSite.getReferer())
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            Document doc = response.parse();
            if (statusCode < 400) {
                String path = getPath(link);
                if (!pageRepository.existsByPath(path) && !stop) {
                    addToDatabase(doc, statusCode, path);
                    parse(doc);
                }
            }

        } catch (IOException | NullPointerException exception) {
            exception.printStackTrace();
            site.setLastError("Ошибка индексации");
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void parse(Document doc) {
        Elements elements = doc.select("a[href]");
        List<Parser> subTasks = new ArrayList<>();
        String validUrl = ".*\\.(js|css|jpg|pdf|jpeg|gif|zip|tar|jar|gz|svg|ppt|pptx|php|png)($|\\?.*)";

        for (Element element : elements) {
            String url = element.attr("abs:href");

            if (stop || url.isEmpty() || !url.contains(site.getUrl()) || url.matches(validUrl) || url.contains("#") || url.contains("vk.com")) {
                continue;
            }

            String path = getPath(url);
            if (!pageRepository.existsByPath(path)) {
                Parser task = new Parser(site, url, connectionToSite, siteRepository, pageRepository);
                task.fork();
                subTasks.add(task);
            }
        }

        for (Parser task : subTasks) {
            task.join();
        }
    }


    private void addToDatabase(Document doc, int statusCode, String path) {
        if (!stop) {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXING);
        }
        Page page = Page.builder()
                .siteId(site)
                .path(path)
                .code(statusCode)
                .content(doc.html())
                .build();
        pageRepository.save(page);
        log.info("IN Parser addToDatabase: path - {}", path);
        siteRepository.save(site);
    }


    private String getPath(String url) {
        String path = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length());
        return path.isEmpty() ? "/" : path;
    }

    public static void stopParsing() {
        stop = true;
    }

    public static void startParsing() {
        stop = false;
    }
}

