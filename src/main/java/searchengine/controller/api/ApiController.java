package searchengine.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.service.indexing.IndexingService;
import searchengine.service.search.SearchService;
import searchengine.service.statistics.StatisticsService;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
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
    public ResponseEntity<IndexingResponse> addOrUpdatePage(@RequestParam(value = "url") String url) {
        log.info("In ApiController addOrUpdatePage: data with url - {} added or updated", url);
        return ResponseEntity.ok(indexingService.addOrUpdatePage(url.trim()));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(value = "query") String query,
                                                 @RequestParam(value = "site", required = false) String site,
                                                 @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                                 @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("In ApiController search: query - {}", query);
        return ResponseEntity.ok(searchService.search(query.trim(), site, offset, limit));
    }

}
