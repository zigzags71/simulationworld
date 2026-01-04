package simcore.naming;

import simcore.config.SimConfig;

/**
 * Registry of first and surnames referenced by agents via integer ids.
 */
public final class NameRegistry {
    private static final String[][][] FIRST_NAME_POOLS = new String[][][]{
            {
                    {"Aria", "Alden", "Ash"},
                    {"Bryn", "Basil", "Bea"},
                    {"Cove", "Cira", "Cedar"},
                    {"Dara", "Dune", "Dax"},
                    {"Elsa", "Ember", "Eon"}
            },
            {
                    {"Faye", "Finn", "Fenn"},
                    {"Gail", "Garr", "Grove"},
                    {"Hale", "Hera", "Holt"},
                    {"Isla", "Ivar", "Indi"},
                    {"Jade", "Joss", "Jori"}
            },
            {
                    {"Kael", "Kora", "Keen"},
                    {"Lark", "Leni", "Loch"},
                    {"Mara", "Mica", "Moss"},
                    {"Nia", "Noll", "Nyx"},
                    {"Oren", "Orla", "Ode"}
            },
            {
                    {"Perr", "Pia", "Pine"},
                    {"Quin", "Quill", "Quest"},
                    {"Rhea", "Rin", "Rook"},
                    {"Sage", "Sia", "Sol"},
                    {"Tala", "Thorne", "Tess"}
            },
            {
                    {"Uma", "Ursa", "Ulric"},
                    {"Vail", "Vera", "Venn"},
                    {"Willow", "Wren", "Wynn"},
                    {"Xena", "Xylo", "Xan"},
                    {"Yara", "Yule", "Yves"}
            }
    };

    private static final String[] SURNAMES = new String[]{
            "Stone", "Vale", "Reed", "North", "Ember", "Hale", "Frost", "Delta", "Strand", "Hollow",
            "Ridge", "Brook", "Wilder", "Ashen", "Cinder", "Grove", "Dawn", "Night", "Cloud", "Crest"
    };

    private static final int[][] FIRST_NAME_OFFSETS = new int[SimConfig.FIELD_BIN_COUNT][SimConfig.FIELD_BIN_COUNT];
    private static final String[] FIRST_NAME_FLAT;

    static {
        int offset = 0;
        int flatSize = 0;
        for (String[][] poolRow : FIRST_NAME_POOLS) {
            for (String[] pool : poolRow) {
                flatSize += pool.length;
            }
        }
        FIRST_NAME_FLAT = new String[flatSize];
        for (int food = 0; food < SimConfig.FIELD_BIN_COUNT; food++) {
            for (int hazard = 0; hazard < SimConfig.FIELD_BIN_COUNT; hazard++) {
                String[] pool = FIRST_NAME_POOLS[food][hazard % FIRST_NAME_POOLS[food].length];
                FIRST_NAME_OFFSETS[food][hazard] = offset;
                System.arraycopy(pool, 0, FIRST_NAME_FLAT, offset, pool.length);
                offset += pool.length;
            }
        }
    }

    private NameRegistry() {
    }

    public static int chooseFirstNameId(int foodBin, int hazardBin, int pick) {
        int safeFood = clampBin(foodBin);
        int safeHazard = clampBin(hazardBin);
        String[] pool = FIRST_NAME_POOLS[safeFood][safeHazard % FIRST_NAME_POOLS[safeFood].length];
        int index = Math.floorMod(pick, pool.length);
        return FIRST_NAME_OFFSETS[safeFood][safeHazard] + index;
    }

    public static String resolveFirstName(int id) {
        if (id < 0 || id >= FIRST_NAME_FLAT.length) {
            return "-";
        }
        return FIRST_NAME_FLAT[id];
    }

    public static int chooseSurnameId(int pick) {
        return Math.floorMod(pick, SURNAMES.length);
    }

    public static String resolveSurname(int id) {
        if (id < 0 || id >= SURNAMES.length) {
            return "-";
        }
        return SURNAMES[id];
    }

    public static int getSurnameCount() {
        return SURNAMES.length;
    }

    public static int getFirstNameCount() {
        return FIRST_NAME_FLAT.length;
    }

    private static int clampBin(int bin) {
        if (bin < 0) {
            return 0;
        }
        return Math.min(bin, SimConfig.FIELD_BIN_COUNT - 1);
    }
}
