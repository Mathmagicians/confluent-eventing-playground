package dk.mathmagicians.playground.architecture.discovery;

import static java.util.stream.Collectors.toCollection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.api.StereotypeFactory;
import org.jmolecules.stereotype.catalog.StereotypeCatalog;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;
import org.jmolecules.stereotype.catalog.support.CatalogSource;
import org.jmolecules.stereotype.catalog.support.JsonPathStereotypeCatalog;
import org.jmolecules.stereotype.reflection.ArchUnitStereotypeFactory;
import org.jmolecules.stereotype.reflection.ReflectionStereotypeFactory;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModuleDependency;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.DependencyDepth;
import org.springframework.modulith.core.DependencyType;

/// The hexagon of an application as PlantUML. Three columns, driving adapters, application, driven adapters, each
/// holding the Spring Modulith application modules that belong there, placed by the jMolecules stereotypes of
/// their types. Every module is a hexagon; modules without a hexagonal stereotype, the domain, nest inside the
/// application module. Inside a module, a type with a stereotype of the hexagonal group is listed with its role;
/// types of other roles are listed up to a limit per role, then an ellipsis line carries the total. The arrows are
/// the module dependencies Modulith found, from adapters into the application, drawn between the types where both
/// have a card: solid `uses` where a Spring bean is injected, dashed `implements` where the adapter's type
/// implements the application's, a port, dashed `depends on` otherwise. Everything is sorted, so the same code
/// gives the same text.
public final class HexagonDocumenter {

    /// The group of the jMolecules hexagonal stereotypes, as their catalog names it.
    static final String HEXAGONAL = "architecture.hexagonal";

    /// Types of a non-hexagonal role are listed below this many; from it on, one fewer and an ellipsis line.
    static final int LIMIT = 10;

    /// The role under a type's name, smaller than the name.
    static final int ROLE_FONT_SIZE = 10;

    private static final String PRIMARY_ADAPTER = HEXAGONAL + ".PrimaryAdapter";
    private static final String SECONDARY_ADAPTER = HEXAGONAL + ".SecondaryAdapter";
    private static final String ADAPTER = HEXAGONAL + ".Adapter";
    private static final String PACKAGE_INFO = "package-info";
    private static final String INDENT = "  ";
    private static final String MODULE = "hexagon";

    /// The rings. The three columns are written left to right; the domain ring nests inside the application.
    enum Ring {
        DRIVING("DRIVING ADAPTERS"),
        CORE("APPLICATION"),
        DOMAIN("DOMAIN"),
        DRIVEN("DRIVEN ADAPTERS");

        final String label;

        Ring(String label) {
            this.label = label;
        }
    }

    /// What an arrow says, from the strongest statement down.
    enum Kind {
        USES("uses", true),
        IMPLEMENTS("implements", false),
        DEPENDS_ON("depends on", false);

        final String label;
        final boolean solid;

        Kind(String label, boolean solid) {
            this.label = label;
            this.solid = solid;
        }

        static Kind strongest(Kind one, Kind other) {
            return one.ordinal() <= other.ordinal() ? one : other;
        }

        /// The dashed pair as written, or with the dots replaced for a solid line, and the label.
        String draw(String pair) {
            return (solid ? pair.replace(".", "-") : pair) + " : " + label;
        }
    }

    private final ApplicationModules modules;
    private final StereotypeCatalog catalog;
    private final StereotypeFactory<JavaPackage, JavaClass, JavaMethod> stereotypes;

    /// Stereotypes from every catalog on the class path, the `META-INF/jmolecules-stereotypes.json` files.
    public HexagonDocumenter(ApplicationModules modules) {
        this(modules, new JsonPathStereotypeCatalog(
                CatalogSource.ofClassLoader(HexagonDocumenter.class.getClassLoader())));
    }

    HexagonDocumenter(ApplicationModules modules, JsonPathStereotypeCatalog catalog) {
        this.modules = modules;
        this.catalog = catalog;
        this.stereotypes = new ArchUnitStereotypeFactory(new ReflectionStereotypeFactory(catalog));
    }

