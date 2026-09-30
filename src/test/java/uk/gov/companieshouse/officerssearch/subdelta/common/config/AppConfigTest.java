package uk.gov.companieshouse.officerssearch.subdelta.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

class AppConfigTest {

    private static final String API_KEY = "some-api-key";
    private static final String API_URL = "http://localhost:8080";

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    // ------------------------------------------------------------------
    // jsonMapper
    // ------------------------------------------------------------------

    @Test
    @DisplayName("JsonMapper serialises properties in snake_case")
    void jsonMapperSerialisesPropertiesInSnakeCase() {
        JsonMapper jsonMapper = appConfig.jsonMapper();

        String json = jsonMapper.writeValueAsString(new SamplePojo("value", 1));

        assertThat(json).contains("\"first_name\"")
                .doesNotContain("\"firstName\"");
    }

    @Test
    @DisplayName("JsonMapper omits null-valued properties")
    void jsonMapperExcludesNullProperties() {
        JsonMapper jsonMapper = appConfig.jsonMapper();

        String json = jsonMapper.writeValueAsString(new SamplePojo(null, 2));

        assertThat(json).doesNotContain("first_name")
                .contains("\"count\":2");
    }

    @Test
    @DisplayName("JsonMapper does not fail when deserialising unknown properties")
    void jsonMapperIgnoresUnknownPropertiesOnDeserialise() {
        JsonMapper jsonMapper = appConfig.jsonMapper();
        String json = "{\"first_name\":\"value\",\"count\":3,\"unexpected_field\":\"noise\"}";

        SamplePojo pojo = assertDoesNotThrow(() -> jsonMapper.readValue(json, SamplePojo.class));

        assertThat(pojo.firstName()).isEqualTo("value");
        assertThat(pojo.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("JsonMapper does not fail when serialising a bean with no properties")
    void jsonMapperDoesNotFailOnEmptyBean() {
        JsonMapper jsonMapper = appConfig.jsonMapper();

        String json = assertDoesNotThrow(() -> jsonMapper.writeValueAsString(new EmptyPojo()));

        assertThat(json).isEqualTo("{}");
    }

    // ------------------------------------------------------------------
    // apiClientSupplier
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Supplier produces an InternalApiClient configured with the given base path")
    void apiClientSupplierProducesClientWithConfiguredBasePath() {
        Supplier<InternalApiClient> supplier = appConfig.apiClientSupplier(API_KEY, API_URL);

        InternalApiClient client = supplier.get();

        assertNotNull(client);
        assertThat(client.getBasePath()).isEqualTo(API_URL);
    }

    @Test
    @DisplayName("Supplier produces a client backed by an ApiKeyHttpClient using the given key")
    void apiClientSupplierProducesClientBackedByApiKeyHttpClient() {
        Supplier<InternalApiClient> supplier = appConfig.apiClientSupplier(API_KEY, API_URL);

        InternalApiClient client = supplier.get();

        Object httpClient = ReflectionTestUtils.getField(client, "httpClient");
        assertThat(httpClient).isInstanceOf(ApiKeyHttpClient.class);

        Object apiKey = ReflectionTestUtils.getField(httpClient, "apiKey");
        assertThat(apiKey).isEqualTo(API_KEY);
    }

    @Test
    @DisplayName("Supplier produces a new InternalApiClient instance on every call")
    void apiClientSupplierProducesNewInstanceEachCall() {
        Supplier<InternalApiClient> supplier = appConfig.apiClientSupplier(API_KEY, API_URL);

        InternalApiClient first = supplier.get();
        InternalApiClient second = supplier.get();

        assertThat(first).isNotSameAs(second);
    }

    // ------------------------------------------------------------------
    // Test fixtures
    // ------------------------------------------------------------------

    private record SamplePojo(String firstName, int count) {
    }

    private static class EmptyPojo {
    }
}