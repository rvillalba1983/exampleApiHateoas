package com.rvillalba.hateoas.example;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration(proxyBeanMethods = false)
public class RestConfiguration implements RepositoryRestConfigurer {

    private final Validator validator;

    public RestConfiguration(@Qualifier("defaultValidator") Validator validator) {
        this.validator = validator;
    }

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        // Por defecto Spring Data REST oculta el id (va en el enlace self). Lo mostramos también en el JSON.
        config.exposeIdsFor(Example.class);
        config.setReturnBodyOnCreate(true);
        config.setReturnBodyOnUpdate(true);
    }

    /** Aplica Bean Validation (@NotBlank, @Size...) antes de crear o modificar. Un error devuelve 400. */
    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener listener) {
        listener.addValidator("beforeCreate", validator);
        listener.addValidator("beforeSave", validator);
    }
}
