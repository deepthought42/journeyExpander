package com.looksee.journeyExpander.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.journeys.Journey;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.repository.JourneyRepository;

@Service
public class JourneyService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyService.class.getName());

	@Autowired
	private JourneyRepository journey_repo;
	
	public Optional<Journey> findById(long id) {
		return journey_repo.findById(id);
	}
	
	public Journey findByKey(String key) {
		return journey_repo.findByKey(key);
	}
	
	public Journey save(Journey journey) {
		log.warn("retrieving journey record with key = "+journey.getKey());
		Journey journey_record = new Journey();
		journey_record.setKey(journey.getKey());
		journey_record.setOrderedIds(journey.getOrderedIds());
		journey_record.setStatus(journey.getStatus());
		journey_record = journey_repo.save(journey_record);
		
		for(Step step: journey.getSteps()) {
			journey_repo.addStep(journey.getId(), step.getId());
		}
		
		journey_record.setSteps(journey.getSteps());
		
		return journey_record;
	}

	public Journey findByCandidateKey(long domain_audit_id, String candidateKey) {
		return journey_repo.findByCandidateKey(domain_audit_id, candidateKey);
	}
	
}
