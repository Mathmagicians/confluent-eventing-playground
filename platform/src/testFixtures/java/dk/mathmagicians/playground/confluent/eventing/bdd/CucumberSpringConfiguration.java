package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

/// The Spring context of the BDD suite: the profiles the `bdd` task names, `<transport>,<env>`, with the Kafka
/// client and topics they give, and the drivers as beans, the `Transport` the one of the profile. The generators
/// run as containers, not in this JVM.
/// Lazy initialization keeps a dry run free of credentials: a missing variable fails at the first step that needs
/// it, with the variable's name.
@CucumberContextConfiguration
@SpringBootTest(
        classes = CucumberSpringConfiguration.class,
        webEnvironment = WebEnvironment.NONE,
        properties = "spring.main.lazy-initialization=true")
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@Import({
    Cluster.class, SchemaRegistry.class, ClusterTransport.class, ConsoleTransport.class, GeneratorContainer.class,
    StoryContainer.class, Published.class, Region.class
})
class CucumberSpringConfiguration {}
