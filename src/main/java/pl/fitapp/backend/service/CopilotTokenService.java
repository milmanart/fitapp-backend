package pl.fitapp.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Slf4j
public class CopilotTokenService {

    private static final String EDITOR_VERSION = "vscode/1.95.0";
    private static final String PLUGIN_VERSION = "copilot/1.243.0";
    private static final String INTEGRATION_ID = "vscode-chat";

    @Value("${github.copilot.github-token:}")
    private String githubToken;

    @Value("${github.copilot.token-url:https://api.github.com/copilot_internal/v2/token}")
    private String tokenUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public CopilotTokenService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return githubToken != null && !githubToken.isBlank();
    }

    public synchronized String getCopilotToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedToken;
        }
        return refreshToken();
    }

    public String getIntegrationId() {
        return INTEGRATION_ID;
    }

    private String refreshToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("Editor-Version", EDITOR_VERSION);
            headers.set("Editor-Plugin-Version", PLUGIN_VERSION);
            headers.set("User-Agent", "GithubCopilot/1.243.0");

            ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String token = root.path("token").asText();
            long expiresAt = root.path("expires_at").asLong(0);
            int refreshIn = root.path("refresh_in").asInt(1500);

            if (token.isBlank()) {
                log.error("Copilot: empty token in response");
                return null;
            }

            cachedToken = token;
            tokenExpiry = expiresAt > 0
                ? Instant.ofEpochSecond(expiresAt)
                : Instant.now().plusSeconds(refreshIn);

            log.info("Copilot: token refreshed, expires at {}", tokenExpiry);
            return cachedToken;

        } catch (Exception e) {
            log.error("Copilot: token refresh failed: {}", e.getMessage());
            return null;
        }
    }
}
