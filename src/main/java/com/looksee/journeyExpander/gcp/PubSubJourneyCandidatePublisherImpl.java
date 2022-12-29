package com.looksee.journeyExpander.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubJourneyCandidatePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubJourneyCandidatePublisherImpl.class);

    @Value("${pubsub.journey_candidate}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}