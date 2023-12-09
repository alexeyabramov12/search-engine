package searchengine.services.lemma;

import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;

import java.util.Map;


@Service
public interface LemmaService {

    void add(Lemma lemma);

    void addAll(Map<Lemma, Integer> lemmaIntegerMap, Page page);

    Boolean existsByLemma(String lemma);

    Long getCount();

    Long getCountBySite(Site site);

    Lemma getLemmaByLemma(String lemma);

    void delete(Lemma lemma);

    void deleteAllLemmas();
}
