package searchengine.service.site;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.site.Site;
import searchengine.repository.SiteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository repository;

    @Override
    public Site add(Site site) {
        return repository.save(site);
    }

    @Override
    public List<Site> getAll() {
        return repository.findAll();
    }

    @Override
    public Site getByUrl(String url) {
        return repository.findByUrl(url);
    }

    @Override
    public Long getCount() {
        return repository.count();
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
