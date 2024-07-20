package searchengine.service.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.repository.index.IndexRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final IndexRepository repository;


    @Override
    public void createIndexes(Map<Lemma, Integer> lemmaIntegerMap, Page page) {
        Set<Lemma> lemmas = lemmaIntegerMap.keySet();
        List<Index> indexes = new ArrayList<>();
        for (Lemma l : lemmas) {
            Index index = Index
                    .builder()
                    .page(page)
                    .rank(lemmaIntegerMap.get(l))
                    .lemma(l)
                    .build();
            indexes.add(index);
        }
        repository.saveAll(indexes);
    }

    @Override
    public void deleteAllIndices() {
        repository.deleteAllIndices();
    }

    @Override
    public void deleteAllByEntities(List<Index> indices) {
        repository.deleteAll();
    }

    @Override
    public List<Index> findAllByPage(Page page) {
        return repository.findAllByPage(page);
    }

    public List<Index> findAllByLemma(Lemma lemma) {
        return repository.findAllByLemma(lemma);
    }
}
