package searchengine.service.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import searchengine.service.site.SiteService;
import searchengine.service.snippet.SnippetService;

import java.util.*;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchRepository repository;
    private final MorphologyService morphologyService;
    private final LemmaService lemmaService;
    private final SiteService siteService;
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

        Pageable pageable = PageRequest.of(offset == 0 ? 0 : offset / limit, limit);
        log.info("In searchServiceImpl search: query - {}, offset - {}", query, offset);
        return getSearchResponse(query, site, pageable);
    }

    @Override
    public boolean existsBySiteAndUri(String site, String uri) {
        return repository.existsBySiteAndUri(site, uri);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public void deleteAllBySiteAndUri(String site, String uri) {
        repository.deleteAllBySiteAndUri(site, uri);
    }


    private SearchResponse getSearchResponse(String query, String site, Pageable pageable) {
        SearchResponse searchResponse = new SearchResponse();
        boolean exist = site != null ? existsByQueryAndSite(query, site) : existsByQuery(query);

        if (exist) {
            List<Search> data = getSearches(query, site, pageable);
            searchResponse.setData(data.stream().map(mapper::convertToDto).toList());
            searchResponse.setCount(getCount(query, site));

            return searchResponse;
        }

        List<Search> searchData = createSearchList(query, site);

        if (searchData.isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError(String.format("По запросу : \"%s\" найдено 0 результатов", query));
            log.info("In searchServiceImpl search: not found any pages for query - {}", query);
            return searchResponse;
        }

        saveAll(searchData);

        searchResponse.setCount(getCount(query, site));
        searchResponse.setData(getSearches(query, site, pageable).stream().map(mapper::convertToDto).toList());

        return searchResponse;
    }

    private Integer getCount(String query, String site) {
        return site != null ? repository.countByQueryAndSite(query, site) : repository.countByQuery(query);
    }

    private List<Search> createSearchList(String query, String site) {
        List<Lemma> lemmas = getLemmas(query, site);
        List<Index> indexes = getIndexes(lemmas);
        List<Page> pages = indexes.stream().map(Index::getPage).toList();
        if (pages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Search> searchData = getSearchData(lemmas, pages, indexes);
        searchData.forEach(s -> s.setQuery(query));

        return searchData;
    }

    private List<Search> getSearches(String query, String site, Pageable pageable) {
        return site != null ? repository.findAllByQueryAndSite(query, site, pageable).getContent() :
                repository.findAllByQuery(query, pageable).getContent();
    }

    private void saveAll(List<Search> searchData) {
        repository.saveAll(searchData);
    }


    private List<Lemma> getLemmas(String query, String site) {
        List<Lemma> lemmas = new ArrayList<>();
        String[] words = query.split("\\s+");

        for (String world : words) {
            String normalForm = morphologyService.getNormalForm(world);

            if (normalForm.isEmpty()) {
                return new ArrayList<>();
            }

            lemmas.addAll(site != null ? lemmaService.getLemmasByLemmaAndSite(normalForm, siteService.getByUrl(site)) :
                    lemmaService.getLemmasByLemma(normalForm));
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

        List<Index> indexes = new ArrayList<>();

        for (Lemma lemma : lemmas) {
            indexes.addAll(indexService.findAllByLemma(lemma));
        }

        return indexes;
    }

    private boolean existsByQuery(String query) {
        return repository.existsByQuery(query);
    }

    private boolean existsByQueryAndSite(String query, String site) {
        return repository.existsByQueryAndSite(query, site);
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