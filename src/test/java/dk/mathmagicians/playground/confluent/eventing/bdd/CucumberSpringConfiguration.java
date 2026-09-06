package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/// The Spring context of the BDD suite: empty for now, the generators run as containers, not in this JVM.
@CucumberContextConfiguration
@SpringBootTest(classes = CucumberSpringConfiguration.class, webEnvironment = WebEnvironment.NONE)
class CucumberSpringConfiguration {}
