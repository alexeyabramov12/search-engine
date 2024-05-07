package searchengine.service.page;

import org.springframework.stereotype.Service;
import searchengine.model.page.Page;
import searchengine.model.site.Site;


@Service
public interface PageService {

    Page getPageByPathAndSite(String path, Site site);

    Page add(Page page);

    Boolean existsByPathAndSite(String path, Site site);

    Long getCountBySite(Site site);

    Long getCount();

    void deleteAllPages();

    void deleteByPathAndSite(String path, Site site);

}
