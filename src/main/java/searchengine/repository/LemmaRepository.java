package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;

import javax.transaction.Transactional;
import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Boolean existsByLemmaAndSite(String lemma, Site site);

    Long countBySite(Site site);

    Lemma findByLemmaAndSite(String lemma, Site site);

    List<Lemma> findAllByLemma(String lemma);

    List<Lemma> findAllByLemmaAndSite(String lemma, Site site);

    @Modifying
    @Transactional
    @Query("DELETE FROM Lemma")
    void deleteAllLemmas();
}
