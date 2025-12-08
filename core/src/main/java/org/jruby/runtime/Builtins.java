package org.jruby.runtime;

import java.lang.invoke.SwitchPoint;
import java.util.EnumMap;
import java.util.HashMap;

public class Builtins {
    // bits for class
    private static final int _FIRST_CLASS = 1;
    public static final int INTEGER = _FIRST_CLASS;
    private static final int _LAST_CLASS = INTEGER + 1;

    // offsets for method
    private static final int _FIRST_METHOD = 0;
    public static final int EQUAL = _FIRST_METHOD;
    private static final int _LAST_METHOD = EQUAL + 1;

    public static final EnumMap<ClassIndex, Integer> classIds = new EnumMap<>(ClassIndex.class);
    public static final HashMap<String, Integer> methodIds = new HashMap<>();

    static {
        classIds.put(ClassIndex.INTEGER, INTEGER);

        methodIds.put("==", EQUAL);
    }

    public static short[] allocate() {
        return new short[_LAST_METHOD];
    }

    public static boolean checkIntegerEquals(ThreadContext context) {
        return (context.builtinBits[EQUAL] & INTEGER) == 0;
    }

    public static void invalidateBuiltin(short[] bits, ClassIndex classIndex, String method) {
        if (classIds.get(classIndex) instanceof Integer classId && methodIds.get(method) instanceof Integer methodId) {
            bits[methodId] |= classId;
        }
    }
}
