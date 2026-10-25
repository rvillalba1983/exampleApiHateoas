package com.rvillalba.hateoas.example;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Example {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    protected Example() {
        // requerido por JPA
    }

    public Example(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
