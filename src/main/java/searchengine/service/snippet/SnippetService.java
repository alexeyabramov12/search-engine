package searchengine.service.snippet;

import org.springframework.stereotype.Service;
import searchengine.model.lemma.Lemma;
import searchengine.model.page.Page;

import java.util.List;

@Service
public interface SnippetService {

    String getSnippet(Page page, List<Lemma> lemmas);
}