    /// Writes `toPlantUml()` to the file, creating its directories.
    public void writeTo(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, toPlantUml());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }

    public String toPlantUml() {
        var rings = new EnumMap<Ring, List<ApplicationModule>>(Ring.class);
        modules.stream()
                .sorted(Comparator.comparing(ApplicationModule::getDisplayName))
                .forEach(module -> rings.computeIfAbsent(ring(module), _ -> new ArrayList<>()).add(module));

        var out = new ArrayList<String>();
        out.add("@startuml");
        out.add("' the hexagon from the code: Spring Modulith modules placed by their jMolecules stereotypes");
        out.add("skinparam shadowing false");
        out.add("skinparam defaultTextAlignment left");
        out.add("<style>");
        out.add("hexagon { HorizontalAlignment center }");
        out.add("rectangle { HorizontalAlignment center }");
        out.add("</style>");
        out.add("");
        var cards = new TreeMap<String, String>();
        column(out, Ring.DRIVING, rings.getOrDefault(Ring.DRIVING, List.of()), List.of(), cards);
        column(out, Ring.CORE, rings.getOrDefault(Ring.CORE, List.of()), rings.getOrDefault(Ring.DOMAIN, List.of()),
                cards);
        column(out, Ring.DRIVEN, rings.getOrDefault(Ring.DRIVEN, List.of()), List.of(), cards);
        out.add("");
        dependencies(out, rings, cards);
        out.add("@enduml");
        return String.join("\n", out) + "\n";
    }

    /// A column: a rectangle with the ring's name, its modules as hexagons stacked down. The nested modules go
    /// inside the first module of the column, or straight into the column when it has none of its own. `cards`
    /// collects the alias of every hexagonal type's card by type name, for the arrows.
    private void column(List<String> out, Ring ring, List<ApplicationModule> members,
                        List<ApplicationModule> nested, Map<String, String> cards) {
        if (members.isEmpty() && nested.isEmpty()) {
            return;
        }
        out.add("rectangle \"" + ring.label + "\" as " + ring.name().toLowerCase() + " {");
        var stacked = new ArrayList<String>();
        if (members.isEmpty()) {
            nested.forEach(module -> stacked.add(box(out, module, INDENT, List.of(), cards)));
        }
        for (var i = 0; i < members.size(); i++) {
            var inside = new ArrayList<String>();
            if (i == 0) {
                nested.forEach(module -> box(inside, module, INDENT + INDENT, List.of(), cards));
            }
            stacked.add(box(out, members.get(i), INDENT, inside, cards));
        }
        stack(out, INDENT, stacked);
        out.add("}");
    }

    /// One module: its hexagon, one card per hexagonal type with its role, grouped by role, then the types of every
    /// other role, up to `LIMIT` per role and an ellipsis line beyond it, then whatever is nested inside it.
    /// Answers its alias.
    private String box(List<String> out, ApplicationModule module, String indent, List<String> nested,
                       Map<String, String> cards) {
        out.add(indent + MODULE + " \"" + module.getDisplayName() + "\" as " + alias(module) + " {");
        var hexagonal = new TreeMap<String, List<JavaClass>>();
        var others = new TreeMap<String, List<String>>();
        types(module).forEach(type -> primary(type).ifPresent(stereotype -> {
            if (stereotype.belongsToGroup(HEXAGONAL)) {
                hexagonal.computeIfAbsent(role(stereotype), _ -> new ArrayList<>()).add(type);
            } else {
                others.computeIfAbsent(role(stereotype), _ -> new ArrayList<>()).add(type.getSimpleName());
            }
        }));
        var stacked = new ArrayList<String>();
        hexagonal.forEach((role, types) -> types.forEach(type -> {
            var alias = card(out, indent, module, type.getSimpleName(), role, type.getSimpleName());
            cards.put(type.getName(), alias);
            stacked.add(alias);
        }));
        others.forEach((role, types) -> {
            var shown = types.size() < LIMIT ? types : types.subList(0, LIMIT - 1);
            shown.forEach(type -> stacked.add(card(out, indent, module, type, role, type)));
            if (shown.size() < types.size()) {
                stacked.add(card(out, indent, module, "... (" + types.size() + " total)", role, role + "_total"));
            }
        });
        stack(out, indent, stacked);
        out.addAll(nested);
        out.add(indent + "}");
        return alias(module);
    }

    /// Writes the card, the type on the first line and its role in guillemets below in a smaller font, so cards
    /// stay narrow and the diagram grows down. Answers the card's alias.
    private static String card(List<String> out, String indent, ApplicationModule module, String label, String role,
                               String name) {
        var alias = alias(module) + "_" + name.replaceAll("[^A-Za-z0-9]", "_");
        out.add(indent + INDENT + "card \"" + label + "\\n<size:" + ROLE_FONT_SIZE + ">«" + role + "»</size>\" as "
                + alias);
        return alias;
    }

    /// A hidden arrow from each element to the next stacks them, so a box grows down instead of sideways.
    private static void stack(List<String> out, String indent, List<String> aliases) {
        for (var i = 1; i < aliases.size(); i++) {
            out.add(indent + INDENT + aliases.get(i - 1) + " -[hidden]-> " + aliases.get(i));
        }
    }

    /// The arrows: from each adapter module into the application modules it depends on, between the two types
    /// where both have a card, the module's hexagon where one has not; one arrow per pair, sorted. A driving
    /// adapter points right into the hexagon, a driven adapter left. Dependencies on the domain are not drawn: the
    /// domain sits inside the application, and its arrows would cross everything to say what the nesting says.
    private void dependencies(List<String> out, Map<Ring, List<ApplicationModule>> rings, Map<String, String> cards) {
        var core = rings.getOrDefault(Ring.CORE, List.of());
        var lines = new TreeMap<String, Kind>();
        for (var ring : List.of(Ring.DRIVING, Ring.DRIVEN)) {
            for (var module : rings.getOrDefault(ring, List.of())) {
                module.getDependencies(modules, DependencyDepth.IMMEDIATE).stream()
                        .filter(dependency -> core.contains(dependency.getTargetModule()))
                        .forEach(dependency -> {
                            var source = cardOr(cards, dependency.getSourceType(), alias(module));
                            var target = cardOr(cards, dependency.getTargetType(), alias(dependency.getTargetModule()));
                            lines.merge(source + " " + arrow(ring) + " " + target, kind(dependency), Kind::strongest);
                        });
            }
        }
        lines.forEach((pair, kind) -> out.add(kind.draw(pair)));
    }

    /// The card of the type, or of the top-level type it is nested in, `Recipe` inside `GenerateLoad`; the module's
    /// hexagon when neither has one.
    private static String cardOr(Map<String, String> cards, JavaClass type, String module) {
        var outer = type;
        while (outer.getEnclosingClass().isPresent()) {
            outer = outer.getEnclosingClass().get();
        }
        return cards.getOrDefault(outer.getName(), module);
    }

    /// The dashed form; the solid form replaces the dots.
    private static String arrow(Ring ring) {
        return switch (ring) {
            case DRIVING -> ".right.>";
            case DRIVEN -> ".left.>";
            case CORE, DOMAIN -> "..>";
        };
    }

    private static Kind kind(ApplicationModuleDependency dependency) {
        if (dependency.getDependencyType() == DependencyType.USES_COMPONENT) {
            return Kind.USES;
        }
        return dependency.getSourceType().isAssignableTo(dependency.getTargetType().getName())
                ? Kind.IMPLEMENTS
                : Kind.DEPENDS_ON;
    }

    /// Which ring a module belongs to, by the hexagonal stereotypes of its types. A qualified adapter carries the
    /// unqualified `Adapter` stereotype as well, so the qualified ones decide first.
    private Ring ring(ApplicationModule module) {
        var identifiers = types(module)
                .flatMap(this::ownStereotypes)
                .map(Stereotype::getIdentifier)
                .collect(toCollection(TreeSet::new));
        if (identifiers.contains(PRIMARY_ADAPTER)) {
            return Ring.DRIVING;
        }
        if (identifiers.contains(SECONDARY_ADAPTER)) {
            return Ring.DRIVEN;
        }
        if (identifiers.contains(ADAPTER)) {
            return Ring.DRIVING;
        }
        if (identifiers.stream().anyMatch(identifier -> identifier.startsWith(HEXAGONAL + "."))) {
            return Ring.CORE;
        }
        return Ring.DOMAIN;
    }

    /// The module's types, sorted by name, without the package's own class and without inner classes.
    private static java.util.stream.Stream<JavaClass> types(ApplicationModule module) {
        return module.getBasePackage().stream()
                .filter(type -> !type.getSimpleName().equals(PACKAGE_INFO))
                .filter(type -> !type.isInnerClass() && !type.isAnonymousClass())
                .sorted(Comparator.comparing(JavaClass::getName));
    }

    /// The type's stereotype of highest priority, which the catalog defines.
    private java.util.Optional<Stereotype> primary(JavaClass type) {
        return ownStereotypes(type).sorted().findFirst();
    }

    /// The stereotypes declared on the type itself. The factory hands a type its package's stereotypes as well,
    /// `@Module` on a `package-info` reaches every type in the package, and those are the package's role, not the
    /// type's. A stereotype is the type's own when one of the annotations the catalog assigns it by is on the type,
    /// directly or as a meta-annotation.
    private java.util.stream.Stream<Stereotype> ownStereotypes(JavaClass type) {
        return stereotypes.fromType(type).stream().filter(stereotype -> declaredOn(type, stereotype));
    }

    private boolean declaredOn(JavaClass type, Stereotype stereotype) {
        return catalog.getDefinition(stereotype).getAssignments().stream()
                .map(StereotypeDefinition.Assignment::getTarget)
                .map(target -> target.startsWith("@") ? target.substring(1) : target)
                .anyMatch(type::isMetaAnnotatedWith);
    }

    /// The last segment of the identifier: `PrimaryPort` from `architecture.hexagonal.PrimaryPort`.
    private static String role(Stereotype stereotype) {
        var identifier = stereotype.getIdentifier();
        return identifier.substring(identifier.lastIndexOf('.') + 1);
    }

    private static String alias(ApplicationModule module) {
        return "m_" + module.getDisplayName().replaceAll("[^A-Za-z0-9]", "_");
    }
}
