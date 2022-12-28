package com.looksee.journeyExpander.models.message;

import java.util.UUID;

import org.threeten.bp.LocalDateTime;

/**
 * Core Message object that defines global fields that are to be used by apage_idll Message objects
 */
public abstract class Message {
	private String messageId;
    private String publishTime;
	private long account_id;
	private long domain_id;
	private long domain_audit_record_id;
	
	public Message(){
		setAccountId(-1);
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now().toString();
	}
	
	/**
	 * 
	 * @param account_id
	 * @param audit_record_id TODO
	 * @param domain eg. example.com
	 */
	public Message(long account_id, long audit_record_id, long domain_id){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now().toString();
		
		setAccountId(account_id);
		setDomainAuditRecordId(audit_record_id);
		setDomainId(domain_id);
	}
	
	public long getAccountId() {
		return account_id;
	}

	protected void setAccountId(long account_id) {
		this.account_id = account_id;
	}

	public long getDomainAuditRecordId() {
		return domain_audit_record_id;
	}

	public void setDomainAuditRecordId(long audit_record_id) {
		this.domain_audit_record_id = audit_record_id;
	}

	public long getDomainId() {
		return domain_id;
	}

	public void setDomainId(long domain_id) {
		this.domain_id = domain_id;
	}
	
	public String getMessageId() {
		return messageId;
    }

    public void setMessageId(String messageId) {
    	this.messageId = messageId;
    }

    public String getPublishTime() {
    	return publishTime;
    }

    public void setPublishTime(String publishTime) {
    	this.publishTime = publishTime;
    }
}
