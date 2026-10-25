package com.rvillalba.hateoas.example;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Spring Data REST expone este repositorio como recurso HATEOAS en {@code /api/examples}:
 * listado paginado, alta, consulta, modificación y borrado, con enlaces HAL en cada respuesta.
 */
@RepositoryRestResource(path = "examples", collectionResourceRel = "examples")
public interface ExampleRepository extends JpaRepository<Example, Long> {

    /** Disponible en {@code /api/examples/search/by-name?name=...}. */
    @RestResource(path = "by-name", rel = "by-name")
    Page<Example> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}
