package com.looksee.journeyExpander.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyExpander.models.journeys.LandingStep;


@Repository
public interface LandingStepRepository extends Neo4jRepository<LandingStep, Long> {

	@Query("MATCH (step:LandingStep{key:$step_key}) RETURN step LIMIT 1")
	public LandingStep findByKey(@Param("step_key") String step_key);

}
