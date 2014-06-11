/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.core.*;

/**
 * Represents a constant read from some object and cached, with the assumption that the object it
 * was read from is unmodified. If that assumption does not hold the read is uninitialized. If the
 * class of the receiver changes we also uninitialize.
 */
@NodeInfo(shortName = "cached-read-constant")
public class CachedReadConstantNode extends ReadConstantChainNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;

    private final Object value;

    private final boolean hasBoolean;
    private final boolean booleanValue;

    private final boolean hasIntegerFixnum;
    private final int integerFixnumValue;

    private final boolean hasLongFixnum;
    private final long longFixnumValue;

    private final boolean hasFloat;
    private final double floatValue;

    @Child protected ReadConstantChainNode next;

    public CachedReadConstantNode(RubyClass expectedClass, Object value, ReadConstantChainNode next) {
        this.expectedClass = expectedClass;
        unmodifiedAssumption = expectedClass.getUnmodifiedAssumption();

        this.value = value;

        /*
         * We could do this lazily as needed, but I'm sure the compiler will appreciate the fact
         * that these fields are all final.
         */

        if (value instanceof Boolean) {
            hasBoolean = true;
            booleanValue = (boolean) value;

            hasIntegerFixnum = false;
            integerFixnumValue = -1;

            hasLongFixnum = false;
            longFixnumValue = -1;

            hasFloat = false;
            floatValue = -1;
        } else if (value instanceof Integer) {
            hasBoolean = false;
            booleanValue = false;

            hasIntegerFixnum = true;
            integerFixnumValue = (int) value;

            hasLongFixnum = true;
            longFixnumValue = (int) value;

            hasFloat = true;
            floatValue = (int) value;
        } else if (value instanceof Long) {
            hasBoolean = false;
            booleanValue = false;

            if ((long) value <= Integer.MAX_VALUE) {
                hasIntegerFixnum = true;
                integerFixnumValue = (int) (long) value;
            } else {
                hasIntegerFixnum = false;
                integerFixnumValue = -1;
            }

            hasLongFixnum = true;
            longFixnumValue = (long) value;

            hasFloat = true;
            floatValue = (long) value;
        } else if (value instanceof Double) {
            hasBoolean = false;
            booleanValue = false;

            hasIntegerFixnum = false;
            integerFixnumValue = -1;

            hasLongFixnum = false;
            longFixnumValue = -1;

            hasFloat = true;
            floatValue = (double) value;
        } else {
            hasBoolean = false;
            booleanValue = false;

            hasIntegerFixnum = false;
            integerFixnumValue = -1;

            hasLongFixnum = false;
            longFixnumValue = -1;

            hasFloat = false;
            floatValue = -1;
        }

        this.next = next;
    }

    @Override
    public Object execute(RubyBasicObject receiver) {
        // TODO(CS): not sure trying next on invalid assumption is right...

        if (receiver.getRubyClass() == expectedClass && unmodifiedAssumption.isValid()) {
            return value;
        } else {
            return next.execute(receiver);
        }
    }

    @Override
    public boolean executeBoolean(RubyBasicObject receiver) throws UnexpectedResultException {
        if (hasBoolean && receiver.getRubyClass() == expectedClass && unmodifiedAssumption.isValid()) {
            return booleanValue;
        } else {
            return next.executeBoolean(receiver);
        }
    }

    @Override
    public int executeIntegerFixnum(RubyBasicObject receiver) throws UnexpectedResultException {
        if (hasIntegerFixnum && receiver.getRubyClass() == expectedClass && unmodifiedAssumption.isValid()) {
            return integerFixnumValue;
        } else {
            return next.executeIntegerFixnum(receiver);
        }
    }

    @Override
    public long executeLongFixnum(RubyBasicObject receiver) throws UnexpectedResultException {
        if (hasLongFixnum && receiver.getRubyClass() == expectedClass && unmodifiedAssumption.isValid()) {
            return longFixnumValue;
        } else {
            return next.executeLongFixnum(receiver);
        }
    }

    @Override
    public double executeFloat(RubyBasicObject receiver) throws UnexpectedResultException {
        if (hasFloat && receiver.getRubyClass() == expectedClass && unmodifiedAssumption.isValid()) {
            return integerFixnumValue;
        } else {
            return next.executeFloat(receiver);
        }
    }

}
