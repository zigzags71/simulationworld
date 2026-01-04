package simcore.agents;

import simcore.world.WorldGrid;

public interface NourishmentPolicy {
    void applyNutrition(AgentState agent, WorldGrid world);
}
