package pl.fitapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisionRequest {
    @NotBlank
    private String imageBase64;
    private String mimeType = "image/jpeg";
}
