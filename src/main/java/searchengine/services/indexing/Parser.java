package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Parser extends RecursiveAction {

    private Site site;
    private String link;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private ConnectionToSite connectionToSite;

    private static String VALID_URL = ".*\\.(js|css|jpg|pdf|jpeg|gif|zip|tar|jar|gz|svg|ppt|pptx|php|png)($|\\?.*)";
    @Override
    protected void compute() {
        try {
            Connection.Response response = Jsoup.connect(link)
                    .userAgent(connectionToSite.getUser_agent())
                    .referrer(connectionToSite.getReferer())
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            Document doc = response.parse();
            if (statusCode < 400) {
                String path = getPath(link);
                if (!pageRepository.existsByPath(path) || path.equals("/")) {
                    addToDatabase(doc, statusCode, path);
                    parse(doc);
                }
            }

        } catch (Exception exception ) {
            log.error(exception.toString());
            site.setLastError("Ошибка индексации");
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        }
    }

    private void parse(Document doc) {
        Elements elements = doc.select("a[href]");
        List<Parser> subTasks = new ArrayList<>();

        for (Element element : elements) {
            String url = element.attr("abs:href");

            if (url.isEmpty() || !url.contains(site.getUrl()) || url.matches(VALID_URL) || url.contains("#") || url.contains("vk.com")) {
                continue;
            }

            String path = getPath(url);
            if (!pageRepository.existsByPath(path)) {
                Parser task = new Parser(site, url, siteRepository, pageRepository, connectionToSite);
                task.fork();
                subTasks.add(task);
            }
        }

        for (Parser task : subTasks) {
            task.join();
        }
    }


    private void addToDatabase(Document doc, int statusCode, String path) {
        site.setStatusTime(LocalDateTime.now());
        site.setStatus(Status.INDEXING);

        Page page = Page.builder()
                .site(site)
                .path(path)
                .code(statusCode)
                .content(doc.html())
                .build();
        if (pageRepository.existsByPath(path)){
            return;
        }

        pageRepository.save(page);
        log.info("IN Parser addToDatabase: path - {}", path);
        siteRepository.save(site);
    }


    private String getPath(String url) {
        String path = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length());
        return path.isEmpty() ? "/" : path;
    }
}

