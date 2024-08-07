package searchengine.repository.site;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.site.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Site findByUrl(String url);
}
