package searchengine.mapper;

import org.mapstruct.Mapper;
import searchengine.dto.search.SearchDto;
import searchengine.model.search.Search;

@Mapper(componentModel = "spring")
public interface SearchMapper {

    SearchDto convertToDto(Search search);
}
