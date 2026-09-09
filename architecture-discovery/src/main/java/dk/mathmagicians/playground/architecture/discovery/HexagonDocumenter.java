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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
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
/// application module, the application's own types in a row above it. Inside a module, every type with a
/// stereotype is a card, its role below its name, in a grid; the permitted types of a sealed type sit in a row
/// under the grid and implement it, one arrowhead. Types of a non-hexagonal role are listed up to the options'
/// maximum, then an ellipsis line carries the total. The arrows, drawn in the legend: from an adapter type into
/// the application and between the application's types and the domain's, `uses` where one holds the other,
/// `implements` where one implements the other, `depends on` for any other reference. Everything is sorted, so the
/// same code gives the same text.
public final class HexagonDocumenter {

    /// Choices for the picture: how many types of one role a box lists before the ellipsis line. Unlimited by
    /// default.
    public record Options(int maxTypes) {

        public static Options defaults() {
            return new Options(Integer.MAX_VALUE);
        }

        public Options withMaxTypes(int maxTypes) {
            if (maxTypes < 1) {
                throw new IllegalArgumentException("maxTypes must be positive, was " + maxTypes);
            }
            return new Options(maxTypes);
        }
    }

    /// The group of the jMolecules hexagonal stereotypes, as their catalog names it.
    static final String HEXAGONAL = "architecture.hexagonal";

    /// The small font: a role under a type's name, the legend.
    static final int SMALL_FONT_SIZE = 10;

    /// The legend's colours, a grey line and grey text, so it stays discrete beside the hexagon.
    private static final String LEGEND_LINE = "BBBBBB";
    private static final String LEGEND_TEXT = "666666";

    private static final String PRIMARY_ADAPTER = HEXAGONAL + ".PrimaryAdapter";
    private static final String SECONDARY_ADAPTER = HEXAGONAL + ".SecondaryAdapter";
    private static final String ADAPTER = HEXAGONAL + ".Adapter";
    private static final String PACKAGE_INFO = "package-info";
    private static final String INDENT = "  ";
    private static final String MODULE = "hexagon";
    private static final String FRAME = "rectangle";
    /// A frame that groups without being seen: a column under its title, the padding inside a hexagon.
    private static final String UNSEEN = " #line:transparent";
    /// A text too small to see: the title of a padding frame, the node where the lines of several permitted types
    /// meet before the one arrow to their sealed type.
    private static final String UNSEEN_TEXT = "\"<size:1> </size>\"";
    private static final String JUNCTION = "label " + UNSEEN_TEXT + " as ";
    /// The permitted types of a sealed type in one line, two when there are more than this.
    private static final int FAMILY_COLUMNS = 4;

    /// The rings. The three columns are written left to right; the domain ring nests inside the application.
    enum Ring {
        DRIVING("DRIVING ADAPTERS", "right"),
        CORE("APPLICATION", ""),
        DOMAIN("DOMAIN", ""),
        DRIVEN("DRIVEN ADAPTERS", "left");

        final String label;
        /// The way an arrow from this ring points into the hexagon.
        final String direction;

        Ring(String label, String direction) {
            this.label = label;
            this.direction = direction;
        }
    }

    /// The relation an arrow draws, from the strongest statement down. Each has its UML line: solid for a use,
    /// dashed with the hollow head for a realization, dashed for a dependency.
    enum Kind {
        USES("uses"),
        IMPLEMENTS("implements"),
        DEPENDS_ON("depends on");

        final String relation;

        Kind(String relation) {
            this.relation = relation;
        }

        String alias() {
            return "l_" + name().toLowerCase();
        }

        static Kind strongest(Kind one, Kind other) {
            return one.ordinal() <= other.ordinal() ? one : other;
        }

        String line(String direction) {
            return switch (this) {
                case USES -> "-" + direction + "->";
                case IMPLEMENTS -> "." + direction + ".|>";
                case DEPENDS_ON -> "." + direction + ".>";
            };
        }
    }

    /// A cell of a grid: the alias at its left edge and at its right edge, one and the same for a card, the two
    /// ends of a sample arrow in the legend; the alias at its top, the middle of a grid's first row, where the cell
    /// hangs from what sits above; and the cards along its bottom edge, the card itself, or the last row of a grid,
    /// where what hangs below attaches.
    record Cell(String left, String right, String top, List<String> bottom) {

        static Cell of(String alias) {
            return new Cell(alias, alias, alias, List.of(alias));
        }

