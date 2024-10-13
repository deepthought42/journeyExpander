package com.looksee.journeyExpander.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyExpander.models.Domain;
import com.looksee.journeyExpander.models.repository.DomainRepository;

@Service
public class DomainService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainRepository domain_repo;

	public Domain findByUrl(String url) {
		return domain_repo.findByUrl(url);
	}

	public Domain findByAuditRecord(long audit_record_id) {
		return domain_repo.findByAuditRecord(audit_record_id);
	}
}
