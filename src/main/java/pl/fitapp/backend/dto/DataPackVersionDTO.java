package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPackVersionDTO {
    private String packName;
    private int version;
    private LocalDateTime updatedAt;
}
