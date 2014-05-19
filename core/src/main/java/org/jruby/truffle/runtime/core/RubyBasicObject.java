/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;
import java.util.Map.Entry;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.lookup.*;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;
import org.jruby.util.cli.Options;

/**
 * Represents the Ruby {@code BasicObject} class - the root of the Ruby class hierarchy.
 */
public class RubyBasicObject extends ObjectStorage {

    @CompilationFinal protected RubyClass rubyClass;
    protected RubyClass rubySingletonClass;

    protected LookupNode lookupNode;

    protected long objectID = -1;

    public boolean hasPrivateLayout = false;

    private static final boolean objectSpaceEnabled = Options.OBJECTSPACE_ENABLED.load();

    public RubyBasicObject(RubyClass rubyClass) {
        super(rubyClass != null ? rubyClass.getObjectLayoutForInstances() : ObjectLayout.EMPTY);

        if (rubyClass != null) {
            unsafeSetRubyClass(rubyClass);

            if (objectSpaceEnabled) {
                rubyClass.getContext().getObjectSpaceManager().add(this);
            }
        }
    }

    public LookupNode getLookupNode() {
        return lookupNode;
    }

    public RubyClass getRubyClass() {
        assert rubyClass != null;
        return rubyClass;
    }

    public void setInstanceVariable(String name, Object value) {
        CompilerAsserts.neverPartOfCompilation();

        updateLayoutToMatchClass();

        setField(name, value);

        if (rubyClass.getObjectLayoutForInstances() != objectLayout) {
            rubyClass.setObjectLayoutForInstances(objectLayout);
        }
    }

    public RubyClass getSingletonClass() {
        if (rubySingletonClass == null) {
            final CoreLibrary coreLibrary = getRubyClass().getContext().getCoreLibrary();

            // TODO(CS): some of these reference comparisons should probably check for subclasses as well

            if (getRubyClass() == coreLibrary.getNilClass() || getRubyClass() == coreLibrary.getTrueClass() || getRubyClass() == coreLibrary.getFalseClass()) {
                rubySingletonClass = getRubyClass();
            } else if (getRubyClass() == coreLibrary.getFixnumClass() || getRubyClass() == coreLibrary.getFloatClass() || getRubyClass() == coreLibrary.getSymbolClass()) {
                throw new RaiseException(coreLibrary.typeError("can't define singleton"));
            } else {
                rubySingletonClass = new RubyClass(rubyClass.getParentModule(), rubyClass, String.format("#<Class:#<%s:0x%x>>", rubyClass.getName(), getObjectID()), true);
                lookupNode = new LookupFork(rubySingletonClass, rubyClass);
            }
        }

        return rubySingletonClass;
    }

    public long getObjectID() {
        if (objectID == -1) {
            objectID = rubyClass.getContext().getNextObjectID();
        }

        return objectID;
    }

    protected void setInstanceVariables(Map<String, Object> instanceVariables) {
        assert instanceVariables != null;
        updateLayoutToMatchClass();
        setFields(instanceVariables);
    }

    public void updateLayoutToMatchClass() {
        if (objectLayout != rubyClass.getObjectLayoutForInstances()) {
            changeLayout(rubyClass.getObjectLayoutForInstances());
        }
    }

    public void switchToPrivateLayout() {
        final Map<String, Object> instanceVariables = getFields();

        hasPrivateLayout = true;
        objectLayout = ObjectLayout.EMPTY;

        for (Entry<String, Object> entry : instanceVariables.entrySet()) {
            objectLayout = objectLayout.withNewVariable(entry.getKey(), entry.getValue().getClass());
        }

        setInstanceVariables(instanceVariables);
    }

    public void extend(RubyModule module) {
        getSingletonClass().include(module);
    }

    public Object send(String name, RubyProc block, Object... args) {
        final RubyMethod method = getLookupNode().lookupMethod(name);

        if (method == null || method.isUndefined()) {
            throw new RaiseException(getRubyClass().getContext().getCoreLibrary().noMethodError(name, toString()));
        }

        return method.call(this, block, args);
    }

    public void unsafeSetRubyClass(RubyClass newRubyClass) {
        assert rubyClass == null;

        rubyClass = newRubyClass;
        lookupNode = rubyClass;
    }

    public Object getInstanceVariable(String name) {
        return RubyNilClass.instanceOrNil(getField(name));
    }

    public boolean hasPrivateLayout() {
        return hasPrivateLayout;
    }
}
