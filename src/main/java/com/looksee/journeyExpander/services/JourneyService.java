package com.looksee.journeyExpander.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.journeys.Journey;
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
		Journey journey_record = journey_repo.findByKey(journey.getKey());
		if(journey_record == null) {
			log.warn("journey record with key not found = "+journey.getKey());
			journey_record = journey_repo.findByCandidateKey(journey.getCandidateKey());
			if(journey_record == null) {
				journey_record = journey_repo.save(journey);
			}
			else {
				journey_record.setKey(journey.getKey());
				journey_record.setOrderedIds(journey.getOrderedIds());
				journey_record.setStatus(journey.getStatus());
				journey_repo.save(journey_record);
			}
		}
		else {
			journey_record.setKey(journey.getKey());
			journey_record.setOrderedIds(journey.getOrderedIds());
			journey_record.setStatus(journey.getStatus());
			journey_repo.save(journey_record);
		}
		
		return journey_record;
	}

	public Journey findByCandidateKey(String candidateKey) {
		return journey_repo.findByCandidateKey(candidateKey);
	}
	
}
