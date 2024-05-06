package searchengine.service.lemma;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;

import java.util.List;


@Service
public interface LemmaService {

    void add(Lemma lemma);

    void addAll(Document doc, Site site, Page page);

    Boolean existsByLemmaAndSite(String lemma, Site site);

    Long getCount();

    Long getCountBySite(Site site);

    Lemma getLemmaByLemmaAndSite(String normalForms, Site site);

    List<Lemma> getLemmasByLemma(String normalForms);

    void delete(Lemma lemma);

    void deleteAllLemmas();
}
