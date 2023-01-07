package com.looksee.journeyExpander.models.journeys;


import com.looksee.journeyExpander.models.enums.Action;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.looksee.journeyExpander.models.ElementState;
import com.looksee.journeyExpander.models.PageState;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState} 
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}
 */
@Node
public class SimpleStep extends Step {
	
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private ElementState element;
	
	private String action;
	private String actionInput;
	
	public SimpleStep() {
		super();
		setActionInput("");
		setAction(Action.UNKNOWN);
	}
	
	public SimpleStep(PageState start_page,
				ElementState element,
				Action action,
				String action_input, 
				PageState end_page) {
		setStartPage(start_page);
		setElementState(element);
		setAction(action);
		setActionInput(action_input);
		setEndPage(end_page);
		setKey(generateKey());
	}
	
	@Override
	public SimpleStep clone() {
		return new SimpleStep(getStartPage(), getElementState(), getAction(), getActionInput(), getEndPage());
	}
	
	public ElementState getElementState() {
		return this.element;
	}
	
	public void setElementState(ElementState element) {
		this.element = element;
	}
	
	public Action getAction() {
		return Action.create(action);
	}
	
	public void setAction(Action action) {
		this.action = action.getShortName();
	}

	@Override
	public String generateKey() {
		String key = "";
		if(getStartPage() != null) {
			key += getStartPage().getId();
		}
		if(element != null) {
			key += element.getId();
		}
		if(getEndPage() != null) {
			key += getEndPage().getId();
		}
		return "simplestep"+key+action+actionInput;
	}

	
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage()+"\n element ="+getElementState()+"\n end page = "+getEndPage();
	}
	
	public String getActionInput() {
		return actionInput;
	}

	public void setActionInput(String action_input) {
		this.actionInput = action_input;
	}
}