        boolean isBox() {
            return !bottom.equals(List.of(left));
        }
    }

    /// A type's entry in a module: its card, and the cards of its permitted types when it is sealed.
    record Entry(Cell card, List<Cell> family) {
    }

    private final ApplicationModules modules;
    private final Options options;
    private final StereotypeCatalog catalog;
    private final StereotypeFactory<JavaPackage, JavaClass, JavaMethod> stereotypes;

    /// Stereotypes from every catalog on the class path, the `META-INF/jmolecules-stereotypes.json` files.
    public HexagonDocumenter(ApplicationModules modules) {
        this(modules, Options.defaults());
    }

    public HexagonDocumenter(ApplicationModules modules, Options options) {
        this(modules, options, new JsonPathStereotypeCatalog(
                CatalogSource.ofClassLoader(HexagonDocumenter.class.getClassLoader())));
    }

    HexagonDocumenter(ApplicationModules modules, Options options, JsonPathStereotypeCatalog catalog) {
        this.modules = modules;
        this.options = options;
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
        out.add("skinparam ranksep 24");
        out.add("skinparam nodesep 24");
        out.add("skinparam ArrowFontSize " + SMALL_FONT_SIZE);
        out.add("skinparam ArrowFontColor #" + LEGEND_TEXT);
        out.add("<style>");
        out.add("hexagon { HorizontalAlignment center }");
        out.add("rectangle { HorizontalAlignment center }");
        out.add("</style>");
        out.add("");
        var cards = new TreeMap<String, String>();
        column(out, Ring.DRIVING, rings.getOrDefault(Ring.DRIVING, List.of()), List.of(), cards);
        var anchors = column(out, Ring.CORE, rings.getOrDefault(Ring.CORE, List.of()),
                rings.getOrDefault(Ring.DOMAIN, List.of()), cards);
        column(out, Ring.DRIVEN, rings.getOrDefault(Ring.DRIVEN, List.of()), List.of(), cards);
        out.add("");
        arrows(out, rings, cards);
        out.add("");
        legend(out, rings, cards, anchors);
        out.add("@enduml");
        return String.join("\n", out) + "\n";
    }

    /// A column: an unseen frame with the ring's name as its title, its modules as hexagons in a grid of one
    /// column. The nested modules go inside the first module of the column, or straight into the column when it
    /// has none of its own. `cards` collects the alias of every stereotyped type's card by type name, for the
    /// arrows. Answers the cards along the column's bottom, where the legend hangs.
    private List<String> column(List<String> out, Ring ring, List<ApplicationModule> members,
                                List<ApplicationModule> nested, Map<String, String> cards) {
        if (members.isEmpty() && nested.isEmpty()) {
            return List.of();
        }
        out.add(FRAME + " \"" + ring.label + "\" as " + ring.name().toLowerCase() + UNSEEN + " {");
        var cells = new ArrayList<Cell>();
        if (members.isEmpty()) {
            nested.forEach(module -> cells.add(box(out, module, INDENT, List.of(), cards)));
        }
        for (var i = 0; i < members.size(); i++) {
            cells.add(box(out, members.get(i), INDENT, i == 0 ? nested : List.of(), cards));
        }
        var bottom = grid(out, "", cells, 1).bottom();
        out.add("}");
        return bottom;
    }

