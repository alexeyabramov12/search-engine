package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionToSite;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final ConnectionToSite connectionToSite;
    private final List<ForkJoinPool> forkJoinPools = new ArrayList<>();


    @Override
    public boolean startIndexing() {
        AtomicBoolean isIndexing = getIndexingStatus(new AtomicBoolean(false));

        if (isIndexing.get()) {
            return false;
        }

        CountDownLatch deletionLatch = new CountDownLatch(1);

        new Thread(() -> {
            deletePagesInBatches(200);
            deleteSiteData();
            deletionLatch.countDown();
        }).start();

        try {
            deletionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        Parser.startParsing();

        List<searchengine.config.Site> siteList = sitesList.getSites();
        siteList.forEach(s -> {
            Site site = Site.builder()
                    .name(s.getName())
                    .url(s.getUrl())
                    .status(Status.INDEXING)
                    .statusTime(LocalDateTime.now())
                    .build();
            siteRepository.save(site);
            new Thread(() -> startPoll(site)).start();
            log.info("In IndexingServiceImpl startIndexing: site - {}", site);
        });
        siteList.clear();

        return true;
    }

    @Override
    public boolean stopIndexing() {
        AtomicBoolean isIndexing = getIndexingStatus(new AtomicBoolean(false));

        if (!isIndexing.get()) {
            return false;
        }

        Parser.stopParsing();

        forkJoinPools.forEach(f -> log.info("FJP info - " + f.toString()));
        forkJoinPools.forEach(ForkJoinPool::shutdownNow);
        forkJoinPools.forEach(f -> log.info("FJP info - " + f.toString()));

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                site.setLastError("Индексация остановлена пользователем");
                site.setStatus(Status.FAILED);
                siteRepository.save(site);
                log.info("In IndexingServiceImpl stopIndexing: site - {}", site);
            }
        });

        forkJoinPools.clear();

        return true;
    }

    private AtomicBoolean getIndexingStatus(AtomicBoolean isIndexing) {
        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                isIndexing.set(true);
            }
        });

        return isIndexing;
    }

    private void startPoll(Site site) {
        String url = site.getUrl();
        Parser parser = new Parser(site, url, connectionToSite, siteRepository, pageRepository);

        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        forkJoinPools.add(forkJoinPool);
        forkJoinPool.invoke(parser);

        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        log.info("In IndexingServiceImpl startPool: indexing completed for site - {}", site);
    }


    private void deletePagesInBatches(int batchSize) {
        Pageable pageable = PageRequest.ofSize(batchSize);

        Page<searchengine.model.Page> pagesToDelete = pageRepository.findAll(pageable);
        while (!pagesToDelete.isEmpty()) {
            pageRepository.deleteAll(pagesToDelete.getContent());
            pageable = PageRequest.ofSize(batchSize);
            pagesToDelete = pageRepository.findAll(pageable);
            log.info("In IndexingServiceImpl deletePagesInBatches: ");
        }
    }


    private void deleteSiteData() {
        siteRepository.deleteAll();
        log.info("In IndexingServiceImpl deleteSiteData: ");
    }


}
