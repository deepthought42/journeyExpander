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

import com.looksee.journeyExpander.gcp.PubSubErrorPublisherImpl;
import com.looksee.journeyExpander.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.journeyExpander.mapper.Body;
import com.looksee.journeyExpander.models.Browser;
import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;
import com.looksee.journeyExpander.models.journeys.Journey;
import com.looksee.journeyExpander.models.journeys.SimpleStep;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.message.VerifiedJourneyMessage;
import com.looksee.journeyExpander.services.PageStateService;
import com.looksee.journeyExpander.services.StepExecutor;


// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;
	
	@Autowired
	private StepExecutor step_executor;

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
	    ObjectMapper input_mapper = new ObjectMapper();
	    VerifiedJourneyMessage journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);

	    log.warn("message " + journey_msg);
	    Journey journey = journey_msg.getJourney();

	    log.warn("JOURNEY EXPANSION MANAGER received new JOURNEY for mapping");

		List<Journey> hover_interactions = new ArrayList<>();
		List<Journey> click_interactions = new ArrayList<>();
		List<String> interactive_elements = new ArrayList<>();
		
		boolean executed_successfully = false;
		int cnt = 0;
		Browser browser = null;
		do {
			try {
				//start a new browser session
				PageState journey_result_page = journey.getSteps().get(journey.getSteps().size()-1).getEndPage();
				//get all leaf elements 
				List<ElementState> leaf_elements = page_state_service.getVisibleLeafElements(journey_result_page.getKey());
				
				for(ElementState leaf_element : leaf_elements) {
					log.warn("journey result page key :: "+journey_result_page.getKey());
					log.warn("journey result matches exploration result?   " + journey_result_page.equals(null));
					//check if page state is same as original page state. If not then add new ElementInteractionStep 
					
					log.warn("creating new element interaction step .... "+leaf_element);
					Step step = new SimpleStep(journey_result_page, 
											 leaf_element, 
											 Action.MOUSE_OVER, 
											 "", 
											 null);
					
					if(existsInJourney(journey, step)) {
						continue;
					}
					//step = step_service.save(step);
					//add element back to service step
					//clone journey and add this step at the end
					List<Step> steps = new ArrayList<>(journey.getSteps());
					steps.add(step);
					
					List<Long> ordered_ids = new ArrayList<>(journey.getOrderedIds());
					ordered_ids.add(step.getId());
					
					Journey new_journey = new Journey(steps, ordered_ids);
					
					//add journey to list of elements to explore for click or typing interactions
					JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
					String journey_json = mapper.writeValueAsString(new_journey);
					log.warn("audit progress update = "+journey_json);
				    journey_candidate_topic.publish(journey_json);
				    interactive_elements.add(leaf_element.getKey());
				}

				log.warn("sending "+hover_interactions.size()+ " hover interactions to Journey Manager +++");
				executed_successfully = true;
				return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
			}
			catch(Exception e) {
				log.warn("Exception occurred while executing journey ::   "+e.getMessage());
				e.printStackTrace();
				if(browser != null) {
					browser.close();
				}
				
				JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
				String journey_json = mapper.writeValueAsString(journey);
				log.warn("audit progress update = "+journey_json);
			    pubSubErrorPublisherImpl.publish(journey_json);
			}
		}while(!executed_successfully && cnt < 50);
		
		return new ResponseEntity<String>("Error occurred while expanding journey", HttpStatus.INTERNAL_SERVER_ERROR);

	}
	
	private boolean existsInJourney(Journey journey, Step step) {
		for(Step journey_step : journey.getSteps()) {
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