    /// One module: its hexagon, padded by an unseen frame, since PlantUML's hexagon cuts its corners through what
    /// sits there. Inside, the cards of its hexagonal types, grouped by role; then the types of every other role,
    /// up to the options' maximum and an ellipsis line beyond it; then the modules nested inside it, each a
    /// hexagon of its own. The cards sit in a near-square grid, at least as wide as the widest family; each sealed
    /// type's permitted types hang from it in a row of their own below the grid; the nested modules come last, and
    /// above them the cards keep to one row. Answers the module as a cell. An arrow ending on a hexagon's alias
    /// makes PlantUML show the alias as its title, so the cell's ends are the module's first card, the hexagon
    /// itself when it has none.
    private Cell box(List<String> out, ApplicationModule module, String indent, List<ApplicationModule> nested,
                     Map<String, String> cards) {
        out.add(indent + MODULE + " \"" + module.getDisplayName() + "\" as " + alias(module) + " {");
        var inner = indent + INDENT;
        out.add(inner + FRAME + " " + UNSEEN_TEXT + " as " + alias(module) + "_in" + UNSEEN + " {");
        var hexagonal = new TreeMap<String, List<JavaClass>>();
        var others = new TreeMap<String, List<JavaClass>>();
        var permitted = permittedTypes(module);
        types(module)
                .filter(type -> !permitted.contains(type.getName()))
                .forEach(type -> primary(type).ifPresent(stereotype -> {
                    var group = stereotype.belongsToGroup(HEXAGONAL) ? hexagonal : others;
                    group.computeIfAbsent(role(stereotype), _ -> new ArrayList<>()).add(type);
                }));
        var entries = new ArrayList<Entry>();
        hexagonal.forEach((role, types) ->
                types.forEach(type -> entries.add(entry(out, inner, module, type, role, cards))));
        others.forEach((role, types) -> {
            var shown = types.size() <= options.maxTypes() ? types : types.subList(0, options.maxTypes() - 1);
            shown.forEach(type -> entries.add(entry(out, inner, module, type, role, cards)));
            if (shown.size() < types.size()) {
                var total = card(out, inner, module, "... (" + types.size() + " total)", role, role + "_total");
                entries.add(new Entry(Cell.of(total), List.of()));
            }
        });
        var own = entries.stream().map(Entry::card).toList();
        var families = entries.stream().filter(entry -> !entry.family().isEmpty()).toList();
        var rows = new ArrayList<Cell>();
        if (!own.isEmpty()) {
            var widest = families.stream().mapToInt(entry -> columns(entry.family())).max().orElse(1);
            var top = grid(out, inner, own, nested.isEmpty() ? Math.max(square(own.size()), widest) : own.size());
            var bottom = new ArrayList<String>();
            for (var entry : families) {
                var family = grid(out, inner, entry.family(), columns(entry.family()));
                out.add(inner + INDENT + entry.card().left() + " -[hidden]down-> " + family.top());
                bottom.addAll(family.bottom());
            }
            rows.add(families.isEmpty() ? top : new Cell(top.left(), top.right(), top.top(), bottom));
        }
        if (!nested.isEmpty()) {
            var children = nested.stream().map(child -> box(out, child, inner + INDENT, List.of(), cards)).toList();
            rows.add(grid(out, inner, children, children.size()));
        }
        var cell = rows.isEmpty() ? Cell.of(alias(module)) : grid(out, inner, rows, 1);
        out.add(inner + "}");
        out.add(indent + "}");
        return cell;
    }

    /// A card for the type. For a sealed type, cards for its permitted types as well, its family, implementing
    /// it: one permitted type draws its arrow, several draw lines to a junction and the one arrow from there, so
    /// the arrows meet in one head. Registers every card by type name.
    private Entry entry(List<String> out, String indent, ApplicationModule module, JavaClass type, String role,
                        Map<String, String> cards) {
        var alias = card(out, indent, module, type.getSimpleName(), role, type.getSimpleName());
        cards.put(type.getName(), alias);
        var family = new ArrayList<Cell>();
        type.getPermittedSubclasses().orElse(Set.of()).stream()
                .filter(member -> member.getPackageName().equals(type.getPackageName()))
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(member -> primary(member).ifPresent(stereotype -> {
                    var memberAlias = card(out, indent, module, member.getSimpleName(), role(stereotype),
                            member.getSimpleName());
                    cards.put(member.getName(), memberAlias);
                    family.add(Cell.of(memberAlias));
                }));
        if (family.size() == 1) {
            out.add(indent + INDENT + family.getFirst().left() + " " + Kind.IMPLEMENTS.line("up") + " " + alias);
        } else if (family.size() > 1) {
            var junction = alias + "_j";
            out.add(indent + INDENT + JUNCTION + junction);
            family.forEach(member -> out.add(indent + INDENT + member.left() + " .up. " + junction));
            out.add(indent + INDENT + junction + " " + Kind.IMPLEMENTS.line("up") + " " + alias);
        }
        return new Entry(Cell.of(alias), family);
    }

    /// Writes the card, the type on the first line and its role in guillemets below in a smaller font. Answers the
    /// card's alias.
    private static String card(List<String> out, String indent, ApplicationModule module, String name, String role,
                               String aliasName) {
        var alias = alias(module) + "_" + aliasName.replaceAll("[^A-Za-z0-9]", "_");
        out.add(indent + INDENT + "card \"" + label(name, role) + "\" as " + alias);
        return alias;
    }

    private static String label(String name, String role) {
        return name + "\\n<size:" + SMALL_FONT_SIZE + ">«" + role + "»</size>";
    }

