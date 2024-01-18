package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.service.indexing.IndexingService;
import searchengine.service.search.SearchService;
import searchengine.service.statistics.StatisticsService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        log.info("In ApiController startIndexing:");
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        log.info("In ApiController stopIndexing:");
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        log.info("In ApiController statistics");
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> addOrUpdatePage(String url) {
        log.info("In ApiController addOrUpdatePage: data with url - {} added or updated", url);
        return ResponseEntity.ok(indexingService.addOrUpdatePage(url.trim()));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(String query, String site, Integer offset, Integer limit) {
        log.info("In ApiController search: query - {}", query);
        return ResponseEntity.ok(searchService.search(query.trim(), site, offset, limit));
    }

}
