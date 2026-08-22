package com.sourav.interviewprep.coding.runner;

import com.sourav.interviewprep.coding.config.CodeRunnerProperties;
import com.sourav.interviewprep.coding.exception.CodeExecutionException;
import com.sourav.interviewprep.coding.exception.CodeRunnerUnavailableException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class RemoteCodeRunner implements CodeRunner {

    private final CodeRunnerProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RemoteCodeRunner(CodeRunnerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
    }

    @Override
    public CodeExecutionResult execute(CodeExecutionRequest request) {
        URI endpoint = endpoint();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)));
            if (!properties.apiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + properties.apiKey());
            }
            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CodeRunnerUnavailableException(
                        "Code runner returned HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), CodeExecutionResult.class);
        } catch (CodeRunnerUnavailableException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CodeRunnerUnavailableException("Code runner request was interrupted", exception);
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new CodeRunnerUnavailableException("Code runner request timed out", exception);
        } catch (java.io.IOException exception) {
            throw new CodeRunnerUnavailableException("Code runner is unavailable", exception);
        } catch (RuntimeException exception) {
            throw new CodeExecutionException("Code runner returned a malformed result", exception);
        } catch (Exception exception) {
            throw new CodeExecutionException("Code execution request could not be prepared", exception);
        }
    }

    private URI endpoint() {
        URI baseUrl = properties.baseUrl();
        if (baseUrl == null || baseUrl.getScheme() == null
                || !(baseUrl.getScheme().equals("http") || baseUrl.getScheme().equals("https"))) {
            throw new CodeRunnerUnavailableException(
                    "Code runner is not configured; set CODE_RUNNER_BASE_URL");
        }
        String normalized = baseUrl.toString().endsWith("/")
                ? baseUrl.toString() : baseUrl + "/";
        return URI.create(normalized).resolve("v1/execute");
    }
}
