package searchengine.service.page;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.repository.PageRepository;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;


    @Override
    public Page getPageByPath(String path) {
        return pageRepository.findByPath(path);
    }

    @Override
    public Page add(Page page) {
        return pageRepository.save(page);
    }

    @Override
    public Boolean existsByPath(String path) {
        return pageRepository.existsByPath(path);
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
    public void deleteByPath(String path) {
        pageRepository.deleteByPath(path);
    }
}
