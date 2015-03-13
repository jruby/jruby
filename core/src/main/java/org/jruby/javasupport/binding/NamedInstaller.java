package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.runtime.Visibility;

/**
* Created by headius on 2/26/15.
*/
public abstract class NamedInstaller {
    static final int STATIC_FIELD = 1;
    static final int STATIC_METHOD = 2;
    static final int INSTANCE_FIELD = 3;
    static final int INSTANCE_METHOD = 4;

    final String name;
    final int type;

    Visibility visibility = Visibility.PUBLIC;

    public NamedInstaller(String name, int type) {
        this.name = name;
        this.type = type;
    }

    abstract void install(RubyModule proxy);

    // small hack to save a cast later on
    boolean hasLocalMethod() { return true; }

    boolean isPublic() { return visibility == Visibility.PUBLIC; }

    //boolean isProtected() { return visibility == Visibility.PROTECTED; }

}
