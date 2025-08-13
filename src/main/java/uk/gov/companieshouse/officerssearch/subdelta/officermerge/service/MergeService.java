package uk.gov.companieshouse.officerssearch.subdelta.officermerge.service;

import org.springframework.messaging.Message;
import uk.gov.companieshouse.officermerge.OfficerMerge;

public interface MergeService {

    void processMessage(Message<OfficerMerge> message);

}
