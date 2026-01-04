package simcore.agents;

import java.util.Objects;

public final class AgentId {
    private final long id;

    public AgentId(long id) {
        this.id = id;
    }

    public long value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentId agentId = (AgentId) o;
        return id == agentId.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgentId{" + id + '}';
    }
}
