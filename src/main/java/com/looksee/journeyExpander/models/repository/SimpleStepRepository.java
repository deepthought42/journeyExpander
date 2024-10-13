package com.looksee.journeyExpander.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyExpander.models.journeys.SimpleStep;


@Repository
public interface SimpleStepRepository extends Neo4jRepository<SimpleStep, Long> {

	@Query("MATCH (step:SimpleStep{key:$step_key})-[*1]->() RETURN step LIMIT 1")
	public SimpleStep findByKey(@Param("step_key") String step_key);

	@Query("MATCH (s:SimpleStep) MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN s")
	public SimpleStep addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep) MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN s")
	public SimpleStep addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep) MATCH(p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN s")
	public SimpleStep addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);
}
