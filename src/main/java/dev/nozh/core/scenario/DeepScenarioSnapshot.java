package dev.nozh.core.scenario;

public final class DeepScenarioSnapshot {

    public final String dimensionKey;

    public final double blocksPlacedPerMin;
    public final double blocksBrokenPerMin;

    public final int hostileMobsNearby;

    public DeepScenarioSnapshot(
            String dimensionKey,
            double blocksPlacedPerMin,
            double blocksBrokenPerMin,
            int hostileMobsNearby
    ) {
        this.dimensionKey = dimensionKey;
        this.blocksPlacedPerMin = blocksPlacedPerMin;
        this.blocksBrokenPerMin = blocksBrokenPerMin;
        this.hostileMobsNearby = hostileMobsNearby;
    }
}
