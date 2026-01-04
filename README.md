# Simulation World

Simulation World is a Maven multi-module project providing a headless simulation core and a JavaFX viewer. The viewer renders a 1024x1024 procedural world with thousands of moving agents. The simulation runs on a background thread at 20 ticks per second while the UI renders up to 180 frames per second.

## Modules
- **sim-core**: Pure simulation logic with no UI dependencies.
- **ui-viewer**: JavaFX visualization that consumes snapshots from the simulation.
- **app**: Launcher module for convenience and future headless entry points.

## Prerequisites
- JDK 17+
- Maven 3.8+
- JavaFX SDK is fetched automatically via Maven.

## Building
```
mvn clean install
```

## Running
You can run the JavaFX viewer using either the UI module or the launcher module:
```
# From the repository root
mvn -pl ui-viewer javafx:run
# or
mvn -pl app exec:java
```

To override the world seed, pass a JVM property:
```
mvn -pl ui-viewer javafx:run -Dsim.seed=9001
```

## IDE Setup
1. Import the root `pom.xml` as a Maven project (e.g., IntelliJ IDEA).
2. Run the Maven "install" lifecycle once to download JavaFX.
3. Use the Maven run configuration `javafx:run` on the `ui-viewer` module (main class `uiviewer.app.MainApp`).

## Controls
- **Camera**: scroll to zoom on the cursor, right/middle-drag to pan.
- **Hover**: hover updates tile food, hazard, crowding, and agent counts.
- **Selection**: left-click locks the tile selection; clicking a tile with agents picks the closest agent for inspection.
- **Overlays**: Food, Hazard, Crowding, and Culture coloring; toggle "Show Agents" to hide or show agent sprites.
- **Simulation**: Use Start to resume ticking, Pause/Resume to toggle; Generate regenerates the world from the sliders/seed; Reset restores defaults.
- **Brush**: pick Food/Hazard/Eraser and Small/Medium/Large sizes; left-click paints live field changes on the hovered tile.
