package com.looksee.journeyExpander.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.looksee.journeyExpander.models.enums.JourneyStatus;
import com.looksee.journeyExpander.models.journeys.Journey;

public interface JourneyRepository extends Neo4jRepository<Journey, Long>  {
	
	public Journey findByKey(@Param("key") String key);

	@Query("MATCH (map:DomainMap)-[:CONTAINS]->(j:Journey{key:$key}) WHERE id(map)=$map_id RETURN j LIMIT 1")
	public Journey findByKey(@Param("map_id") long domain_map_id, @Param("key") String key);
	
	@Query("MATCH (j:Journey) WITH j MATCH (s:Step) WHERE id(s)=$step_id AND id(j)=$journey_id MERGE (j)-[:HAS]->(s) RETURN j")
	public Journey addStep(@Param("journey_id") long journey_id, @Param("step_id") long id);

	@Query("MATCH (audit:DomainAuditRecord) WHERE id(audit)=$audit_id MATCH (audit)-[*2]->(j:Journey) WHERE j.status=$status RETURN COUNT(j)")
	public int findAllJourneysForDomainAudit(@Param("audit_id") long audit_id, @Param("status") JourneyStatus status);
	

	@Query("MATCH (j:Journey) WHERE id(j)=$journey_id SET j.status=$status, j.key=$key, j.orderedIds=$ordered_ids RETURN j")
	public Journey updateFields(@Param("journey_id") long journey_id, 
								@Param("status") JourneyStatus status, 
								@Param("key") String key,
								@Param("ordered_ids") List<Long> ordered_ids);

	@Query("MATCH (map:DomainMap) WHERE id(map)=$map_id MATCH (map)-[:CONTAINS]->(j:Journey{candidateKey:$candidateKey}) RETURN j LIMIT 1")
	public Journey findByCandidateKey(@Param("map_id") long domain_map_id, @Param("candidateKey") String candidate_key);
}