    /// Hidden arrows lay the cells out in a grid with the columns given: right between neighbours in a row, down
    /// from the cards along a cell's bottom to the top of the cell below it. Answers the grid as a cell: its first
    /// cell's left, its first row's last right, the top of the middle cell of its first row, and the cards along
    /// its bottom, those of the last row, and in a grid of several columns those of every box, since a box in a
    /// row above may reach lower.
    private static Cell grid(List<String> out, String indent, List<Cell> cells, int columns) {
        for (var i = 0; i < cells.size(); i++) {
            if ((i + 1) % columns != 0 && i + 1 < cells.size()) {
                out.add(indent + INDENT + cells.get(i).right() + " -[hidden]right-> " + cells.get(i + 1).left());
            }
            if (i + columns < cells.size()) {
                for (var card : cells.get(i).bottom()) {
                    out.add(indent + INDENT + card + " -[hidden]down-> " + cells.get(i + columns).top());
                }
            }
        }
        var lastRow = (cells.size() - 1) / columns * columns;
        var bottom = new ArrayList<String>();
        for (var i = 0; i < cells.size(); i++) {
            if (i >= lastRow || columns > 1 && cells.get(i).isBox()) {
                bottom.addAll(cells.get(i).bottom());
            }
        }
        var firstRow = Math.min(columns, cells.size());
        return new Cell(cells.getFirst().left(), cells.get(firstRow - 1).right(), cells.get((firstRow - 1) / 2).top(),
                bottom);
    }

    /// About as many rows as columns, so a box stays near square whatever it holds.
    private static int square(int cells) {
        return Math.max(1, (int) Math.ceil(Math.sqrt(cells)));
    }

    /// A family in one line, two when it is long.
    private static int columns(List<Cell> family) {
        return Math.min(family.size(), FAMILY_COLUMNS);
    }

    /// The arrows, sorted, one per pair of types, the strongest kind when there are several. From each adapter
    /// type into the application type it depends on; a dependency on the domain from an adapter is not drawn, the
    /// domain sits inside the application and the nesting says it. And from each application type with a card
    /// to the application and domain types it refers to, sideways within the row, down into the domain.
    private void arrows(List<String> out, Map<Ring, List<ApplicationModule>> rings, Map<String, String> cards) {
        var core = rings.getOrDefault(Ring.CORE, List.of());
        var lines = new TreeMap<String, Kind>();
        for (var ring : List.of(Ring.DRIVING, Ring.DRIVEN)) {
            for (var module : rings.getOrDefault(ring, List.of())) {
                module.getDependencies(modules, DependencyDepth.IMMEDIATE).stream()
                        .filter(dependency -> core.contains(dependency.getTargetModule()))
                        .forEach(dependency -> {
                            var source = cardOr(cards, dependency.getSourceType(), alias(module));
                            var target = cardOr(cards, dependency.getTargetType(), alias(dependency.getTargetModule()));
                            lines.merge(source + "\t" + target + "\t" + ring.direction, kind(dependency),
                                    Kind::strongest);
                        });
            }
        }
        var inside = Stream.concat(core.stream(), rings.getOrDefault(Ring.DOMAIN, List.of()).stream())
                .flatMap(HexagonDocumenter::types)
                .map(JavaClass::getName)
                .filter(cards::containsKey)
                .collect(toCollection(TreeSet::new));
        for (var module : core) {
            types(module).filter(type -> cards.containsKey(type.getName())).forEach(source ->
                    source.getDirectDependenciesFromSelf().stream()
                            .map(dependency -> topLevel(dependency.getTargetClass()))
                            .filter(target -> !target.equals(source) && inside.contains(target.getName()))
                            .forEach(target -> {
                                var sideways = target.getPackageName().equals(source.getPackageName());
                                lines.merge(cards.get(source.getName()) + "\t" + cards.get(target.getName()) + "\t"
                                        + (sideways ? "right" : ""), kind(source, target), Kind::strongest);
                            }));
        }
        lines.forEach((key, kind) -> {
            var parts = key.split("\t", -1);
            out.add(parts[0] + " " + kind.line(parts[2]) + " " + parts[1]);
        });
    }

