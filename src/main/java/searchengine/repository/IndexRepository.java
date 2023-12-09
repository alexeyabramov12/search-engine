package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.index.Index;
import searchengine.model.page.Page;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    List<Index> findAllByPage(Page page);

    @Modifying
    @Transactional
    @Query("DELETE FROM Index")
    void deleteAllIndices();
}
