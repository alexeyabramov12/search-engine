package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.page.Page;
import searchengine.model.site.Site;

import javax.transaction.Transactional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    Page findByPathAndSite(String path, Site site);
    Boolean existsByPathAndSite(String path, Site site);

    Long countBySite(Site site);

    @Modifying
    @Transactional
    @Query("DELETE FROM Page ")
    void deleteAllPages();

    @Transactional
    void deleteByPathAndSite(String path, Site site);

}
