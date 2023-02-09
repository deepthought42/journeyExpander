package com.looksee.journeyExpander.models.journeys;


import com.looksee.journeyExpander.models.enums.Action;
import com.looksee.journeyExpander.models.enums.StepType;

import org.springframework.data.neo4j.core.schema.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState} 
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LANDING")
@Node
public class LandingStep extends Step {
	
	public LandingStep() {
		super();
	}
	
	public LandingStep(PageState start_page) 
	{
		setStartPage(start_page);
		setKey(generateKey());
	}

	@Override
	public LandingStep clone() {
		return new LandingStep(getStartPage());
	}
	
	@Override
	public String generateKey() {
		return "landingstep"+getStartPage().getId();
	}

	
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage();
	}

	@Override
	StepType getStepType() {
		return StepType.LANDING;
	}
}
