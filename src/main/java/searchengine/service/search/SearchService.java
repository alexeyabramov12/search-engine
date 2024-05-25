package searchengine.service.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;

@Service
public interface SearchService {

    SearchResponse search(String query, String site, Integer offset, Integer limit);

    boolean existsBySiteAndUri(String site, String uri);

    void deleteAll();

    void deleteAllBySiteAndUri(String site, String uri);

}
