package simcore.naming;

/**
 * Registry of culture names referenced by id.
 */
public final class CultureNameRegistry {
    private static final String[] CULTURE_NAMES = new String[]{
            "Origin", "Highland", "Lowland", "Mariner", "Frontier"
    };

    private CultureNameRegistry() {
    }

    public static String resolveCultureName(int id) {
        if (id < 0 || id >= CULTURE_NAMES.length) {
            return CULTURE_NAMES[0];
        }
        return CULTURE_NAMES[id];
    }

    public static int getCultureCount() {
        return CULTURE_NAMES.length;
    }
}
