package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Response<T> {
    private T object;
    private List<String> errors;
}