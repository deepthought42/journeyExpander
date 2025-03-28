package com.looksee.journeyExpander.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.PageState;
import com.looksee.journeyExpander.models.journeys.LandingStep;
import com.looksee.journeyExpander.models.journeys.LoginStep;
import com.looksee.journeyExpander.models.journeys.SimpleStep;
import com.looksee.journeyExpander.models.journeys.Step;
import com.looksee.journeyExpander.models.repository.LandingStepRepository;
import com.looksee.journeyExpander.models.repository.LoginStepRepository;
import com.looksee.journeyExpander.models.repository.SimpleStepRepository;
import com.looksee.journeyExpander.models.repository.StepRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enables interacting with database for {@link SimpleStep Steps}
 */
@Service
@Retry(name = "neoforj")
public class StepService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;
	
	@Autowired
	private SimpleStepRepository simple_step_repo;

	@Autowired
	private LoginStepRepository login_step_repo;
	
	@Autowired
	private LandingStepRepository landing_step_repo;
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	/**
	 * Saves a {@link Step} to the database
	 * @Version - 9/19/2023
	 * 
	 * @param step
	 * 
	 * @return saved database record for {@link Step}
	 */
	public Step save(Step step) {
		assert step != null;
		
		if(step instanceof SimpleStep) {
			SimpleStep step_record = simple_step_repo.findByKey(step.getKey());
			SimpleStep simple_step = (SimpleStep)step;
			
			if(step_record != null) {
				step_record.setElementState(simple_step.getElementState());
				step_record.setStartPage(simple_step.getStartPage());
				step_record.setEndPage(simple_step.getEndPage());
				return step_record;
			}
			
			SimpleStep new_simple_step = new SimpleStep();
			new_simple_step.setAction(simple_step.getAction());
			new_simple_step.setActionInput(simple_step.getActionInput());
			new_simple_step.setKey(simple_step.generateKey());
			new_simple_step = simple_step_repo.save(new_simple_step);
			setStartPage(new_simple_step.getId(), simple_step.getStartPage().getId());
			new_simple_step.setStartPage(simple_step.getStartPage());
			
			if(simple_step.getEndPage() != null) {
				addEndPage(new_simple_step.getId(), simple_step.getEndPage().getId());
				new_simple_step.setEndPage(simple_step.getEndPage());
			}
			
			setElementState(new_simple_step.getId(), simple_step.getElementState().getId());
			new_simple_step.setElementState(simple_step.getElementState());
			
			return new_simple_step;
		}
		else if(step instanceof LoginStep) {
			LoginStep step_record = login_step_repo.findByKey(step.getKey());
			LoginStep login_step = (LoginStep)step;
			if(step_record != null) {
				step_record.setTestUser(login_step.getTestUser());
				step_record.setUsernameElement(login_step.getUsernameElement());
				step_record.setPasswordElement(login_step.getPasswordElement());
				step_record.setSubmitElement(login_step.getSubmitElement());
				step_record.setStartPage(login_step.getStartPage());
				step_record.setEndPage(login_step.getEndPage());

				return step_record;
			}
						
			LoginStep new_login_step = new LoginStep();
			new_login_step.setKey(login_step.generateKey());
			new_login_step = login_step_repo.save(new_login_step);
			
			setStartPage(new_login_step.getId(), login_step.getStartPage().getId());
			new_login_step.setStartPage(login_step.getStartPage());
			
			addEndPage(new_login_step.getId(), login_step.getEndPage().getId());
			new_login_step.setEndPage(login_step.getEndPage());
			
			new_login_step.setUsernameElement(login_step.getUsernameElement());
			
			new_login_step.setPasswordElement(login_step.getPasswordElement());

			new_login_step.setSubmitElement(login_step.getSubmitElement());

			new_login_step.setTestUser(login_step.getTestUser());

			return new_login_step;
		}
		else if(step instanceof LandingStep) {
			LandingStep landing_step_record = landing_step_repo.findByKey(step.getKey());
			
			if(landing_step_record != null) {
				landing_step_record.setStartPage(step.getStartPage());
				
				return landing_step_record;
			}
			else {
				LandingStep landing_step = new LandingStep();
				landing_step.setKey(step.getKey());
				
				Step saved_step = landing_step_repo.save(landing_step);
				setStartPage(saved_step.getId(), step.getStartPage().getId());
				saved_step.setStartPage(step.getStartPage());
				
				return saved_step;
			}
		}
		
		return null;
	}
	

	/**
	 * Checks if page state is listed as a the start page for a journey step
	 * 
	 * @param page_state
	 * @param domain_map_id TODO
	 * @return
	 * 
	 * @pre page_state != null
	 * @pre page_state.getId() != null
	 */
	public List<Step> getStepsWithStartPage(PageState page_state, long domain_map_id) {
		assert page_state != null;
		assert page_state.getId() != null;
				
		return step_repo.getStepsWithStartPage(domain_map_id, page_state.getId());
	}

	public void setElementState(long step_id, long element_id) {
		step_repo.setElementState(step_id, element_id);
	}

	public void addEndPage(long step_id, long page_id) {
		step_repo.addEndPage(step_id, page_id);
	}

	public void setStartPage(Long step_id, Long page_id) {
		step_repo.setStartPage(step_id, page_id);		
	}

	/**
	 * Retrieve single step with given candidate key
	 * @param domain_map_id TODO
	 * @param candidateKey
	 * 
	 * @return
	 */
	public Step findByCandidateKey(String candidate_key, long domain_map_id) {
		return step_repo.findByCandidateKey(candidate_key, domain_map_id);
	}
}
