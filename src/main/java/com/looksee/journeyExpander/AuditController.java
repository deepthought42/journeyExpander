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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.journeyExpander.models.enums.Action;
import com.looksee.browsing.ActionFactory;
import com.looksee.journeyExpander.gcp.PubSubAuditRecordPublisherImpl;
import com.looksee.journeyExpander.gcp.PubSubErrorPublisherImpl;
import com.looksee.journeyExpander.mapper.Body;
import com.looksee.journeyExpander.models.Browser;
import com.looksee.journeyExpander.models.BrowserConnectionHelper;
import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;
import com.looksee.journeyExpander.models.enums.BrowserEnvironment;
import com.looksee.journeyExpander.models.enums.BrowserType;
import com.looksee.journeyExpander.models.journeys.Journey;
import com.looksee.journeyExpander.models.journeys.SimpleStep;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.message.VerifiedJourneyMessage;
import com.looksee.journeyExpander.services.BrowserService;
import com.looksee.journeyExpander.services.ElementStateService;
import com.looksee.journeyExpander.services.PageStateService;
import com.looksee.journeyExpander.services.StepExecutor;
import com.looksee.journeyExpander.services.StepService;

import us.codecraft.xsoup.Xsoup;


// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubAuditRecordPublisherImpl pubSubPageAuditPublisherImpl;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private StepExecutor step_executor;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException 
	{
		VerifiedJourneyMessage journey_msg = (VerifiedJourneyMessage) body.getMessage();
	    log.warn("message " + journey_msg);
	    
	    Journey journey = journey_msg.getJourney();
	    /*
	    String data = message.getData();
	    log.warn("data :: "+data);
	    
	    //create ObjectMapper instance
	    ObjectMapper objectMapper = new ObjectMapper();
	    byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
	    String decoded_json = new String(decodedBytes);
	    
	    //convert json string to object
	    Journey journey = objectMapper.readValue(decoded_json, Journey.class);
	     */
	    
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
				browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
				ActionFactory action_factory = new ActionFactory(browser.getDriver());

				//log.warn("journey :: "+journey);
				log.warn("browser :: "+browser);
				executeJourney(journey, browser);
				String current_url = browser.getDriver().getCurrentUrl();
				log.warn("CURRENT URL   ::    "+current_url);
				//construct page and add page to list of page states
				URL page_url = new URL(current_url);							

				//build page state for baseline
				PageState journey_result_page = browser_service.buildPageState(page_url);
				journey_result_page = page_state_service.save(journey_result_page);
				//domain_service.addPage(domain.getId(), journey_result_page.getKey());

				Document doc = Jsoup.parse(journey_result_page.getSrc());
				
				//get all leaf elements 
				List<ElementState> leaf_elements = page_state_service.getVisibleLeafElements(journey_result_page.getKey());
				
				for(ElementState leaf_element : leaf_elements) {
					
					//check each leaf element for mouseover interaction
					WebElement web_element = browser.getDriver().findElement(By.xpath(leaf_element.getXpath()));
					action_factory.execAction(web_element, "", Action.MOUSE_OVER);

					Element element = Xsoup.compile(leaf_element.getXpath()).evaluate(doc).getElements().get(0);
					String css_selector = "";//generateXpathUsingJsoup(element, doc, attributes, xpath_cnt);

					ElementState new_element_state = BrowserService.buildElementState(
																		leaf_element.getXpath(), 
																		browser.extractAttributes(web_element), 
																		element,
																		web_element, 
																		leaf_element.getClassification(), 
																		Browser.loadCssProperties(web_element, 
																		browser.getDriver()),
																		"",
																		css_selector);
					
					new_element_state = element_state_service.save(new_element_state);
					//if page url is not the same as journey result page url then load new page for this
					//construct page and add page to list of page states
					
					PageState exploration_result_page = browser_service.buildPageState(browser);
					log.warn("Page state built in journey explorer");

					log.warn("journey result page key :: "+journey_result_page.getKey());
					log.warn("exploration result page ::  "+exploration_result_page.getKey());
					log.warn("journey result matches exploration result?   " + journey_result_page.equals(exploration_result_page));
					//check if page state is same as original page state. If not then add new ElementInteractionStep 
					if(!journey_result_page.equals(exploration_result_page)) {
						exploration_result_page = page_state_service.save(exploration_result_page);
						
						log.warn("creating new element interaction step .... "+new_element_state);
						Step step = new SimpleStep(journey_result_page, 
												 new_element_state, 
												 Action.MOUSE_OVER, 
												 "", 
												 exploration_result_page);
						if(existsInJourney(journey, step)) {
							continue;
						}
						step = step_service.save(step);
						//add element back to service step
						//clone journey and add this step at the end
						List<Step> steps = new ArrayList<>(journey.getSteps());
						steps.add(step);
						List<Long> ordered_ids = new ArrayList<>(journey.getOrderedIds());
						ordered_ids.add(step.getId());
						Journey new_journey = new Journey(steps, ordered_ids);
						
						//add journey to list of elements to explore for click or typing interactions
						//getSender().tell(new_journey, getSelf());
						JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
						String journey_json = mapper.writeValueAsString(new_journey);
						log.warn("audit progress update = "+journey_json);
						//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
					    pubSubPageAuditPublisherImpl.publish(journey_json);

					    interactive_elements.add(leaf_element.getKey());
					}
				}

				log.warn("sending "+hover_interactions.size()+ " hover interactions to Journey Manager +++");
				executed_successfully = true;
				break;
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
				//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
			    pubSubErrorPublisherImpl.publish(journey_json);
			}
			//TimingUtils.pauseThread(15000L);
		}while(!executed_successfully && cnt < 50);
		
	
		return new ResponseEntity("Successfully sent message to audit manager", HttpStatus.OK);
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
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]