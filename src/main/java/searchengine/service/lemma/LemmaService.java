package searchengine.service.lemma;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;

import java.util.List;
import java.util.Map;


@Service
public interface LemmaService {

    Map<Lemma, Integer> getLemmasIntegerMap(Document doc, Site site);

    void add(Lemma lemma);

    Boolean existsByLemmaAndSite(String lemma, Site site);

    Long getCount();

    Long getCountBySite(Site site);

    List<Lemma> getLemmasByLemmaAndSite(String normalForms, Site site);

    List<Lemma> getLemmasByLemma(String normalForms);

    void delete(Lemma lemma);

    void deleteAllLemmas();
}
