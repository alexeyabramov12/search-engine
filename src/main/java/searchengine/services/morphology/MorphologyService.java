package searchengine.services.morphology;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;

import java.io.IOException;
import java.util.Map;

@Service
public interface MorphologyService {

    Map<Lemma, Integer> getLemmas(Document doc, Site site) throws IOException;

}
