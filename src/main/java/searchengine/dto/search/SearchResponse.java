package searchengine.dto.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponse {

    private boolean result;
    private String error;
    private Integer count;
    private List<SearchData> data;
}
