package org.jruby;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a registry of all bit flags we use on JRuby objects.
 *
 * In order to maximally use our bit flags and prevent overlap between ancestors and dependents,
 * this class registers flags on a first-come, first-served basis using previous flags registered
 * for ancestor classes as a base line for new flags in a descendant.
 *
 * Because of the first-come, first-served nature, the most general types will need to register
 * their flags first. This guarantees all bit flags from the progenitor on down will be packed
 * tightly while avoiding overlaps.
 */
public class FlagRegistry {
    private final Map<Class, Integer> currentShift = new HashMap<>();
    private final Map<Class, BitSet> registry = new HashMap<>();

    /**
     * Register a new flag for the given class.
     *
     * The bit index for the new flag will be calculated at runtime, by walking parent classes
     * and looking for previously-registered flags. Ancestors should register all their flags
     * before descendants (which means they should not be registered in a static initializer
     * unless the parent is known to have fully run its own static initializers).
     *
     * @param klass the class for which to register a new flag
     * @return an integer with the new flag bit set
     */
    public synchronized int newFlag(Class klass) {
        Class currentKlass = klass;
        Integer shift = null;
        while (currentKlass != null &&
                (shift = currentShift.get(currentKlass)) == null) {
            currentKlass = currentKlass.getSuperclass();
        }
        if (shift == null) shift = 0;

        BitSet flags = registry.get(klass);
        if (flags == null) {
            flags = new BitSet();
            registry.put(klass, flags);
        }
        flags.set(shift);

        assert flagsAreValid(klass, shift);

        currentShift.put(klass, shift + 1);

        return 1 << shift++;
    }

    public synchronized void printFlags() {
        System.out.println(registry);
    }

    private boolean flagsAreValid(Class klass, int bitIndex) {
        BitSet gathered = new BitSet();
        Class currentKlass = klass;
        while (currentKlass != null) {
            BitSet flags = registry.get(klass);
            if (flags != null) {
                if (flags.intersects(gathered)) {
                    throw new AssertionError(klass.getName() + " uses flag " + bitIndex + " that overlaps with " + currentKlass);
                }
                gathered.and(flags);
            }
            currentKlass = currentKlass.getSuperclass();
        }
        return true;
    }
}
