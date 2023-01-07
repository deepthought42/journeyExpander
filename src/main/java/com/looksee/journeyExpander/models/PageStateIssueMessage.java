package com.looksee.journeyExpander.models;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.journeyExpander.models.enums.AuditCategory;
import com.looksee.journeyExpander.models.enums.ObservationType;
import com.looksee.journeyExpander.models.enums.Priority;


/**
 * A observation of potential error for a given {@link Element element} 
 */
public class PageStateIssueMessage extends UXIssueMessage {

	@Relationship(type = "FOR")
	private PageState pageState;
	
	public PageStateIssueMessage() {}
	
	public PageStateIssueMessage(
				PageState page, 
				String description,
				String recommendation, 
				Priority priority, 
				AuditCategory category, 
				Set<String> labels,
				String wcag_compliance, 
				String title, 
				int points_awarded, 
				int max_points
	) {
		super(	priority, 
				description, 
				ObservationType.PAGE_STATE,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				recommendation);
		
		setPage(page);
	}

	public PageState getElements() {
		return pageState;
	}


	public void setPage(PageState page_state) {
		this.pageState = page_state;
	}
}
