package searchengine.dto.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SearchData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

}
