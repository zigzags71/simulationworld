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
```bash
mvn clean install
```

## Running
You can run the JavaFX viewer using either the UI module or the launcher module:

```bash
# From the repository root
mvn -pl ui-viewer javafx:run
# or
mvn -pl app exec:java
```

To override the world seed, pass a JVM property:
```bash
mvn -pl ui-viewer javafx:run -Dsim.seed=9001
```

## IDE Setup
1. Import the root `pom.xml` as a Maven project (e.g., IntelliJ IDEA).
2. Ensure the Maven projects view shows `sim-core`, `ui-viewer`, and `app` modules.
3. Run configuration: set main class to `uiviewer.app.MainApp` (UI module) or `app.Launcher` (app module). VM options for JavaFX are handled by the Maven plugin.

## Controls
- Hover over the canvas to inspect tile food, hazard, and crowding fields.
- Click to select the agent located on the hovered tile and view its inspector details.
- Use the overlay toggles (Food, Hazard, Crowding, Culture) to change the tile coloring mode.
