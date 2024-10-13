package com.looksee.journeyExpander.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;
import com.looksee.journeyExpander.models.repository.ElementStateRepository;
import com.looksee.journeyExpander.models.repository.PageStateRepository;

import io.github.resilience4j.retry.annotation.Retry;



/**
 * Service layer object for interacting with {@link PageState} database layer
 *
 */
@Service
@Retry(name="neo4j")
public class PageStateService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageStateService.class.getName());
	
	@Autowired
	private PageStateRepository page_state_repo;

	@Autowired
	private ElementStateRepository element_state_repo;
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state
	 * @return
	 * @throws Exception 
	 * 
	 * @pre page_state != null
	 */
	public PageState save(PageState page_state) throws Exception {
		assert page_state != null;
		
		PageState page_state_record = page_state_repo.findByKey(page_state.getKey());
		
		if(page_state_record == null) {
			log.warn("page state wasn't found in database. Saving new page state to neo4j");

			return page_state_repo.save(page_state);
		}

		return page_state_record;
	}
	
	public PageState findByKey(String page_key) {
		PageState page_state = page_state_repo.findByKey(page_key);
		if(page_state != null){
			page_state.setElements(getElementStates(page_key));
		}
		return page_state;
	}

	public List<ElementState> getElementStates(String page_key){
		assert page_key != null;
		assert !page_key.isEmpty();
		
		return element_state_repo.getElementStates(page_key);
	}
	
	/**
	 * 
	 * @param page_state_id
	 * @return
	 */
	@Retryable
	public List<ElementState> getElementStates(long page_state_id){
		return element_state_repo.getElementStates(page_state_id);
	}

	public PageState findByUrl(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return page_state_repo.findByUrl(url);
	}
}
