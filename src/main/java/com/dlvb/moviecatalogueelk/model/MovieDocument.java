package com.dlvb.moviecatalogueelk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;


/**
 * Represents a movie document stored in Elasticsearch.
 * @author Matushkin Anton
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "movie.public.movie")
public class MovieDocument {

    @Id
    private Integer id;

    private String description;

    private String title;

    @JsonProperty("genre_id")
    private Integer genreId;

    @JsonProperty("__deleted")
    private Boolean deleted;

}
