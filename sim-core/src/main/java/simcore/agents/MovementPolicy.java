package simcore.agents;

import simcore.world.WorldGrid;

public interface MovementPolicy {
    void move(AgentState agent, WorldGrid world);
}
