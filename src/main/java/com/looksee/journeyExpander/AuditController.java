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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.openqa.selenium.remote.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage.Body;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.storage.Acl.Domain;
import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
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
	private static Logger log = LoggerFactory.getLogger(AuditController.class);
	
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

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
	    
	    ObjectMapper input_mapper = new ObjectMapper();
	    VerifiedJourneyMessage journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);

	    Journey journey = journey_msg.getJourney();
	    
	    //log.warn("JOURNEY EXPANSION MANAGER received new JOURNEY for mapping :  "+journey.getId());
		List<String> interactive_elements = new ArrayList<>();
		List<Step> journey_steps = journey.getSteps();

		try {
			//get last step
			Step last_step = journey_steps.get(journey_steps.size()-1);
			PageState journey_result_page = null;
			
			if(last_step instanceof LandingStep) {
				//get start page as journey result page
				journey_result_page = journey_steps.get(journey_steps.size()-1).getStartPage();
			}
			else {
				//get end page as journey result page
				journey_result_page = journey_steps.get(journey_steps.size()-1).getEndPage();
			}
			
			//if start page is external then don't expand
			Domain domain = domain_service.findByAuditRecord(journey_msg.getAuditRecordId());
			if(BrowserUtils.isExternalLink(domain.getUrl(), journey_result_page.getUrl())) {
				//create and save journey
				return new ResponseEntity<String>("Last page of journey is external. No further expansion is allowed", HttpStatus.OK); 
			}
			
			//if the page has already been expanded then don't expand the journey for this page
			DomainMap domain_map = domain_map_service.findByDomainAuditId(journey_msg.getAuditRecordId());
			List<Step> page_steps = step_service.getStepsWithStartPage(journey_result_page, domain_map.getId());
			if(page_steps.size() > 1) {
				//log.warn("RETURNING WITHOUT EXPANSION!!!!  Steps were found that start with page with key = "+journey_result_page.getKey());
				return new ResponseEntity<String>("RETURNING WITHOUT EXPANSION!!!!  Steps were found that start with page with key"+journey_result_page.getKey(), HttpStatus.OK);
			}
			
		    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
			
			// EXPAND JOURNEY
			List<Action> actions = new ArrayList<>();
			actions.add(Action.CLICK);
			//actions.add(Action.MOUSE_OVER);
			
			//get all elements then filter for interactive elements
			List<ElementState> leaf_elements = page_state_service.getElementStates(journey_result_page.getId());
			journey_result_page.setElements(leaf_elements);
			log.warn(leaf_elements.size()+" leaf elements found for url = "+journey_result_page.getUrl() + " with id = "+journey_result_page.getId());
			
			//Filter out non interactive elements
			//Filter out elements that are in explored map for PageState with key
			//Filter journey form elements
			leaf_elements = leaf_elements.parallelStream()
											.filter(element -> !BrowserService.isStructureTag(element.getName()))
											/*
											.filter(element -> {
													Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
													return BrowserService.hasWidthAndHeight(dimension);
											})
													 */
											//.filter(element -> element.getXLocation() >= 0 && element.getYLocation() >= 0)
											//.filter(element -> !ElementStateUtils.isFormElement(element))
											.filter(element -> ElementStateUtils.isInteractiveElement(element))
											.collect(Collectors.toList());
			
			
			if(journey_result_page.getUrl().contains("blog")){
				log.warn("blog page was found with "+leaf_elements.size()+" to expand");
				
				for(ElementState element: leaf_elements){
					log.warn("element xpath = "+element.getXpath());
					log.warn("element cssSelector = "+element.getCssSelector());
					log.warn("-------------------------------------------");
				}
			}

			//filter elements that were in previous page
			String current_url = "";
			for(int i=journey_steps.size()-1; i>=0; i--) {
				String start_url = journey_steps.get(i).getStartPage().getUrl();
				if(current_url.contentEquals(start_url)) {
					break;
				}
				else {
					current_url = start_url;
				}
			}
			
			log.warn(leaf_elements.size()+" leaf elements after filtering");

			int journey_cnt = 0;
			for(ElementState leaf_element : leaf_elements) {
				//check if page state is same as original page state. If not then add new ElementInteractionStep 
				
				for(Action action: actions) {
					Step step = new SimpleStep(journey_result_page, 
												 	leaf_element, 
												 	action, 
												 	"",
												 	null,
												 	JourneyStatus.CANDIDATE);
					
					if(existsInJourney(journey, step)) {
						log.warn("IGNRONING JOURNEY! step already exists within journey");
						continue;
					}

					if(!shouldBeExpanded(journey)) {
						log.warn("IGNORING JOURNEY! journey should not be expanded");
						continue;
					}
					
					if(domain_map == null) {
						domain_map = domain_map_service.save(new DomainMap());
						audit_record_service.addDomainMap(journey_msg.getAuditRecordId(), domain_map.getId());
					}
					
					//if step with candidate key exists for domain map then don't expand
					Step step_record = step_service.findByCandidateKey(step.getKey(), domain_map.getId());
					if(step_record != null) {
						log.warn("IGNORING STEP!!  Step with candidate key already exists!!");
						continue;
					}
					
					//add element back to service step
					//clone journey and add this step at the end
					step = step_service.save(step);
					List<Step> steps = new ArrayList<>(journey.getSteps());
					steps.add(step);
					
					//CREATE NEW JOURNEY
					Journey expanded_journey = new Journey(steps, JourneyStatus.CANDIDATE);
					Journey journey_record = journey_service.findByCandidateKey(domain_map.getId(), expanded_journey.getCandidateKey());
					
					if(journey_record != null) {
						continue;
					}

					journey_record = journey_service.save(domain_map.getId(), expanded_journey);
					long journey_id = journey_record.getId();
					journey_record.setSteps(steps);
					steps.stream().map(temp_step -> journey_service.addStep(journey_id, temp_step.getId()));
					//add journey to domain map
					domain_map_service.addJourneyToDomainMap(journey_record.getId(), domain_map.getId());

					//add journey to list of elements to explore for click or typing interactions
					JourneyCandidateMessage candidate = new JourneyCandidateMessage(journey_record, 
																					BrowserType.CHROME,
																					journey_msg.getAccountId(),
																					journey_msg.getAuditRecordId(),
																					domain_map.getId());
					String candidate_json = mapper.writeValueAsString(candidate);
					journey_candidate_topic.publish(candidate_json);
				    interactive_elements.add(leaf_element.getKey());
				    journey_cnt++;
				}
			}

			log.warn("generated "+journey_cnt+" journeys to explore");

			return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
		}
		catch(Exception e) {
			log.warn("Exception occurred while expanding journey ::   "+e.getMessage());
			e.printStackTrace();
			
			log.warn("Error while expanding journey = "+journey.getId());
			//JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
			//String journey_json = mapper.writeValueAsString(journey);
		    //pubSubErrorPublisherImpl.publish(journey_json);
		}
		
		return new ResponseEntity<String>("Error occurred while expanding journey", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	
	/**
	 * if last step in journey is a {@link LandingStep} or a {@link SimpleStep} results in a change of state, 
	 * 	then return true.
	 * 
	 * @param journey
	 * @return
	 */
	private boolean shouldBeExpanded(Journey journey) {
		Step last_step = journey.getSteps().get(journey.getSteps().size()-1);
		if(last_step instanceof LandingStep) {
			return true;
		}
		else if(last_step instanceof SimpleStep) {
			if(!last_step.getStartPage().getKey().contentEquals(last_step.getEndPage().getKey())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if {@link Step} exists within the given {@link Journey}
	 * 
	 * @param journey
	 * @param step
	 * 
	 * @return true if step already exists, otherwise false
	 */
	private boolean existsInJourney(Journey journey, Step step) {
		for(Step journey_step : journey.getSteps()) {
			if(journey_step == null) {
				continue;
			}
			
			if(step instanceof LandingStep && journey_step instanceof LandingStep) {
				if(step.getStartPage().getKey().contentEquals(journey_step.getStartPage().getKey())) {
					return true;
				}
			}
			else if( step instanceof SimpleStep && journey_step instanceof SimpleStep) {
				SimpleStep temp1 = (SimpleStep)step;
				SimpleStep temp2 = (SimpleStep)journey_step;
				if(temp1.getStartPage().getUrl().contentEquals(temp2.getStartPage().getUrl())
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