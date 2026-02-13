package pl.fitapp.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class FoodResolveBatchRequest {
    private List<String> foodKeys;
}
