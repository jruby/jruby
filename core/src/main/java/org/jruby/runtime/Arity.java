/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The arity of a method is the number of arguments it takes.
 */
public final class Arity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<Integer, Arity> arities = new HashMap<Integer, Arity>();
    private final int value;
    
    public final static Arity NO_ARGUMENTS = newArity(0);
    public final static Arity ONE_ARGUMENT = newArity(1);
    public final static Arity TWO_ARGUMENTS = newArity(2);
    public final static Arity THREE_ARGUMENTS = newArity(3);
    public final static Arity OPTIONAL = newArity(-1);
    public final static Arity ONE_REQUIRED = newArity(-2);
    public final static Arity TWO_REQUIRED = newArity(-3);
    public final static Arity THREE_REQUIRED = newArity(-4);

    private Arity(int value) {
        this.value = value;
    }
    
    private static Arity createArity(int required, int optional, boolean rest) {
        return createArity((optional > 0 || rest) ? -(required + 1) : required);
    }

    public static Arity createArity(int value) {
        switch (value) {
        case -4:
            return THREE_REQUIRED;
        case -3:
            return TWO_REQUIRED;
        case -2:
            return ONE_REQUIRED;
        case -1:
            return OPTIONAL;
        case 0:
            return NO_ARGUMENTS;
        case 1:
            return ONE_ARGUMENT;
        case 2:
            return TWO_ARGUMENTS;
        case 3:
            return THREE_ARGUMENTS;
        }
        return newArity(value);
    }
    
    public static Arity fromAnnotation(JRubyMethod anno) {
        return createArity(anno.required(), anno.optional(), anno.rest());
    }
    
    public static Arity fromAnnotation(JRubyMethod anno, int required) {
        return createArity(required, anno.optional(), anno.rest());
    }
    
    public static Arity fromAnnotation(JRubyMethod anno, Class[] parameterTypes, boolean isStatic) {
        int required;
        if (anno.optional() == 0 && !anno.rest() && anno.required() == 0) {
            // try count specific args to determine required
            int i = parameterTypes.length;
            if (isStatic) i--;
            if (parameterTypes.length > 0) {
                if (parameterTypes[0] == ThreadContext.class) i--;
                if (parameterTypes[parameterTypes.length - 1] == Block.class) i--;
            }

            required = i;
        } else {
            required = anno.required();
        }
        
        return createArity(required, anno.optional(), anno.rest());
    }
    
    private static Arity newArity(int value) {
        Arity result;
        synchronized (arities) {
            result = arities.get(value);
            if (result == null) {
                result = new Arity(value);
                arities.put(value, result);
            }
        }
        return result;
    }

    public static Arity fixed(int arity) {
        assert arity >= 0;
        return createArity(arity);
    }

    public static Arity optional() {
        return OPTIONAL;
    }

    public static Arity required(int minimum) {
        assert minimum >= 0;
        return createArity(-(1 + minimum));
    }

    public static Arity noArguments() {
        return NO_ARGUMENTS;
    }

    public static Arity singleArgument() {
        return ONE_ARGUMENT;
    }

    public static Arity twoArguments() {
        return TWO_ARGUMENTS;
    }

    public int getValue() {
        return value;
    }

    public void checkArity(Ruby runtime, IRubyObject[] args) {
		  checkArity(runtime, args.length);
    }

    public void checkArity(Ruby runtime, int length) {
        if (isFixed()) {
            if (length != required()) {
                throw runtime.newArgumentError("wrong number of arguments (" + length + " for " + required() + ")");
            }
        } else {
            if (length < required()) {
                throw runtime.newArgumentError("wrong number of arguments (" + length + " for " + required() + ")");
            }
        }
    }

    public boolean isFixed() {
        return value >= 0;
    }

    public int required() {
        return value < 0 ? -(1 + value) : value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return isFixed() ? "Fixed" + required() : "Opt";
    }

    // Some helper functions:

    public static int checkArgumentCount(Ruby runtime, IRubyObject[] args, int min, int max) {
        return checkArgumentCount(runtime, args.length, min, max);
    }

    public static int checkArgumentCount(ThreadContext context, IRubyObject[] args, int min, int max) {
        return checkArgumentCount(context, args.length, min, max);
    }

    public static int checkArgumentCount(Ruby runtime, String name, IRubyObject[] args, int min, int max) {
        return checkArgumentCount(runtime, name, args.length, min, max);
    }

    public static int checkArgumentCount(Ruby runtime, int length, int min, int max) {
        raiseArgumentError(runtime, length, min, max);

        return length;
    }

    public static int checkArgumentCount(ThreadContext context, int length, int min, int max) {
        raiseArgumentError(context, length, min, max);

        return length;
    }

    public static int checkArgumentCount(Ruby runtime, int length, int min, int max, boolean hasKwargs) {
        raiseArgumentError(runtime, length, min, max, hasKwargs);

        return length;
    }

    public static int checkArgumentCount(Ruby runtime, String name, int length, int min, int max) {
        raiseArgumentError(runtime, name, length, min, max);

        return length;
    }

    public static int checkArgumentCount(Ruby runtime, String name, int length, int min, int max, boolean hasKwargs) {
        raiseArgumentError(runtime, name, length, min, max, hasKwargs);

        return length;
    }
    
    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error    
    public static void raiseArgumentError(Ruby runtime, IRubyObject[] args, int min, int max) {
        raiseArgumentError(runtime, args.length, min, max);
    }

    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error
    public static void raiseArgumentError(Ruby runtime, int length, int min, int max) {
        if (length < min) throw runtime.newArgumentError(length, min);
        if (max > -1 && length > max) throw runtime.newArgumentError(length, max);
    }

    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error
    public static void raiseArgumentError(ThreadContext context, int length, int min, int max) {
        if (length < min) throw context.runtime.newArgumentError(length, min);
        if (max > -1 && length > max) throw context.runtime.newArgumentError(length, max);
    }

    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error
    public static void raiseArgumentError(Ruby runtime, int length, int min, int max, boolean hasKwargs) {
        if (length < min) throw runtime.newArgumentError(length, min);
        if (max > -1 && length > max) {
            if (hasKwargs  && length == max + 1) {
                // we have an extra arg, but kwargs active; let it fall through to assignment
                return;
            }
            throw runtime.newArgumentError(length, max);
        }
    }

    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error
    public static void raiseArgumentError(Ruby runtime, String name, int length, int min, int max) {
        if (length < min) throw runtime.newArgumentError(name, length, min);
        if (max > -1 && length > max) throw runtime.newArgumentError(name, length, max);
    }

    // FIXME: JRuby 2/next should change this name since it only sometimes raises an error
    public static void raiseArgumentError(Ruby runtime, String name, int length, int min, int max, boolean hasKwargs) {
        if (length < min) throw runtime.newArgumentError(name, length, min);
        if (max > -1 && length > max) {
            if (hasKwargs  && length == max + 1) {
                // we have an extra arg, but kwargs active; let it fall through to assignment
                return;
            }
            throw runtime.newArgumentError(name, length, max);
        }
    }

    /**
     */
    public static IRubyObject[] scanArgs(Ruby runtime, IRubyObject[] args, int required, int optional) {
        int total = required+optional;
        int real = checkArgumentCount(runtime, args,required,total);
        IRubyObject[] narr = new IRubyObject[total];
        System.arraycopy(args,0,narr,0,real);
        for(int i=real; i<total; i++) {
            narr[i] = runtime.getNil();
        }
        return narr;
    }
}
