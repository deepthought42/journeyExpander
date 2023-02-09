package com.looksee.journeyExpander.models;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.journeyExpander.models.enums.ObservationType;
import com.looksee.journeyExpander.models.enums.Priority;
import com.looksee.journeyExpander.models.recommend.Recommendation;


/**
 * Details issues for when a page is devoid of a certain styling such as padding, 
 * that should be used, because it adds extra white-space to the content
 */
@Node
public class StylingMissingIssueMessage extends UXIssueMessage {
	
	public StylingMissingIssueMessage(
			String description, 
			Set<Recommendation> recommendation, 
			Priority priority) {
		super();
		
		assert description != null;
		setDescription(description);
		setType(ObservationType.STYLE_MISSING);
		setKey(generateKey());
	}

	@Override
	public ObservationType getType() {
		return ObservationType.STYLE_MISSING;
	}

}
