package uk.gov.companieshouse.officerssearch.subdelta.resourcechanged.service;

import uk.gov.companieshouse.stream.ResourceChangedData;

public interface ResourceChangedService {

    void processMessage(ResourceChangedData changedData);
}
