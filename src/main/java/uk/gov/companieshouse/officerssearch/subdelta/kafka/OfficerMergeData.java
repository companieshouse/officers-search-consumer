package uk.gov.companieshouse.officerssearch.subdelta.kafka;

public record OfficerMergeData(String officerId, String previousOfficerId, String contextId) {

}
