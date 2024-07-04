package com.looksee.journeyExpander.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.repository.ElementStateRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ElementStateService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementStateService.class);

	@Autowired
	private ElementStateRepository element_repo;
	
	/**
	 * saves element state to database
	 * 
	 * @param element
	 * @return saved record of element state
	 * 
	 * @pre element != null
	 */
	@Retry(name = "neoforj")
	public ElementState save(ElementState element) {
		assert element != null;

		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null) {
			return element_repo.save(element);
		}
		
		return element_record;
	}
	
	public List<ElementState> getChildElementForParent(String parent_key, String child_element_key) {
		return element_repo.getChildElementForParent(parent_key, child_element_key);
	}
}
