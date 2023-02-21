package com.looksee.journeyExpander.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyExpander.models.AuditRecord;
import com.looksee.journeyExpander.models.DomainAuditRecord;
import com.looksee.journeyExpander.models.PageAuditRecord;
import com.looksee.journeyExpander.models.PageState;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
@Repository
@Retry(name = "neoforj")
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key}) WITH ar MATCH (a:Audit{key:$audit_key}) MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);
	
	@Query("MATCH (ar:PageAuditRecord) WITH ar MATCH (a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);
	
	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord{key:$page_audit_key}) WHERE id(dar)=$domain_audit_record_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_key") String page_audit_key);

	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord) WHERE id(dar)=$domain_audit_record_id AND id(par)=$page_audit_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_id") long page_audit_record_id);
	
	@Query("MATCH (d:Domain)-[]-(ar:DomainAuditRecord) WHERE id(d)=$domain_id RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<AuditRecord> findMostRecentDomainAuditRecord(@Param("domain_id") long domain_id);

	@Query("MATCH (domain_audit:DomainAuditRecord)-[:HAS]->(audit:PageAuditRecord) WHERE id(domain_audit)=$domain_audit_id RETURN audit")
	public Set<AuditRecord> getAllPageAudits(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{url:$url}) RETURN page_audit ORDER BY page_audit.created_at DESC LIMIT 1")
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(@Param("url") String url);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	@Deprecated
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit)=$page_audit_id RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (ar:AuditRecord) WITH ar MATCH (page:PageState) WHERE id(ar)=$audit_record_id AND id(page)=$page_state_id MERGE (ar)-[h:HAS]->(page) RETURN ar")
	public void addPageToAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("page_state_id") long page_state_id);

	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit) MATCH (audit)-[:HAS]-(issue:UXIssueMessage{priority:$severity}) WHERE id(audit_record)=$audit_record_id RETURN count(issue) as count")
	public long getIssueCountBySeverity(@Param("audit_record_id") long id, @Param("severity") String severity);

	@Query("MATCH (audit_record:DomainAuditRecord)-[]->(page_audit:PageAuditRecord) WHERE id(audit_record)=$audit_record_id RETURN count(page_audit) as count")
	public int getPageAuditRecordCount(@Param("audit_record_id") long domain_audit_id);

	@Query("MATCH (doman_audit:DomainAuditRecord)-[:HAS]->(page_audit:PageAuditRecord) WHERE id(page_audit)=$audit_record_id RETURN doman_audit LIMIT 1")
	public Optional<DomainAuditRecord> getDomainForPageAuditRecord(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (journey:Journey) WHERE id(ar)=$audit_record_id AND id(journey)=$journey_id MERGE (ar)-[:HAS_PATH]->(journey) RETURN ar")
	public AuditRecord addJourney(@Param("audit_record_id") long audit_record_id, @Param("journey_id")  long journey_id);

	@Query("MATCH(d:Domain{url: $url}) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(@Param("url") String url);
	
	@Query("MATCH(d:Domain) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit WHERE id(d)=$id RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(@Param("id") long id);

	@Query("MATCH (d:Domain{key:$domain_key})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	@Query("MATCH (d:Domain{key:$domain_key})<-[]-(audit:AuditRecord{key:$audit_record_key}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);
	
	@Query("MATCH(d:Domain) WITH d WHERE id(d)=$domain_id MATCH (ar:DomainAuditRecord)-[]->(d) MATCH y=(ar)-[:HAS]->(page_audit:PageAuditRecord) MATCH z=(page_audit)-[:HAS]->(audit:Audit) RETURN y,z")
	public List<DomainAuditRecord> getAuditRecordHistory(@Param("domain_id") long domain_id);

	@Query("MATCH (a:PageAuditRecord)-[:FOR]->(ps:PageState) WHERE id(ps)=$id RETURN a ORDER BY a.created_at DESC LIMIT 1")
	public PageAuditRecord getAuditRecord(@Param("id") long id);
	
	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (map:DomainMap) WHERE id(ar)=$audit_record_id AND id(map)=$domain_map_id MERGE (ar)-[:CONTAINS]->(map) RETURN ar")
	public AuditRecord addDomainMap(@Param("audit_record_id") long audit_record_id, @Param("domain_map_id") long domain_map_id);

}
