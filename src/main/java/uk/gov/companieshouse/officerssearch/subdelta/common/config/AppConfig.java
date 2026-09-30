package uk.gov.companieshouse.officerssearch.subdelta.common.config;

import static uk.gov.companieshouse.officerssearch.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Bean
    JsonMapper jsonMapper() {
        LOGGER.info("jsonMapper() method called.");

        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }

    @Bean
    Supplier<InternalApiClient> apiClientSupplier(
            @Value("${api.api-key}") String apiKey,
            @Value("${api.api-url}") String apiUrl) {
        LOGGER.info("apiClientSupplier() method called.");

        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(apiKey));
            internalApiClient.setBasePath(apiUrl);
            return internalApiClient;
        };
    }
}
