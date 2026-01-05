package simcore.agents;

public class AgentTickMetrics {
    private int population;
    private int deathsThisTick;
    private int totalDeaths;
    private int signalsEmittedThisTick;
    private int followMovesThisTick;
    private int activeSignalsCount;
    private float totalEnergy;
    private float totalHunger;
    private float totalStress;
    private float totalHazard;
    private float totalPredictionError;

    public void accumulate(AgentState agent, float hazard) {
        totalEnergy += agent.getEnergy();
        totalHunger += agent.getHunger();
        totalStress += agent.getStress();
        totalHazard += hazard;
        totalPredictionError += agent.getPredictionError();
        population++;
    }

    public void markDeath() {
        deathsThisTick++;
    }

    public int getPopulation() {
        return population;
    }

    public int getDeathsThisTick() {
        return deathsThisTick;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public void incrementSignalsEmitted() {
        signalsEmittedThisTick++;
    }

    public void incrementFollowMoves() {
        followMovesThisTick++;
    }

    public int getSignalsEmittedThisTick() {
        return signalsEmittedThisTick;
    }

    public int getFollowMovesThisTick() {
        return followMovesThisTick;
    }

    public void setActiveSignalsCount(int activeSignalsCount) {
        this.activeSignalsCount = activeSignalsCount;
    }

    public int getActiveSignalsCount() {
        return activeSignalsCount;
    }

    public float getMeanEnergy() {
        return population == 0 ? 0f : totalEnergy / population;
    }

    public float getMeanHunger() {
        return population == 0 ? 0f : totalHunger / population;
    }

    public float getMeanStress() {
        return population == 0 ? 0f : totalStress / population;
    }

    public float getMeanHazard() {
        return population == 0 ? 0f : totalHazard / population;
    }

    public float getMeanPredictionError() {
        return population == 0 ? 0f : totalPredictionError / population;
    }
}
