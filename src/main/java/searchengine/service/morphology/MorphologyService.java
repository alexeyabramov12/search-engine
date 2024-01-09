package searchengine.service.morphology;

import org.springframework.stereotype.Service;

@Service
public interface MorphologyService {

    String getNormalForm(String word);

}
