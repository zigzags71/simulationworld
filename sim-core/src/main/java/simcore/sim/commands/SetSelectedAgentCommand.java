package simcore.sim.commands;

public class SetSelectedAgentCommand {
    private final long agentId;

    public SetSelectedAgentCommand(long agentId) {
        this.agentId = agentId;
    }

    public long getAgentId() {
        return agentId;
    }
}
