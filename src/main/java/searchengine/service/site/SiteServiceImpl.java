package searchengine.service.site;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.site.Site;
import searchengine.repository.SiteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    @Override
    public Site add(Site site) {
        return siteRepository.save(site);
    }

    @Override
    public List<Site> getAll() {
        return siteRepository.findAll();
    }

    @Override
    public Long getCount() {
        return siteRepository.count();
    }

    @Override
    public void deleteAll() {
        siteRepository.deleteAll();
    }
}
