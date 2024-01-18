package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.mapper.SearchMapper;
import searchengine.model.index.Index;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;
import searchengine.model.search.Search;
import searchengine.model.site.Site;
import searchengine.repository.SearchRepository;
import searchengine.service.index.IndexService;
import searchengine.service.lemma.LemmaService;
import searchengine.service.morphology.MorphologyService;
import searchengine.service.snippet.SnippetService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchRepository searchRepository;
    private final MorphologyService morphologyService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SnippetService snippetService;
    private final SearchMapper mapper;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if (query.isEmpty()) {
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
            log.info("In searchServiceImpl search: empty query");
            return searchResponse;
        }
        Pageable pageable = PageRequest.of(offset, limit);
        log.info("In searchServiceImpl search: query - {}", query);
        return getSearchResponse(query, site, pageable);
    }

    private boolean existsByQuery(String query) {
        return searchRepository.existsByQuery(query);
    }

    private SearchResponse getSearchResponse(String query, String site, Pageable pageable) {
        SearchResponse searchResponse = new SearchResponse();

        if (existsByQuery(query)) {
            searchResponse.setResult(true);
            searchResponse.setCount(searchRepository.countByQuery(query));
            List<Search> data = getSearches(query, site, pageable);
            searchResponse.setData(data.stream().map(mapper::convertToDto).toList());
            return searchResponse;
        }

        List<Search> searchData = createSearch(query, site);
        if (searchData.isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("Указанная страница не найдена");
            log.info("In searchServiceImpl search: not found any pages for query - {}", query);
            return searchResponse;
        }

        saveAll(searchData);

        searchResponse.setCount(searchRepository.countByQuery(query));
        searchResponse.setResult(true);
        searchResponse.setData(getSearches(query, site, pageable).stream().map(mapper::convertToDto).toList());

        return searchResponse;
    }

    private List<Search> createSearch(String query, String site) {
        List<Lemma> lemmas = getLemmas(query);
        List<Index> indexes = getIndexes(lemmas);
        List<Page> pages = site == null ?
                indexes.stream().map(Index::getPage).toList() :
                indexes.stream().map(Index::getPage).filter(p -> p.getSite().getUrl().equals(site)).toList();
        if (pages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Search> searchData = getSearchData(lemmas, pages, indexes);
        searchData.forEach(s -> s.setQuery(query));

        return searchData;
    }

    private List<Search> getSearches(String query, String site, Pageable pageable) {
        return searchRepository.findAllByQuery(query, pageable);
    }

    private void saveAll(List<Search> searchData) {
        searchRepository.saveAll(searchData);
    }

    private List<Lemma> getLemmas(String query) {
        List<Lemma> lemmas = new ArrayList<>();
        String[] words = query.split("\\s+");

        for (String world : words) {
            String normalForm = morphologyService.getNormalForm(world);
            if (normalForm.isEmpty()) {
                return new ArrayList<>();
            }
            Lemma lemma = lemmaService.getLemmaByLemma(normalForm);
            if (lemma == null) {
                return new ArrayList<>();
            }
            lemmas.add(lemma);
        }


        return lemmas.stream()
                .filter(l -> l.getFrequency() < getAverageFrequency(lemmas) * 2)
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();
    }

    private List<Index> getIndexes(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Page> pages = new HashSet<>();
        List<Index> indexes = new ArrayList<>();
        boolean isFirstIteration = true;

        for (Lemma lemma : lemmas) {
            List<Index> localIndexes = indexService.findAllByLemma(lemma);
            indexes.addAll(localIndexes);
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

        return pages.stream()
                .flatMap(page -> indexes.stream().filter(i -> i.getPage().equals(page)))
                .toList();
    }


    private int getAverageFrequency(List<Lemma> lemmas) {
        int sumFrequency = 0;

        for (Lemma lemma : lemmas) {
            sumFrequency += lemma.getFrequency();
        }

        return sumFrequency / lemmas.size();
    }

    private List<Search> getSearchData(List<Lemma> lemmas, List<Page> pages, List<Index> indexes) {
        List<Search> searchDataList = new ArrayList<>();
        float maxRelevance = getMaxRelevance(pages, indexes);

        pages.forEach(page -> {
            Site site = page.getSite();

            Search search = Search.builder()
                    .uri(page.getPath().equals("/") ? "" : page.getPath())
                    .site(site.getUrl())
                    .siteName(site.getName())
                    .title(getTitle(page.getContent()))
                    .snippet(snippetService.getSnippet(page, lemmas))
                    .relevance(getAbsolutRelevance(page, indexes) / maxRelevance)
                    .build();

            searchDataList.add(search);
        });

        searchDataList.sort(Comparator.comparing(Search::getRelevance).reversed());
        return searchDataList;
    }

    private String getTitle(String content) {
        return Jsoup.parse(content).title();
    }

    private float getAbsolutRelevance(Page page, List<Index> indexes) {
        return indexes.stream()
                .filter(index -> index.getPage().equals(page))
                .mapToInt(Index::getRank)
                .sum();
    }


    private float getMaxRelevance(List<Page> pages, List<Index> indexes) {
        float maxRelevance = 0;

        for (Page page : pages) {
            List<Index> localIndexes = indexes.stream()
                    .filter(i -> i.getPage().equals(page))
                    .toList();

            float absolutRelevance = getAbsolutRelevance(page, localIndexes);
            maxRelevance = Math.max(absolutRelevance, maxRelevance);
        }

        return maxRelevance;
    }
}