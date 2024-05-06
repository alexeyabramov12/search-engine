package searchengine.model.search;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Search {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "query", nullable = false)
    private String query;
    @Column(name = "site", nullable = false)
    private String site;
    @Column(name = "site_name", nullable = false)
    private String siteName;
    @Column(name = "url", nullable = false)
    private String uri;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "snippet", columnDefinition = "TEXT", nullable = false)
    private String snippet;
    @Column(name = "relevance", nullable = false)
    private float relevance;
}
