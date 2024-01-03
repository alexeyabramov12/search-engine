package searchengine.service.site;

import org.springframework.stereotype.Service;
import searchengine.model.site.Site;

import java.util.List;

@Service
public interface SiteService {
    Site add(Site site);

    List<Site> getAll();

    Long getCount();

    void deleteAll();

}
