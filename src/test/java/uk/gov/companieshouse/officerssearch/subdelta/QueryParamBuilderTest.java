package uk.gov.companieshouse.officerssearch.subdelta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.request.QueryParam;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.companieshouse.officerssearch.subdelta.TestUtils.TEST_INTERNAL_GET_PARAMS;

@ExtendWith(MockitoExtension.class)
class QueryParamBuilderTest {

    private final QueryParamBuilder queryParamBuilder = new QueryParamBuilder();

    @Test
    void successfullyBuildQueryParams() {
        // given
        List<QueryParam> expected = List.of(new QueryParam("items_per_page", "500"));

        // when
        List<QueryParam> actual = queryParamBuilder.build(TEST_INTERNAL_GET_PARAMS);

        // then
        assertEquals(expected.get(0).getKey(), actual.get(0).getKey());
        assertEquals(expected.get(0).getValue(), actual.get(0).getValue());
    }
}
