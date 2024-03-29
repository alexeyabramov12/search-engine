package searchengine.service.index;

import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;

import java.util.List;

@Service
public interface IndexService {

    Index add(Index index);

    void addAll(Iterable<Index> indices);

    void deleteAllIndices();

    void deleteAllByEntities(List<Index> indices);

    List<Index> findAllByPage(Page page);

    List<Index> findAllByLemma(Lemma lemma);
}
