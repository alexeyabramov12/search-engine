package searchengine.service.page;

import org.springframework.stereotype.Service;
import searchengine.model.page.Page;
import searchengine.model.site.Site;


@Service
public interface PageService {

    Page getPageByPath(String path);

    Page add(Page page);

    Boolean existsByPath(String path);

    Long getCountBySite(Site site);

    Long getCount();

    void deleteAllPages();

    void deleteByPath(String path);

}
