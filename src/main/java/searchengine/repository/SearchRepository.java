package searchengine.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.search.Search;


@Repository
public interface SearchRepository extends JpaRepository<Search, Integer> {

    Page<Search> findAllByQuery(String query, Pageable pageable);
    boolean existsByQuery(String query);
    Integer countByQuery(String query);
}
