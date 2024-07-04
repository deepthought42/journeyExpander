package com.looksee.journeyExpander.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.enums.JourneyStatus;
import com.looksee.journeyExpander.models.journeys.Journey;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.repository.JourneyRepository;

import lombok.Synchronized;

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
	
	/**
	 * 
	 * @param journey
	 * @return
	 */
	@Retryable
	@Synchronized
	public Journey save(long domain_map_id, Journey journey) {
		Journey journey_record = journey_repo.findByKeyOrCandidateKey(domain_map_id, journey.getKey(), journey.getCandidateKey());
		if(journey_record == null) {
			//journey_record = journey_repo.save(journey);
			journey_record = new Journey();
			journey_record.setOrderedIds(journey.getOrderedIds());
			journey_record.setStatus(journey.getStatus());
			journey_record.setKey(journey.generateKey());
			journey_record.setCandidateKey(journey.generateCandidateKey());
			journey_record = journey_repo.save(journey_record);
			journey_record.setSteps(journey.getSteps());

			for(Step step: journey.getSteps()){
				addStep(journey_record.getId(), step.getId());
			}
		}
		
		return journey_record;
	}
	
	public Journey updateFields(long journey_id, JourneyStatus status, String key, List<Long> ordered_ids) {
		return journey_repo.updateFields(journey_id, status, key, ordered_ids);
	}

	public Journey addStep(long journey_id, long step_id) {
		return journey_repo.addStep(journey_id, step_id);
	}

	public Journey findByCandidateKey(long domain_map_id, String candidate_key) {
		return journey_repo.findByCandidateKey(domain_map_id, candidate_key);
	}
	
}
