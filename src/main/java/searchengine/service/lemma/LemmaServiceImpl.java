package searchengine.service.lemma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.repository.LemmaRepository;
import searchengine.service.indexing.CreateSiteMap;
import searchengine.service.index.IndexService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexService indexService;


    @Override
    public void add(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    @Override
    public void addAll(Map<Lemma, Integer> lemmaIntegerMap, Page page) {
        lemmaIntegerMap.keySet().forEach(l -> {
            Index index = Index
                    .builder()
                    .page(page)
                    .rank(lemmaIntegerMap.get(l))
                    .build();

            if (!existsByLemma(l.getLemma()) && !CreateSiteMap.isStop()) {
                try {
                    lemmaRepository.save(l);
                    index.setLemma(l);
                    indexService.add(index);
                } catch (DataIntegrityViolationException e) {
                    log.error("In LemmaServiceImpl addLemma: Duplicate Lemma: " + l);
                }

            }
            if (existsByLemma(l.getLemma())) {
                Lemma lemma = getLemmaByLemma(l.getLemma());
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
                index.setLemma(lemma);
                indexService.add(index);
            }
        });
    }

    @Override
    public Boolean existsByLemma(String lemma) {
        return lemmaRepository.existsByLemma(lemma);
    }

    @Override
    public Long getCount() {
        return lemmaRepository.count();
    }

    @Override
    public Long getCountBySite(Site site) {
        return lemmaRepository.countBySite(site);
    }

    @Override
    public Lemma getLemmaByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    @Override
    public void delete(Lemma lemma) {
        lemmaRepository.delete(lemma);
    }

    @Override
    public void deleteAllLemmas() {
        lemmaRepository.deleteAllLemmas();
    }
}
