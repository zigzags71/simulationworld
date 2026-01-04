package simcore.naming;

import simcore.config.SimConfig;
import simcore.util.BinningUtil;
import simcore.util.MathUtil;

/**
 * Generates deterministic name ids for agents based on their spawn location and ids.
 */
public final class NameGenerator {
    private NameGenerator() {
    }

    public static NameRecord generateForSpawn(long agentId, long worldSeed, int tileIndex, float foodValue, float hazardValue) {
        int foodBin = BinningUtil.bin01(foodValue, SimConfig.FIELD_BIN_COUNT);
        int hazardBin = BinningUtil.bin01(hazardValue, SimConfig.FIELD_BIN_COUNT);
        int firstPick = hashToInt(worldSeed, agentId, tileIndex);
        int surnamePick = hashToInt(agentId, worldSeed, tileIndex + 31);
        int firstNameId = NameRegistry.chooseFirstNameId(foodBin, hazardBin, firstPick);
        int surnameId = NameRegistry.chooseSurnameId(surnamePick);
        int cultureId = 0;
        return new NameRecord(firstNameId, surnameId, cultureId);
    }

    private static int hashToInt(long a, long b, long c) {
        long mixed = a;
        mixed ^= rotateLeft(b, 13);
        mixed ^= rotateLeft(c, 27);
        mixed *= 0x9E3779B97F4A7C15L;
        return Math.abs((int) (mixed ^ (mixed >>> 32)));
    }

    private static long rotateLeft(long value, int distance) {
        return (value << distance) | (value >>> (64 - distance));
    }

    public record NameRecord(int firstNameId, int surnameId, int cultureId) {
        public String fullName() {
            return NameRegistry.resolveFirstName(firstNameId) + " " + NameRegistry.resolveSurname(surnameId);
        }

        public String cultureName() {
            return CultureNameRegistry.resolveCultureName(MathUtil.clamp(cultureId, 0, CultureNameRegistry.getCultureCount() - 1));
        }
    }
}
