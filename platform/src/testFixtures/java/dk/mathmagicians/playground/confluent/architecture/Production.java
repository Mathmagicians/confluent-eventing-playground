package dk.mathmagicians.playground.confluent.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

/// The classes the architecture tests look at: production code alone. ArchUnit's own option leaves out a
/// project's tests; the platform's test fixtures, the shared BDD, travel as a jar and a classes folder of their
/// own, so they are left out here as well. For `@AnalyzeClasses(importOptions)` and `ApplicationModules.of`.
public final class Production implements ImportOption {

    private static final ImportOption WITHOUT_TESTS = new ImportOption.DoNotIncludeTests();

    @Override
    public boolean includes(Location location) {
        return WITHOUT_TESTS.includes(location)
                && !location.contains("test-fixtures")
                && !location.contains("testFixtures");
    }
}
