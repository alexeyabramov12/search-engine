package searchengine.service.page;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.repository.page.PageRepository;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;


    @Override
    public Page getPageByPathAndSite(String path, Site site) {
        return pageRepository.findByPathAndSite(path, site);
    }

    @Override
    public Page add(Page page) {
        return pageRepository.save(page);
    }

    @Override
    public Boolean existsByPathAndSite(String path, Site site) {
        return pageRepository.existsByPathAndSite(path, site);
    }

    @Override
    public Long getCountBySite(Site site) {
        return pageRepository.countBySite(site);
    }

    @Override
    public Long getCount() {
        return pageRepository.count();
    }

    @Override
    public void deleteAllPages() {
        pageRepository.deleteAllPages();
    }

    @Override
    public void deleteByPathAndSite(String path, Site site) {
        pageRepository.deleteByPathAndSite(path, site);
    }
}
