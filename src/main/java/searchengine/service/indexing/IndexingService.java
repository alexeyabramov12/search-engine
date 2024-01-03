package searchengine.service.indexing;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;

@Service
public interface IndexingService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse addOrUpdatePage(String url);

}
