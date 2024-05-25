package searchengine.service.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionToSite;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.site.Site;
import searchengine.model.site.Status;
import searchengine.service.index.IndexService;
import searchengine.service.indexing.sitemap.SiteMapService;
import searchengine.service.lemma.LemmaService;
import searchengine.service.page.PageService;
import searchengine.service.search.SearchService;
import searchengine.service.site.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;


@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteMapService siteMapService;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SearchService searchService;
    private final SitesList sitesList;
    private final ConnectionToSite connectionToSite;
    private final List<ForkJoinPool> forkJoinPools = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();


    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();

        if (!forkJoinPools.isEmpty()) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация уже запущена");
            log.error("In IndexingServiceImpl startIndexing: indexing already started");
            return indexingResponse;
        }

        CountDownLatch deletionLatch = new CountDownLatch(1);

        new Thread(() -> {
            deleteData();
            deletionLatch.countDown();
        }).start();

        try {
            deletionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            indexingResponse.setResult(false);
            indexingResponse.setError("ошибка Индексации");
            log.error("In IndexingServiceImpl startIndexing: indexing error");
            return indexingResponse;
        }

        Parsing.setStop(false);

        if (siteService.getAll().isEmpty()) {
            addSites();
        }

        siteService.getAll().forEach(site -> {
            Thread thread = new Thread(() -> startPoll(site));
            threads.add(thread);
            thread.start();
            log.info("In IndexingServiceImpl startIndexing: site - {}", site);
        });

        indexingResponse.setResult(true);
        return indexingResponse;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();


        if (forkJoinPools.isEmpty()) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация не запущена");
            log.error("In IndexingServiceImpl stopIndexing: indexing not running");
            return indexingResponse;
        }

        Parsing.setStop(true);
        forkJoinPools.forEach(ForkJoinPool::shutdown);
        threads.forEach(Thread::interrupt);

        siteService.getAll().forEach(site -> {
            site.setLastError("Индексация остановлена пользователем");
            site.setStatus(Status.FAILED);
            site.setStatusTime(LocalDateTime.now());
            siteService.add(site);
            log.info("In IndexingServiceImpl stopIndexing: site - {}", site);

        });

        forkJoinPools.clear();
        threads.clear();

        indexingResponse.setResult(true);
        return indexingResponse;
    }

    @Override
    public IndexingResponse addOrUpdatePage(String url) {
        IndexingResponse indexingResponse = new IndexingResponse();

        if (siteService.getAll().isEmpty()) {
            addSites();
        }

        Site site = checkUrl(url);

        if (site == null) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            log.error("In IndexingServiceImpl addOrUpdatePage: ");
            return indexingResponse;
        }

        Parsing parsing = new Parsing(site, url, connectionToSite, siteMapService);
        try {
            String path = parsing.getPath(url);
            Connection.Response response = parsing.getResponse(url);
            if (!siteMapService.createOrUpdatePage(response, path, site)) {
                throw new RuntimeException();
            }
        } catch (RuntimeException | IOException e) {
            indexingResponse.setError("Данная страница не существет");
            return indexingResponse;
        }

        indexingResponse.setResult(true);
        return indexingResponse;
    }

    private void addSites() {
        sitesList.getSites().forEach(s -> {
            Site site = Site.builder()
                    .name(s.getName())
                    .url(s.getUrl())
                    .status(Status.INDEXING)
                    .statusTime(LocalDateTime.now())
                    .build();
            siteService.add(site);
        });
    }

    private Site checkUrl(String url) {
        Site site = null;

        if (url.isEmpty()) {
            return null;
        }

        for (Site s : siteService.getAll()) {
            if (url.contains(s.getUrl())) {
                site = s;
            }
        }

        return site;
    }


    private void startPoll(Site site) {
        Parsing parsing = new Parsing(site, site.getUrl(), connectionToSite, siteMapService);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPools.add(forkJoinPool);
        forkJoinPool.invoke(parsing);

        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteService.add(site);
        log.info("In IndexingServiceImpl startPool: indexing completed for site - {}", site);
    }


    private void deleteData() {
        indexService.deleteAllIndices();
        lemmaService.deleteAllLemmas();
        pageService.deleteAllPages();
        siteService.deleteAll();
        searchService.deleteAll();
        log.info("In IndexingServiceImpl deleteData: deleteData");
    }
}
