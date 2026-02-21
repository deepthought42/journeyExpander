package com.looksee.journeyExpander;

/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// [START cloudrun_pubsub_handler]
// [START run_pubsub_handler]

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static final Logger log = LoggerFactory.getLogger(AuditController.class);
	private static final ObjectMapper INPUT_MAPPER = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	private static final JsonMapper OUTPUT_MAPPER = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	
	@Autowired
	private DomainService domain_service;

	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private DomainMapService domain_map_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;

	/**
	 * This method is used to receive a journey from the AuditRecordService and expand it.
	 * 
	 * It will then publish the expanded journey to the PubSubJourneyCandidatePublisherImpl.
	 * 
	 * @param body
	 * @return
	 */
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
		if(body == null || body.getMessage() == null || body.getMessage().getData() == null || body.getMessage().getData().isBlank()) {
			log.warn("IGNORING JOURNEY! request payload missing message data");
			return new ResponseEntity<String>("Message data is required", HttpStatus.BAD_REQUEST);
		}

		VerifiedJourneyMessage journey_msg;
		try {
			String target = new String(Base64.getDecoder().decode(body.getMessage().getData()), StandardCharsets.UTF_8);
			journey_msg = INPUT_MAPPER.readValue(target, VerifiedJourneyMessage.class);
		}
		catch(IllegalArgumentException | JsonProcessingException e) {
			log.warn("IGNORING JOURNEY! failed to parse incoming Pub/Sub payload", e);
			return new ResponseEntity<String>("Invalid message payload", HttpStatus.BAD_REQUEST);
		}

		Journey journey = journey_msg.getJourney();
		if(journey == null || journey.getSteps() == null || journey.getSteps().isEmpty()) {
			log.warn("IGNORING JOURNEY! journey or journey steps missing");
			return new ResponseEntity<String>("Journey has no steps", HttpStatus.BAD_REQUEST);
		}

		if(!shouldBeExpanded(journey)) {
			log.warn("IGNORING JOURNEY! journey should not be expanded");
			return new ResponseEntity<String>("Journey should not be expanded", HttpStatus.OK);
		}

		try {
			List<Step> journey_steps = journey.getSteps();
			Step last_step = journey_steps.get(journey_steps.size()-1);
			PageState journey_result_page = last_step instanceof LandingStep ? last_step.getStartPage() : last_step.getEndPage();
			if(journey_result_page == null) {
				return new ResponseEntity<String>("Journey result page not found", HttpStatus.BAD_REQUEST);
			}
			
			Domain domain = domain_service.findByAuditRecord(journey_msg.getAuditRecordId());
			if(domain == null) {
				log.warn("IGNORING JOURNEY! no domain found for audit record {}", journey_msg.getAuditRecordId());
				return new ResponseEntity<String>("Domain not found for audit record", HttpStatus.BAD_REQUEST);
			}
			if(BrowserUtils.isExternalLink(domain.getUrl(), journey_result_page.getUrl())) {
				return new ResponseEntity<String>("Last page of journey is external. No further expansion is allowed", HttpStatus.OK);
			}
			
			DomainMap domain_map = domain_map_service.findByDomainAuditId(journey_msg.getAuditRecordId());
			if(domain_map != null) {
				List<Step> page_steps = step_service.getStepsWithStartPage(journey_result_page, domain_map.getId());
				if(page_steps != null && page_steps.size() > 1) {
					return new ResponseEntity<String>("RETURNING WITHOUT EXPANSION!!!!  Steps were found that start with page with key"+journey_result_page.getKey(), HttpStatus.OK);
				}
			}
			
			List<Action> actions = List.of(Action.CLICK);
			List<ElementState> page_elements = page_state_service.getElementStates(journey_result_page.getId());
			List<ElementState> leaf_elements = page_elements == null ? new ArrayList<>() : new ArrayList<>(page_elements);
			journey_result_page.setElements(leaf_elements);
			log.warn(leaf_elements.size()+" leaf elements found for url = "+journey_result_page.getUrl() + " with id = "+journey_result_page.getId());
			
			leaf_elements.removeIf(element -> element == null
							|| BrowserService.isStructureTag(element.getName())
							|| !ElementStateUtils.isInteractiveElement(element));

			log.warn(leaf_elements.size()+" leaf elements after filtering");

			int journey_cnt = 0;
			for(ElementState leaf_element : leaf_elements) {
				for(Action action: actions) {
					Step step = new SimpleStep(journey_result_page,
												leaf_element,
											action,
											"",
											null,
											JourneyStatus.CANDIDATE);
					
					if(existsInJourney(journey, step)) {
						log.warn("IGNORING JOURNEY! step already exists within journey");
						continue;
					}
					
					if(domain_map == null) {
						domain_map = domain_map_service.save(new DomainMap());
						audit_record_service.addDomainMap(journey_msg.getAuditRecordId(), domain_map.getId());
					}
					
					Step step_record = step_service.findByCandidateKey(step.getKey(), domain_map.getId());
					if(step_record != null) {
						log.warn("IGNORING STEP!!  Step with candidate key already exists!!");
						continue;
					}
					
					step = step_service.save(step);
					List<Step> steps = new ArrayList<>(journey_steps);
					steps.add(step);
					
					Journey expanded_journey = new Journey(steps, JourneyStatus.CANDIDATE);
					Journey journey_record = journey_service.findByCandidateKey(expanded_journey.getCandidateKey());
					if(journey_record != null) {
						continue;
					}

					journey_record = journey_service.save(domain_map.getId(), expanded_journey);
					long journey_id = journey_record.getId();
					journey_record.setSteps(steps);
					steps.forEach(temp_step -> journey_service.addStep(journey_id, temp_step.getId()));
					domain_map_service.addJourneyToDomainMap(journey_record.getId(), domain_map.getId());

					JourneyCandidateMessage candidate = new JourneyCandidateMessage(journey_record,
																	BrowserType.CHROME,
																	journey_msg.getAccountId(),
																	journey_msg.getAuditRecordId(),
																	domain_map.getId());
					String candidate_json = OUTPUT_MAPPER.writeValueAsString(candidate);
					journey_candidate_topic.publish(candidate_json);
					journey_cnt++;
				}
			}

			log.warn("generated "+journey_cnt+" journeys to explore");
			return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
		}
		catch(Exception e) {
			log.error("Exception occurred while expanding journey {}", journey.getId(), e);
			return new ResponseEntity<String>("Error occurred while expanding journey", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * if last step in journey is a {@link LandingStep} or a {@link SimpleStep} results in a change of state,
	 * then return true.
	 * 
	 * @param journey
	 * @return
	 */
	private boolean shouldBeExpanded(Journey journey) {
		if(journey == null || journey.getSteps() == null || journey.getSteps().isEmpty()) {
			return false;
		}

		Step last_step = journey.getSteps().get(journey.getSteps().size()-1);
		if(last_step == null) {
			return false;
		}

		if(last_step instanceof LandingStep) {
			return last_step.getStartPage() != null;
		}
		else if(last_step instanceof SimpleStep) {
			if(last_step.getStartPage() == null || last_step.getEndPage() == null) {
				return false;
			}
			return !last_step.getStartPage().getKey().contentEquals(last_step.getEndPage().getKey());
		}
		return false;
	}
	
	/**
	 * Checks if {@link Step} exists within the given {@link Journey}
	 * 
	 * @param journey {@link Journey} to check for the given {@link Step}
	 * @param step {@link Step} to check for in the given {@link Journey}
	 * 
	 * @return true if step already exists, otherwise false
	 */
	private boolean existsInJourney(Journey journey, Step step) {
		if(journey == null || journey.getSteps() == null || step == null) {
			return false;
		}
		for(Step journey_step : journey.getSteps()) {
			if(journey_step == null) {
				continue;
			}
			
			if(step instanceof LandingStep && journey_step instanceof LandingStep) {
				if(step.getStartPage() != null
						&& journey_step.getStartPage() != null
						&& step.getStartPage().getKey().contentEquals(journey_step.getStartPage().getKey())) {
					return true;
				}
			}
			else if(step instanceof SimpleStep && journey_step instanceof SimpleStep) {
				SimpleStep temp1 = (SimpleStep)step;
				SimpleStep temp2 = (SimpleStep)journey_step;
				if(temp1.getStartPage() != null
						&& temp2.getStartPage() != null
						&& temp1.getElementState() != null
						&& temp2.getElementState() != null
						&& temp1.getStartPage().getUrl().contentEquals(temp2.getStartPage().getUrl())
						&& temp1.getElementState().getKey().contentEquals(temp2.getElementState().getKey())
						&& temp1.getAction().equals(temp2.getAction())
						&& temp1.getActionInput().contentEquals(temp2.getActionInput())) {
					return true;
				}
			}
		}
		return false;
	}

}
