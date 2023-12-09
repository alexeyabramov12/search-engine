package searchengine.services.morphology;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MorphologyServiceImpl implements MorphologyService {

    private final RussianLuceneMorphology russianLuceneMorphology;
    private final EnglishLuceneMorphology englishLuceneMorphology;

    private static final List<String> WRONG_TEGS = List.of("СОЮЗ", "МЕЖД", "МС", "ПРЕДЛ", "ВВОДН", "ЧАСТ", "CONJ", "PART");
    private static final String RUSSIAN_ALPHABET = "[а-яА-Я]+";
    private static final String ENGLISH_ALPHABET = "[a-zA-z]+";

    @Override
    public Map<Lemma, Integer> getLemmas(Document doc, Site site) throws IOException {
        String[] words = doc.text().split("\\s+");
        Map<Lemma, Integer> lemmasMap = new HashMap<>();
        for (String word : words) {
            String wordLowerCase = word.toLowerCase();
            if (getLanguage(wordLowerCase).isEmpty() || word.length() == 1) {
                continue;
            }
            if (getLanguage(wordLowerCase).equals("RUSSIAN")) {
                addLemmas(wordLowerCase, russianLuceneMorphology, lemmasMap, site);
                continue;
            }
            if (getLanguage(wordLowerCase).equals("ENGLISH")) {
                addLemmas(wordLowerCase, englishLuceneMorphology, lemmasMap, site);
            }
        }

        return lemmasMap;
    }

    public String getLanguage(String word) {
        if (word.matches(RUSSIAN_ALPHABET)) {
            return "RUSSIAN";
        }
        if (word.matches(ENGLISH_ALPHABET)) {
            return "ENGLISH";
        }
        return "";
    }

    private void addLemmas(String word, LuceneMorphology luceneMorphology, Map<Lemma, Integer> lemmasMap, Site site) {
        if (!checkWord(word, luceneMorphology)) {
            return;
        }

        String normalForm = luceneMorphology.getNormalForms(word).toString();
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

    private boolean checkWord(String word, LuceneMorphology luceneMorphology) {
        if (!luceneMorphology.checkString(word)) {
            return false;
        }
        String morphInfo = luceneMorphology.getMorphInfo(word).toString();
        for (String teg : WRONG_TEGS) {
            if (morphInfo.contains(teg)) {
                return false;
            }
        }

        return true;
    }

}
