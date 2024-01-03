package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.site.Site;
import searchengine.service.index.IndexService;
import searchengine.service.lemma.LemmaService;
import searchengine.service.morphology.MorphologyService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MorphologyService morphologyService;
    private final LemmaService lemmaService;
    private final IndexService indexService;


    @Override
    public SearchResponse search(String query, Integer offset, Integer limit) {
        if (query.isEmpty()) {
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
            log.info("In searchServiceImpl search: empty query");
            return searchResponse;
        }

        log.info("In searchServiceImpl search: query - {}", query);
        return getSearchResponse(query);
    }

    private List<Lemma> getLemmas(String query) {
        List<Lemma> lemmas = new ArrayList<>();
        String[] words = query.split("\\s+");

        for (String world : words) {
            String normalForm = morphologyService.getNormalForm(world);
            if (normalForm.isEmpty()) {
                continue;
            }
            Lemma lemma = lemmaService.getLemmaByLemma(normalForm);
            if (lemma == null) {
                continue;
            }
            lemmas.add(lemma);
        }


        return lemmas
                .stream()
                .filter(l -> l.getFrequency() < getAverageFrequency(lemmas) * 2)
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();
    }


    private List<Page> getPages(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Page> pages = new HashSet<>();
        boolean isFirstIteration = true;

        for (Lemma lemma : lemmas) {
            List<Index> localIndexes = indexService.findAllByLemma(lemma);
            Set<Page> localPages = new HashSet<>();

            for (Index index : localIndexes) {
                localPages.add(index.getPage());
            }

            if (isFirstIteration) {
                pages.addAll(localPages);
                isFirstIteration = false;
            } else {
                pages.retainAll(localPages);
            }

            if (pages.isEmpty()) {
                break;
            }
        }

        return new ArrayList<>(pages);
    }

    private List<Index> getIndexes(List<Lemma> lemmas, List<Page> pages) {
        if (lemmas.isEmpty()) {
            return new ArrayList<>();
        }
        List<Index> indexes = new ArrayList<>();
        for (Lemma lemma : lemmas) {
            indexes.addAll(indexService.findAllByLemma(lemma));
        }

        for (Page page : pages) {

        }
        return indexes;
    }

    private int getAverageFrequency(List<Lemma> lemmas) {
        int sumFrequency = 0;

        for (Lemma lemma : lemmas) {
            sumFrequency += lemma.getFrequency();
        }

        return sumFrequency / lemmas.size();
    }

    private SearchResponse getSearchResponse(String query) {
        SearchResponse searchResponse = new SearchResponse();

        List<Lemma> lemmas = getLemmas(query);
        List<Page> pages = getPages(lemmas);
        List<Index> indexes = getIndexes(lemmas, pages);

        if (pages.isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("Указанная страница не найдена");
            log.info("In searchServiceImpl search: not found any pages for query - {}", query);
            return searchResponse;
        }

        searchResponse.setResult(true);
        searchResponse.setCount(pages.size());
        searchResponse.setData(getSearchData(lemmas, pages, indexes));

        return searchResponse;
    }

    private List<SearchData> getSearchData(List<Lemma> lemmas, List<Page> pages, List<Index> indexes) {
        List<SearchData> searchDataList = new ArrayList<>();

        pages.forEach(page -> {
            Site site = page.getSite();

            SearchData searchData = SearchData
                    .builder()
                    .url(page.getPath())
                    .site(site.getUrl())
                    .siteName(site.getName())
                    .title(getTitle(page.getContent()))
                    .snippet(getSnippet(page, lemmas))
                    .relevance(getRelevance())
                    .build();

            searchDataList.add(searchData);
        });

        return searchDataList;
    }

    private String getTitle(String content) {
        return Jsoup.parse(content).title();
    }

    private String getSnippet(Page page, List<Lemma> lemmas) {
        String text = page.getContent();


        return "";
    }

    private double getRelevance() {
        return 0.0;
    }
}