package searchengine.service.lemma;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;


@Service
public interface LemmaService {

    void add(Lemma lemma);

    void addAll(Document doc, Site site, Page page);

    Boolean existsByLemma(String lemma);

    Long getCount();

    Long getCountBySite(Site site);

    Lemma getLemmaByLemma(String normalForms);

    void delete(Lemma lemma);

    void deleteAllLemmas();
}
