package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.lemma.Lemma;
import searchengine.model.site.Site;

import javax.transaction.Transactional;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Boolean existsByLemma(String lemma);

    Long countBySite(Site site);

    Lemma findByLemma(String lemma);

    @Modifying
    @Transactional
    @Query("DELETE FROM Lemma")
    void deleteAllLemmas();
}
