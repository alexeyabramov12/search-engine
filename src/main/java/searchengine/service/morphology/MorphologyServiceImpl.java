package searchengine.service.morphology;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;


import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class MorphologyServiceImpl implements MorphologyService {

    private final RussianLuceneMorphology russianLuceneMorphology;
    private final EnglishLuceneMorphology englishLuceneMorphology;

    private static final List<String> WRONG_TAGS = List.of("СОЮЗ", "МЕЖД", "МС", "ПРЕДЛ", "ВВОДН", "ЧАСТ", "CONJ", "PART");
    private static final String RUSSIAN_ALPHABET = "[а-яА-Я]+";
    private static final String ENGLISH_ALPHABET = "[a-zA-z]+";


    @Override
    public String getNormalForm(String word) {
        String result = "";
        String wordLowerCase = word.toLowerCase();

        if (getLanguage(wordLowerCase).isEmpty() || word.length() == 1) {
            return result;
        }
        if (getLanguage(wordLowerCase).equals("RUSSIAN")) {
            result = !checkWord(wordLowerCase, russianLuceneMorphology) ? ""
                    : russianLuceneMorphology.getNormalForms(wordLowerCase).toString();
        }
        if (getLanguage(wordLowerCase).equals("ENGLISH")) {
            result = !checkWord(wordLowerCase, englishLuceneMorphology) ? ""
                    : englishLuceneMorphology.getNormalForms(wordLowerCase).toString();
        }

        return result;
    }

    private String getLanguage(String word) {
        if (word.matches(RUSSIAN_ALPHABET)) {
            return "RUSSIAN";
        }
        if (word.matches(ENGLISH_ALPHABET)) {
            return "ENGLISH";
        }
        return "";
    }


    private boolean checkWord(String word, LuceneMorphology luceneMorphology) {
        if (!luceneMorphology.checkString(word)) {
            return false;
        }
        String morphInfo = luceneMorphology.getMorphInfo(word).toString();
        for (String teg : WRONG_TAGS) {
            if (morphInfo.contains(teg)) {
                return false;
            }
        }

        return true;
    }

}
