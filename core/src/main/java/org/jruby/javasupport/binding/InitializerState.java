package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
public class InitializerState {
    public final Map<String, AssignedName> staticNames;
    public final Map<String, AssignedName> instanceNames;
    public final Map<String, NamedInstaller> staticCallbacks = new HashMap<String, NamedInstaller>();
    public final Map<String, NamedInstaller> instanceCallbacks = new HashMap<String, NamedInstaller>();
    public final List<ConstantField> constantFields = new ArrayList<ConstantField>();

    public InitializerState(Ruby runtime, Class superclass) {
        if (superclass == null) {
            staticNames = new HashMap<String, AssignedName>();
            instanceNames = new HashMap<String, AssignedName>();
        } else {
            JavaClass superJavaClass = JavaClass.get(runtime, superclass);
            staticNames = new HashMap<String, AssignedName>(superJavaClass.initializer.staticAssignedNames);
            instanceNames = new HashMap<String, AssignedName>(superJavaClass.initializer.instanceAssignedNames);
        }
        staticNames.putAll(STATIC_RESERVED_NAMES);
        instanceNames.putAll(INSTANCE_RESERVED_NAMES);
    }

    // TODO: other reserved names?
    private static final Map<String, AssignedName> RESERVED_NAMES = new HashMap<String, AssignedName>();
    static {
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
    }
    public static final Map<String, AssignedName> STATIC_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }
    public static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        // only possible for "getClass" to be an instance method in Java
        INSTANCE_RESERVED_NAMES.put("class", new AssignedName("class", Priority.RESERVED));
        // "initialize" has meaning only for an instance (as opposed to a class)
        INSTANCE_RESERVED_NAMES.put("initialize", new AssignedName("initialize", Priority.RESERVED));
    }
}
