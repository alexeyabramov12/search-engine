package searchengine.dto.search;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

}
