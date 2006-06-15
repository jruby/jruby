/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The arity of a method is the number of arguments it takes.
 */
public final class Arity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map arities = new HashMap();
    private final int value;

    private Arity(int value) {
        this.value = value;
    }

    public static Arity createArity(int value) {
        Integer integerValue = new Integer(value);
        Arity result;
        synchronized (arities) {
            result = (Arity) arities.get(integerValue);
            if (result == null) {
                result = new Arity(value);
                arities.put(integerValue, result);
            }
        }
        return result;
    }

    public static Arity fixed(int arity) {
        assert arity >= 0;
        return createArity(arity);
    }

    public static Arity optional() {
        return createArity(-1);
    }

    public static Arity required(int minimum) {
        assert minimum >= 0;
        return createArity(-(1 + minimum));
    }

    public static Arity noArguments() {
        return createArity(0);
    }

    public static Arity singleArgument() {
        return createArity(1);
    }

	public static Arity twoArguments() {
		return createArity(2);
	}

    public int getValue() {
        return value;
    }

    public void checkArity(IRuby runtime, IRubyObject[] args) {
        if (isFixed()) {
            if (args.length != required()) {
                throw runtime.newArgumentError("wrong number of arguments(" + args.length + " for " + required() + ")");
            }
        } else {
            if (args.length < required()) {
                throw runtime.newArgumentError("wrong number of arguments(" + args.length + " for " + required() + ")");
            }
        }
    }

    public boolean isFixed() {
        return value >= 0;
    }

    private int required() {
        if (value < 0) {
            return -(1 + value);
        }
		return value;
    }

    public boolean equals(Object other) {
        return this == other;
    }

    public int hashCode() {
        return value;
    }
}
