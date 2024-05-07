package searchengine.service.indexing.sitemap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.service.index.IndexService;
import searchengine.service.lemma.LemmaService;
import searchengine.service.page.PageService;
import searchengine.service.site.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteMapServiceImpl implements SiteMapService {

    private final PageService pageService;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final IndexService indexService;


    @Override
    public boolean exists(String path, Site site) {
        return pageService.existsByPathAndSite(path, site);
    }

    @Override
    public void createSiteMap(Document doc, int statusCode, String path, Site site) {
        site.setStatusTime(LocalDateTime.now());
        Page page = Page.builder()
                .site(site)
                .path(path)
                .code(statusCode)
                .content(doc.html())
                .build();


        lemmaService.addAll(doc, site, pageService.add(page));

        log.info("IN CreateSiteMap addToDatabase: add data by path - {}", path);
    }

    @Override
    public void updateSite(Site site) {
        siteService.add(site);
    }

    @Override
    public boolean createOrUpdatePage(Connection.Response response, String path, Site site) throws IOException {
        int statusCode = response.statusCode();
        Document doc = response.parse();

        if (statusCode > 400 || site == null) {
            return false;
        }

        if (pageService.existsByPathAndSite(path, site)) {
            deleteDataByPath(path, site);
        }

        createSiteMap(doc, statusCode, path, site);

        return true;
    }


    private void deleteDataByPath(String path, Site site) {
        Page page = pageService.getPageByPathAndSite(path, site);
        List<Index> indices = indexService.findAllByPage(page);
        List<Lemma> lemmas = new ArrayList<>();
        indices.forEach(index -> lemmas.add(index.getLemma()));
        lemmas.forEach(lemma -> {
            if (lemma.getFrequency() == 1) {
                lemmaService.delete(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaService.add(lemma);
            }
        });
        indexService.deleteAllByEntities(indices);
        pageService.deleteByPathAndSite(path, site);
        log.info("IN CreateSiteMap deleteDataByPath: delete data by path - {}", path);
    }

}
