package searchengine.services.statistics;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.StatisticsResponse;

@Service
public interface StatisticsService {
    StatisticsResponse getStatistics();
}
