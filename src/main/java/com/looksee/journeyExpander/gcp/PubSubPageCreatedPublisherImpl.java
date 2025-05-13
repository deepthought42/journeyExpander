package com.looksee.journeyExpander.gcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubPageCreatedPublisherImpl extends PubSubPublisher {
    @Value("${pubsub.page_built}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}