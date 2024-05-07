package searchengine.service.lemma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.repository.LemmaRepository;
import searchengine.service.indexing.Parsing;
import searchengine.service.index.IndexService;
import searchengine.service.morphology.MorphologyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexService indexService;
    private final MorphologyService morphologyService;


    @Override
    public void add(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    @Override
    public void addAll(Document doc, Site site, Page page) {
        Map<Lemma, Integer> lemmaIntegerMap = getLemmasIntegerMap(doc, site);
        lemmaIntegerMap.keySet().forEach(l -> {
            Index index = Index
                    .builder()
                    .page(page)
                    .rank(lemmaIntegerMap.get(l))
                    .build();

            if (!existsByLemmaAndSite(l.getLemma(), site) && !Parsing.isStop()) {
                try {
                    lemmaRepository.save(l);
                    index.setLemma(l);
                    indexService.add(index);
                } catch (DataIntegrityViolationException e) {
                    log.error("In LemmaServiceImpl addAll: Duplicate Lemma: {}", l);
                }

            }
            if (existsByLemmaAndSite(l.getLemma(), site)) {
                Lemma lemma = getLemmaByLemmaAndSite(l.getLemma(), site);
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
                index.setLemma(lemma);
                indexService.add(index);
            }
        });
    }

    @Override
    public Boolean existsByLemmaAndSite(String lemma, Site site) {
        return lemmaRepository.existsByLemmaAndSite(lemma, site);
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
    public Lemma getLemmaByLemmaAndSite(String lemma, Site site) {
        return lemmaRepository.findByLemmaAndSite(lemma, site);
    }

    @Override
    public List<Lemma> getLemmasByLemma(String normalForms) {
        return lemmaRepository.findAllByLemma(normalForms);
    }

    @Override
    public void delete(Lemma lemma) {
        lemmaRepository.delete(lemma);
    }

    @Override
    public void deleteAllLemmas() {
        lemmaRepository.deleteAllLemmas();
    }

    private Map<Lemma, Integer> getLemmasIntegerMap(Document doc, Site site) {
        Map<Lemma, Integer> lemmasMap = new HashMap<>();
        String[] words = doc.text().split("\\s+");
        for (String word : words) {
            String normalForm = morphologyService.getNormalForm(word);

            if (normalForm.isEmpty()) {
                continue;
            }

            Lemma lemma = Lemma
                    .builder()
                    .site(site)
                    .lemma(normalForm)
                    .frequency(1)
                    .build();

            if (lemmasMap.containsKey(lemma)) {
                lemmasMap.put(lemma, lemmasMap.get(lemma) + 1);
            } else {
                lemmasMap.put(lemma, 1);
            }
        }
        return lemmasMap;
    }
}
