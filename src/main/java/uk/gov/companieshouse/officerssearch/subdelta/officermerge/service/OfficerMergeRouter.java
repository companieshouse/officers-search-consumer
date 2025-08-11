package uk.gov.companieshouse.officerssearch.subdelta.officermerge.service;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.officermerge.OfficerMerge;

@Component
public class OfficerMergeRouter {

    public void route(Message<OfficerMerge> capture) {
    }
}

