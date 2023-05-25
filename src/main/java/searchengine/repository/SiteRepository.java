package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Site findByName(String name);

}
