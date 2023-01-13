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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.journeyExpander.models.enums.Action;
import com.looksee.journeyExpander.models.enums.BrowserType;
import com.looksee.journeyExpander.gcp.PubSubErrorPublisherImpl;
import com.looksee.journeyExpander.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.journeyExpander.mapper.Body;
import com.looksee.journeyExpander.models.Browser;
import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;
import com.looksee.journeyExpander.models.journeys.Journey;
import com.looksee.journeyExpander.models.journeys.SimpleStep;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.message.JourneyCandidateMessage;
import com.looksee.journeyExpander.models.message.PageBuiltMessage;
import com.looksee.journeyExpander.models.message.VerifiedJourneyMessage;
import com.looksee.journeyExpander.services.ElementStateService;
import com.looksee.journeyExpander.services.JourneyService;
import com.looksee.journeyExpander.services.StepExecutor;
import com.looksee.journeyExpander.services.StepService;
import com.looksee.journeyExpander.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.journeyExpander.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.journeyExpander.models.enums.BrowserType;
import com.looksee.journeyExpander.models.enums.PathStatus;


// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;
	
	@Autowired
	private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
	
	@Autowired
	private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;
	
	@Autowired
	private StepExecutor step_executor;

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
	    
	    log.warn("data value :: "+target);
	    ObjectMapper input_mapper = new ObjectMapper();
	    VerifiedJourneyMessage journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);
	    
	    log.warn("journey order ids" + journey_msg.getJourney().getOrderedIds());
	    log.warn("journey status :: "+journey_msg.getStatus());
	    log.warn("journey browser :: "+journey_msg.getBrowser());
	    
	    Journey journey = journey_msg.getJourney();
	    
	    log.warn("JOURNEY EXPANSION MANAGER received new JOURNEY for mapping :  "+journey);

		List<Journey> hover_interactions = new ArrayList<>();
		//List<Journey> click_interactions = new ArrayList<>();
		List<String> interactive_elements = new ArrayList<>();

		List<Step> journey_steps = journey.getSteps();
		log.warn("journey steps : "+journey_steps);
		try {
			//boolean page_needs_extraction = false;
			//start a new browser session
			PageState journey_result_page = journey_steps.get(journey_steps.size()-1).getEndPage();
			log.warn("journey result page = "+journey_result_page);
			
			if(journey_result_page == null) {
				journey_result_page = journey_steps.get(journey_steps.size()-1).getStartPage();
				log.warn("journey last step start page = "+journey_result_page);
				//page_needs_extraction = true;
			}
			
			//if start and end page match then it is a page load step and can be discarded in favor of a new expanded step
			boolean page_load_step = false;
			if(journey_steps.get(journey_steps.size()-1).getEndPage().equals(journey_steps.get(journey_steps.size()-1).getStartPage())){
				page_load_step = true;
			}
			
			//if the page has already been expanded then don't expand the journey for this page
			
			
		    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

			List<Step> stepsWithPage = step_service.wasPageExpanded(journey_result_page);
			if( !stepsWithPage.isEmpty() ) {
				
				for(Step step : stepsWithPage) {
					//send pageBuiltMessage with pageState and journey verified message
					PageState page_state = step.getEndPage();
					
					log.warn("page state id :: " + page_state.getId());
					
			   		PageBuiltMessage page_built_msg = new PageBuiltMessage(journey_msg.getAccountId(),
			   																journey_msg.getDomainAuditRecordId(),
			   																journey_msg.getDomainId(), 
			   																page_state.getId(),
			   																journey_msg.getDomainAuditRecordId());
					
					String page_built_str = mapper.writeValueAsString(page_built_msg);
				    pubSubPageCreatedPublisherImpl.publish(page_built_str);

					if(journey_msg.getDomainAuditRecordId() >= 0) {
						List<Step> steps = new ArrayList<>();
						steps.add(new Step(page_state, null));
						log.warn("adding steps to journey");
						Journey new_journey = new Journey(steps);
						log.warn("building journey Candidate message");
						new_journey = journey_service.save(new_journey);
						
						VerifiedJourneyMessage verified_journey_msg = new VerifiedJourneyMessage(new_journey, 
																						PathStatus.READY, 
																						BrowserType.CHROME, 
																						journey_msg.getDomainId(), 
																						journey_msg.getAccountId(), 
																						journey_msg.getDomainAuditRecordId());
						log.warn("sending verified journey");
						String journey_msg_str = mapper.writeValueAsString(verified_journey_msg);

						pubSubJourneyVerifiedPublisherImpl.publish(journey_msg_str);
					}
				}
				return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK); 
			}
			
			List<Action> actions = new ArrayList<>();
			actions.add(Action.CLICK);
			actions.add(Action.MOUSE_OVER);
			
			//get all leaf elements 
			log.warn("getting visible leaf elements for page with id = "+journey_result_page.getId());
			List<ElementState> leaf_elements = element_state_service.getVisibleLeafElements(journey_result_page.getId());
			log.warn(leaf_elements.size()+" leaf elements found");

			for(ElementState leaf_element : leaf_elements) {
				log.warn("journey result page key :: "+journey_result_page.getKey());
				//check if page state is same as original page state. If not then add new ElementInteractionStep 
				
				for(Action action: actions) {
					Step step = new SimpleStep(journey_result_page, 
											 	leaf_element, 
											 	action, 
											 	"", 
											 	null);
					
					if(existsInJourney(journey, step)) {
						continue;
					}

					//add element back to service step
					//clone journey and add this step at the end
					List<Step> steps = new ArrayList<>(journey.getSteps());
					if(page_load_step) {
						steps.set(steps.size()-1, step);
					}
					else {
						steps.add(step);
					}
					
					//log.warn("journey ordered ids = "+journey.getOrderedIds());
					//List<Long> ordered_ids = new ArrayList<>(journey.getOrderedIds());
					/*
					if(page_needs_extraction) {
						steps.set(steps.size()-1, step);
						//ordered_ids.set(ordered_ids.size()-1, step.getId());
					}
					else {
						steps.add(step);
						//ordered_ids.add(step.getId());
					}
					*/
					//Journey new_journey = new Journey(steps);
					
					Journey new_journey = new Journey(steps, ordered_ids);
					new_journey = journey_service.save(new_journey);
					
					//add journey to list of elements to explore for click or typing interactions
					JourneyCandidateMessage candidate = new JourneyCandidateMessage(steps, 
																					BrowserType.CHROME,
																					journey_msg.getDomainId(),
																					journey_msg.getAccountId(),
																					journey_msg.getDomainAuditRecordId());
					String candidate_json = mapper.writeValueAsString(candidate);
					log.warn("journey candidate message = "+candidate_json);
				    journey_candidate_topic.publish(candidate_json);
				    interactive_elements.add(leaf_element.getKey());
				}
			}

			log.warn("sending "+hover_interactions.size()+ " hover interactions to Journey Manager +++");
			return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
		}
		catch(Exception e) {
			log.warn("Exception occurred while expanding journey ::   "+e.getMessage());
			e.printStackTrace();
			
			JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
			String journey_json = mapper.writeValueAsString(journey);
			log.warn("audit progress update = "+journey_json);
		    pubSubErrorPublisherImpl.publish(journey_json);
		}
		
		return new ResponseEntity<String>("Error occurred while expanding journey", HttpStatus.INTERNAL_SERVER_ERROR);
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
		log.warn("step = "+step);
		log.warn("journey steps = "+journey.getSteps());
		for(Step journey_step : journey.getSteps()) {
			log.warn("journey step = "+journey_step);
			if(journey_step == null) {
				continue;
			}
			
			if(journey_step.getKey().contentEquals(step.getKey())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param journey
	 * @param browser
	 */
	private void executeJourney(Journey journey, Browser browser) {
		assert journey != null;
		assert browser != null;
		
		List<Step> ordered_steps = new ArrayList<>();
		//execute journey steps
		for(long step_id : journey.getOrderedIds()) {
			
			for(Step step: journey.getSteps()) {
				if(step.getId() == step_id) {
					ordered_steps.add(step);
					break;
				}
			}
		}

		for(Step step : ordered_steps) {
			
			log.warn("step :: "+step);
			//execute step
			step_executor.execute(browser, step);
		}
	}	

}