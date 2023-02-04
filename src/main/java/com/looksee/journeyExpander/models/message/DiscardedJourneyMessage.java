package com.looksee.journeyExpander.models.message;

import com.looksee.journeyExpander.models.enums.BrowserType;
import com.looksee.journeyExpander.models.journeys.Journey;

public class DiscardedJourneyMessage extends Message {

	private Journey journey;
	private BrowserType browserType;
	private long domainId;
	private long accountId;
	private long auditRecordId;
   
	private DiscardedJourneyMessage(Journey journey, 
								   BrowserType browserType, 
								   long domainId, 
								   long accountId, 
								   long auditRecordId) {
		setJourney(journey);
		setBrowserType(browserType);
		setDomainId(domainId);
		setAccountId(accountId);
		setAuditRecordId(auditRecordId);
	}

	public BrowserType getBrowserType() {
		return browserType;
	}

	public void setBrowserType(BrowserType browserType) {
		this.browserType = browserType;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public long getAuditRecordId() {
		return auditRecordId;
	}

	public void setAuditRecordId(long auditRecordId) {
		this.auditRecordId = auditRecordId;
	}

	public Journey getJourney() {
		return journey;
	}

	public void setJourney(Journey journey) {
		this.journey = journey;
	}

}
