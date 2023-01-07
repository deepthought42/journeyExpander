package com.looksee.journeyExpander.models.journeys;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.journeyExpander.models.LookseeObject;
import com.looksee.journeyExpander.models.PageState;


@Node
public class Step extends LookseeObject {
	
	@Relationship(type = "STARTS_WITH")
	private PageState startPage;
	
	@Relationship(type = "ENDS_WITH")
	private PageState endPage;

	public Step() {}
	
	public Step(PageState start_page, PageState end_page) {
		setStartPage(start_page);
		setEndPage(end_page);
	}
	
	public PageState getStartPage() {
		return startPage;
	}
	
	public void setStartPage(PageState page_state) {
		this.startPage = page_state;
	}
	
	
	public PageState getEndPage() {
		return this.endPage;
	}
	
	public void setEndPage(PageState page_state) {
		this.endPage = page_state;
	}
	
	@Override
	public String generateKey() {
		String key = "";
		if(startPage != null) {
			key += startPage.getId();
		}
		if(endPage != null) {
			key += endPage.getId();
		}
		return "step"+key;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Step clone() {
		return new Step(getStartPage(), getEndPage());
	}
}
