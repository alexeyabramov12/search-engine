package searchengine.services.indexing;

import org.springframework.stereotype.Service;

@Service
public interface IndexingService {

    boolean startIndexing();

    boolean stopIndexing();

}
