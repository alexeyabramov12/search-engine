package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    private Long statusTime;
    private String error;
    private Long pages;
    private Long lemmas;
}
