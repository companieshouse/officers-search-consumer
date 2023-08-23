package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.request.QueryParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QueryParamBuilder {

    public static final Map<String, String> INTERNAL_GET_PARAMS = Map.of("items_per_page", "500");

    public List<QueryParam> build(Map<String, String> queryMap) {
        List<QueryParam> queryParams = new ArrayList<>();
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            queryParams.add(new QueryParam(entry.getKey(), entry.getValue()));
        }
        return queryParams;
    }
}
