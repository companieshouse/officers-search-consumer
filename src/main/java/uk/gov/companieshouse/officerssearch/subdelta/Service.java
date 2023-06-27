package uk.gov.companieshouse.officerssearch.subdelta;

import uk.gov.companieshouse.stream.ResourceChangedData;

interface Service {

    void processMessage(ResourceChangedData changedData);
}
