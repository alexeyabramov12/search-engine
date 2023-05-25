package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.statistics.StatisticsService;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    public final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() throws JSONException {
        JSONObject response = new JSONObject();
        if (indexingService.startIndexing()) {
            response.put("result", true);
            log.info("In ApiController startIndexing: indexing started");
        } else {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            log.error("In ApiController startIndexing: indexing already started");
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() throws JSONException {
        JSONObject response = new JSONObject();
        if (indexingService.stopIndexing()) {
            response.put("result", true);
            log.info("In ApiController stopIndexing: indexing stopped");
        } else {
            response.put("result", false);
            response.put("error", "Индексация не запущена");
            log.error("In ApiController stopIndexing: indexing not running");
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        log.info("In ApiController statistics");
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


}
