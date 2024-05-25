package searchengine.service.index;

import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;

import java.util.List;
import java.util.Map;

@Service
public interface IndexService {

    void createIndexes(Map<Lemma, Integer> lemmaIntegerMap, Page page);

    void deleteAllIndices();

    void deleteAllByEntities(List<Index> indices);

    List<Index> findAllByPage(Page page);

    List<Index> findAllByLemma(Lemma lemma);
}
