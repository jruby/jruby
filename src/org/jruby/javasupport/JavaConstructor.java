/*
 * JavaConstructor.java - No description
 * Created on 21. September 2001, 15:03
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaConstructor implements Callback {
    private Constructor[] constructors = null;

    public JavaConstructor(Constructor[] constructors) {
        this.constructors = constructors;
    }

    public Arity getArity() {
        return Arity.optional();
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        Constructor constructor = findMatchingConstructor(args);

        if (constructor == null) {
            throw new ArgumentError(recv.getRuntime(), "wrong arguments.");
        }

        int argsLength = args != null ? args.length : 0;
        Object[] newArgs = new Object[argsLength];
        for (int i = 0; i < argsLength; i++) {
            newArgs[i] =
                JavaUtil.convertRubyToJava(recv.getRuntime(), args[i], constructor.getParameterTypes()[i]);
        }

        try {
            Object javaValue = constructor.newInstance(newArgs);
            RubyJavaObject javaObject =
                new RubyJavaObject(recv.getRuntime(), (RubyClass) recv, javaValue);
            javaObject.callInit(args);
            return javaObject;

        } catch (IllegalAccessException ex) {
            throw new RaiseException(recv.getRuntime(), "RuntimeError", ex.getMessage());
        } catch (InstantiationException ex) {
            throw new RaiseException(recv.getRuntime(), "RuntimeError", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new RaiseException(recv.getRuntime(), "RuntimeError", ex.getMessage());
        } catch (InvocationTargetException ex) {
            recv.getRuntime().getJavaSupport().handleNativeException((Exception)ex.getTargetException());

            return recv.getRuntime().getNil();
        }
    }

    private Constructor findMatchingConstructor(IRubyObject[] args) {
        ArrayList executeConstructors = new ArrayList(constructors.length);

        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            if (hasMatchingArguments(constructor, args)) {
                executeConstructors.add(constructor);
            }
        }

        if (executeConstructors.isEmpty()) {
            return null;
        }
        return (Constructor) executeConstructors.get(0);
    }

    private static boolean hasMatchingArgumentCount(Constructor constructor, int expected) {
        return (constructor.getParameterTypes().length == expected);
    }

    private static boolean hasMatchingArguments(Constructor constructor, IRubyObject[] args) {
        int expectedLength = (args != null ? args.length : 0);
        if (! hasMatchingArgumentCount(constructor, expectedLength)) {
            return false;
        }
        Class[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (! JavaUtil.isCompatible(args[i], parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }
}

