package uk.gov.companieshouse.officerssearch.subdelta.search;

import uk.gov.companieshouse.stream.ResourceChangedData;

public interface Service {

    void processMessage(ResourceChangedData changedData);
}
