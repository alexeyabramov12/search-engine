package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectionToSite;
import searchengine.config.SiteConfig;
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


@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final ConnectionToSite connectionToSite;
    private final List<ForkJoinPool> forkJoinPools = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();


    @Override
    public boolean startIndexing() {

        if (!forkJoinPools.isEmpty()) {
            return false;
        }

        CountDownLatch deletionLatch = new CountDownLatch(1);

        new Thread(() -> {
            deleteData(200);
             deletionLatch.countDown();
        }).start();

        try {
            deletionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }


        List<SiteConfig> siteConfigList = sitesList.getSites();
        siteConfigList.forEach(s -> {
            Site site = Site.builder()
                    .name(s.getName())
                    .url(s.getUrl())
                    .status(Status.INDEXING)
                    .statusTime(LocalDateTime.now())
                    .build();
            siteRepository.save(site);
            Thread thread = new Thread(() -> startPoll(site));
            threads.add(thread);
            thread.start();
            log.info("In IndexingServiceImpl startIndexing: site - {}", site);
        });

        return true;
    }

    @Override
    public boolean stopIndexing() {

        if (forkJoinPools.isEmpty()) {
            return false;
        }


        forkJoinPools.forEach(ForkJoinPool::shutdownNow);
        threads.forEach(Thread::interrupt);

        siteRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                site.setLastError("Индексация остановлена пользователем");
                site.setStatus(Status.FAILED);
                siteRepository.save(site);
                log.info("In IndexingServiceImpl stopIndexing: site - {}", site);
            }
        });

        forkJoinPools.clear();
        threads.clear();

        return true;
    }

    private void startPoll(Site site) {
        Parser parser = new Parser(site, site.getUrl(), siteRepository, pageRepository, connectionToSite);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPools.add(forkJoinPool);
        forkJoinPool.invoke(parser);

        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        log.info("In IndexingServiceImpl startPool: indexing completed for site - {}", site);
    }


    private void deleteData(int batchSize) {
        Pageable pageable = PageRequest.ofSize(batchSize);

        Page<searchengine.model.Page> pagesToDelete = pageRepository.findAll(pageable);
        while (!pagesToDelete.isEmpty()) {
            pageRepository.deleteAll(pagesToDelete.getContent());
            pageable = PageRequest.ofSize(batchSize);
            pagesToDelete = pageRepository.findAll(pageable);
            log.info("In IndexingServiceImpl deleteData: deletePagesInBatches");
        }
        siteRepository.deleteAll();
        log.info("In IndexingServiceImpl deleteData: deleteSiteData");
    }

}
