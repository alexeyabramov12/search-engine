package searchengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.search.Search;

import java.util.List;

@Repository
public interface SearchRepository extends PagingAndSortingRepository<Search, Long> {

    boolean existsByQuery(String query);

    List<Search> findAllByQuery(String query, Pageable pageable);

    Integer countByQuery(String query);
}
