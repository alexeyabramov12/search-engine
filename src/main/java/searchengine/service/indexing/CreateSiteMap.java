package searchengine.service.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionToSite;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.model.site.Status;
import searchengine.service.index.IndexService;
import searchengine.service.lemma.LemmaService;
import searchengine.service.morphology.MorphologyService;
import searchengine.service.page.PageService;
import searchengine.service.site.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@AllArgsConstructor
public class CreateSiteMap extends RecursiveAction {

    private Site site;
    private String link;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final ConnectionToSite connectionToSite;
    private final MorphologyService morphologyService;

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
                if ((!pageService.existsByPathAndSite(path, site) && link.contains(site.getUrl())) && !stop) {
                    addToDatabase(doc, statusCode, path);
                    parse(doc);
                }
            }
        } catch (IOException exception) {
            log.error(exception.toString());
            site.setLastError("Ошибка индексации");
            site.setStatus(Status.FAILED);
            siteService.add(site);
        }
    }

    public boolean createOrUpdatePage(String url) throws IOException {
        Connection.Response response = getResponse(url);
        int statusCode = response.statusCode();
        Document doc = response.parse();

        if (statusCode > 400 || site == null) {
            return false;
        }

        String path = getPath(url);

        if (pageService.existsByPathAndSite(path, site)) {
            deleteDataByPath(path);
        }

        addToDatabase(doc, statusCode, path);

        return true;
    }

    public static void setStop(boolean stop) {
        CreateSiteMap.stop = stop;
    }

    private Connection.Response getResponse(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(connectionToSite.getUser_agent())
                .referrer(connectionToSite.getReferer())
                .ignoreHttpErrors(true)
                .execute();
    }

    private void parse(Document doc) {
        Elements elements = doc.select("a[href]");
        List<CreateSiteMap> subTasks = new ArrayList<>();

        for (Element element : elements) {
            String url = element.attr("abs:href");

            if (stop || url.isEmpty() || !url.contains(site.getUrl()) || url.matches(VALID_URL) || url.contains("#") || url.contains("vk.com")) {
                continue;
            }

            String path = getPath(url);
            if (!pageService.existsByPathAndSite(path, site)) {
                CreateSiteMap task = new CreateSiteMap(site, url, siteService, pageService, lemmaService, indexService, connectionToSite, morphologyService);
                task.fork();
                subTasks.add(task);
            }
        }

        for (CreateSiteMap task : subTasks) {
            task.join();
        }
    }

    private void addToDatabase(Document doc, int statusCode, String path) {
        site.setStatusTime(LocalDateTime.now());
        Page page = Page.builder()
                .site(site)
                .path(path)
                .code(statusCode)
                .content(doc.html())
                .build();

        lemmaService.addAll(doc, site, pageService.add(page));

        log.info("IN CreateSiteMap addToDatabase: add data by path - {}", path);
    }

    private void deleteDataByPath(String path) {
        Page page = pageService.getPageByPathAndSite(path, site);
        List<Index> indices = indexService.findAllByPage(page);
        List<Lemma> lemmas = new ArrayList<>();
        indices.forEach(index -> lemmas.add(index.getLemma()));
        lemmas.forEach(lemma -> {
            if (lemma.getFrequency() == 1) {
                lemmaService.delete(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaService.add(lemma);
            }
        });
        indexService.deleteAllByEntities(indices);
        pageService.deleteByPathAndSite(path, site);
        log.info("IN CreateSiteMap deleteDataByPath: delete data by path - {}", path);
    }

    private String getPath(String url) {
        String path = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length());
        return path.isEmpty() ? "/" : path;
    }
}