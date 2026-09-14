/// Driving adapter: Spring Boot's process, whose arguments start the story and whose context closes at the ttl.
@PrimaryAdapter
@Module(name = "Boot")
package dk.mathmagicians.playground.confluent.eventing.adapter.cli.boot;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jmolecules.ddd.annotation.Module;
