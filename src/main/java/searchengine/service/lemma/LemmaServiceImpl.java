package searchengine.service.lemma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;
import searchengine.repository.LemmaRepository;
import searchengine.service.indexing.Parsing;
import searchengine.service.morphology.MorphologyService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository repository;
    private final MorphologyService morphologyService;


    @Override
    public void add(Lemma lemma) {
        repository.save(lemma);
    }

    @Override
    public Map<Lemma, Integer> getLemmasIntegerMap(Document doc, Site site) {
        Map<Lemma, Integer> lemmaIntegerMap = new HashMap<>();
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

            if (lemmaIntegerMap.containsKey(lemma)) {
                lemmaIntegerMap.put(lemma, lemmaIntegerMap.get(lemma) + 1);
            } else {
                lemmaIntegerMap.put(lemma, 1);
            }
        }
        return addAll(lemmaIntegerMap, site);
    }

    @Override
    public Boolean existsByLemmaAndSite(String lemma, Site site) {
        return repository.existsByLemmaAndSite(lemma, site);
    }

    @Override
    public Long getCount() {
        return repository.count();
    }

    @Override
    public Long getCountBySite(Site site) {
        return repository.countBySite(site);
    }

    @Override
    public List<Lemma> getLemmasByLemmaAndSite(String lemma, Site site) {
        return repository.findAllByLemmaAndSite(lemma, site);
    }

    @Override
    public List<Lemma> getLemmasByLemma(String normalForms) {
        return repository.findAllByLemma(normalForms);
    }

    @Override
    public void delete(Lemma lemma) {
        repository.delete(lemma);
    }

    @Override
    public void deleteAllLemmas() {
        repository.deleteAllLemmas();
    }


    private Map<Lemma, Integer> addAll(Map<Lemma, Integer> lemmaIntegerMap, Site site) {
        Map<Lemma, Integer> result = new HashMap<>();
        Set<Lemma> lemmas = lemmaIntegerMap.keySet();
        for (Lemma l : lemmas) {
            if (Parsing.isStop()) {
                break;
            }

            if (!repository.existsByLemmaAndSite(l.getLemma(), site)) {
                try {
                    repository.save(l);
                    result.put(l, lemmaIntegerMap.get(l));
                    continue;
                } catch (DataIntegrityViolationException e) {
                    log.error("In SiteMapServiceImpl createIndexes: Duplicate Lemma: {}", l);
                }
            }
            Lemma lemma = repository.findByLemmaAndSite(l.getLemma(), site);
            lemma.setFrequency(lemma.getFrequency() + 1);
            repository.save(lemma);
            result.put(lemma, lemmaIntegerMap.get(l));
        }

        return result;
    }
}
