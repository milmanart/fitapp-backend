package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishDictionaryEntryDTO {
    private String term;
    private String dishKey;
    private String lang;
}
