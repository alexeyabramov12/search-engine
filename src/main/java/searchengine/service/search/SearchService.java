package searchengine.service.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;

@Service
public interface SearchService {

    SearchResponse search(String query, Integer offset, Integer limit);

}