    /// The legend: a discrete grey box in the lower right corner, one row of horizontal samples, every arrow with
    /// its relation written on it. Hidden arrows from the last driven card and from the cards along the bottom of
    /// the application column hang it below everything, to the right. The text legend carries the system's name.
    private void legend(List<String> out, Map<Ring, List<ApplicationModule>> rings, Map<String, String> cards,
                        List<String> anchors) {
        out.add(FRAME + " \"<size:" + SMALL_FONT_SIZE + ">legend</size>\" as legend #line:" + LEGEND_LINE
                + ";text:" + LEGEND_TEXT + " {");
        var samples = new ArrayList<Cell>();
        for (var kind : Kind.values()) {
            var sample = new Cell(kind.alias() + "_from", kind.alias() + "_to", kind.alias() + "_from", List.of());
            out.add(INDENT + "label \" \" as " + sample.left());
            out.add(INDENT + "label \" \" as " + sample.right());
            out.add(INDENT + sample.left() + " " + kind.line("right") + " " + sample.right() + " : " + kind.relation);
            samples.add(sample);
        }
        grid(out, "", samples, samples.size());
        out.add("}");
        var driven = rings.getOrDefault(Ring.DRIVEN, List.of()).stream()
                .flatMap(HexagonDocumenter::types)
                .map(type -> cards.get(type.getName()))
                .filter(Objects::nonNull)
                .toList();
        Stream.concat(driven.isEmpty() ? Stream.empty() : Stream.of(driven.getLast()), anchors.stream())
                .forEach(anchor -> out.add(anchor + " -[hidden]down-> " + samples.getFirst().left()));
        out.add("legend right");
        out.add(INDENT + systemName());
        out.add("endlegend");
    }

    /// Modulith's system name, `@Modulithic(systemName)`, or the last segment of the root package.
    private String systemName() {
        return modules.getSystemName().orElseGet(() -> {
            var root = modules.getRootPackages().getFirst().getName();
            return root.substring(root.lastIndexOf('.') + 1);
        });
    }

    /// The card of the type, or of the top-level type it is nested in, `Recipe` inside `GenerateLoad`; the module's
    /// hexagon when neither has one.
    private static String cardOr(Map<String, String> cards, JavaClass type, String module) {
        return cards.getOrDefault(topLevel(type).getName(), module);
    }

    private static JavaClass topLevel(JavaClass type) {
        var outer = type;
        while (outer.getEnclosingClass().isPresent()) {
            outer = outer.getEnclosingClass().get();
        }
        return outer;
    }

    private static Kind kind(ApplicationModuleDependency dependency) {
        if (dependency.getDependencyType() == DependencyType.USES_COMPONENT) {
            return Kind.USES;
        }
        return dependency.getSourceType().isAssignableTo(dependency.getTargetType().getName())
                ? Kind.IMPLEMENTS
                : Kind.DEPENDS_ON;
    }

    /// Between two types of the hexagon: `implements` when the source is one of the target's kind, `uses` when it
    /// holds one in a field, a record component, `depends on` for a reference in a signature or a body.
    private static Kind kind(JavaClass source, JavaClass target) {
        if (source.isAssignableTo(target.getName())) {
            return Kind.IMPLEMENTS;
        }
        return source.getFields().stream().anyMatch(field -> field.getRawType().equals(target))
                ? Kind.USES
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
    private static Stream<JavaClass> types(ApplicationModule module) {
        return module.getBasePackage().stream()
                .filter(type -> !type.getSimpleName().equals(PACKAGE_INFO))
                .filter(type -> !type.isInnerClass() && !type.isAnonymousClass())
                .sorted(Comparator.comparing(JavaClass::getName));
    }

    /// The names of the types a sealed type of the module permits, which are drawn after it.
    private static Set<String> permittedTypes(ApplicationModule module) {
        return types(module)
                .filter(JavaClass::isSealed)
                .flatMap(type -> type.getPermittedSubclasses().orElse(Set.of()).stream())
                .map(JavaClass::getName)
                .collect(toCollection(TreeSet::new));
    }

    /// The type's stereotype of highest priority, which the catalog defines.
    private Optional<Stereotype> primary(JavaClass type) {
        return ownStereotypes(type).sorted().findFirst();
    }

    /// The stereotypes declared on the type itself. The factory hands a type its package's stereotypes as well,
    /// `@Module` on a `package-info` reaches every type in the package, and those are the package's role, not the
    /// type's. A stereotype is the type's own when one of the annotations the catalog assigns it by is on the type,
    /// directly or as a meta-annotation.
    private Stream<Stereotype> ownStereotypes(JavaClass type) {
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
