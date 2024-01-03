package searchengine.service.index;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.repository.IndexRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    @Override
    public Index add(Index index) {
        return indexRepository.save(index);
    }

    @Override
    public void addAll(Iterable<Index> indices) {
        indexRepository.saveAll(indices);
    }

    @Override
    public void deleteAllIndices() {
        indexRepository.deleteAllIndices();
    }

    @Override
    public void deleteAllByEntities(List<Index> indices) {
        indexRepository.deleteAll();
    }

    @Override
    public List<Index> findAllByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

    public List<Index> findAllByLemma(Lemma lemma) {
        return indexRepository.findAllByLemma(lemma);
    }
}
