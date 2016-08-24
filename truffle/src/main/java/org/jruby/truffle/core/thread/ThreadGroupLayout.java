package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface ThreadGroupLayout extends BasicObjectLayout {

    DynamicObjectFactory createThreadGroupShape(
        DynamicObject logicalClass,
        DynamicObject metaClass);

    DynamicObject createThreadGroup(
        DynamicObjectFactory factory,
        boolean enclosed);

    boolean getEnclosed(DynamicObject object);

    void setEnclosed(DynamicObject object, boolean value);

}
