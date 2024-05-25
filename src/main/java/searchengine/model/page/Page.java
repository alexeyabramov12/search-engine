package searchengine.model.page;

import lombok.*;
import searchengine.model.site.Site;

import javax.persistence.*;
import javax.persistence.Index;

@Getter
@Setter
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "page",
        indexes = {
                @Index(columnList = "path", name = "path_index")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"path", "site_id"})
        })
public class Page {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
}
