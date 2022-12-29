package com.looksee.journeyExpander.services;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.looksee.browsing.ActionFactory;
import com.looksee.journeyExpander.models.Browser;
import com.looksee.journeyExpander.models.journeys.SimpleStep;
import com.looksee.journeyExpander.models.journeys.Step;

@Service
public class StepExecutor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepExecutor.class);
	
	public void execute(Browser browser, Step step) {
		assert browser != null;
		assert step != null;
		
		//step.setElementState(step_service.getElementState(step.getKey()));
		if(step instanceof SimpleStep) {
	
			WebElement interactive_elem = browser.getDriver().findElement(By.xpath(((SimpleStep)step).getElementState().getXpath()));
			ActionFactory action_factory = new ActionFactory(browser.getDriver());
			action_factory.execAction(interactive_elem, ((SimpleStep)step).getActionInput(), ((SimpleStep)step).getAction());
		}
		/*
		if(step instanceof ElementInteractionStep) {
			ElementInteractionStep interaction_step = (ElementInteractionStep)step;
			
			interaction_step.setElement(step_service.getElementState(interaction_step.getKey()));
			interaction_step.setAction(step.getAction());

			WebElement interactive_elem = browser.getDriver().findElement(By.xpath(interaction_step.getElement().getXpath()));
			ActionFactory action_factory = new ActionFactory(browser.getDriver());
			action_factory.execAction(interactive_elem, interaction_step.getActionInput(), interaction_step.getAction());
		}
		else if (step instanceof NavigationStep) {
			log.warn("navigation step url   :: "+((NavigationStep) step).getUrl());
			browser.navigateTo(((NavigationStep) step).getUrl());
			browser.removeDriftChat();
		}
		*/
	}
}
