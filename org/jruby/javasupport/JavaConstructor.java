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
import sun.security.krb5.internal.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class JavaConstructor implements Callback {
    private Constructor[] constructors = null;

    public JavaConstructor(Constructor[] constructors) {
        this.constructors = constructors;
    }

    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        LinkedList executeConstructors = new LinkedList(Arrays.asList(constructors));

        int argsLength = args != null ? args.length : 0;

        // remove constructors with wrong parameter count.
        Iterator iter = executeConstructors.iterator();
        while (iter.hasNext()) {
            Constructor constructor = (Constructor) iter.next();
            if (constructor.getParameterTypes().length != argsLength) {
                iter.remove();
            }
        }

        // remove constructors with wrong parameter types.
        iter = executeConstructors.iterator();
        while (iter.hasNext()) {
            Constructor constructor = (Constructor) iter.next();
            for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                if (!JavaUtil.isCompatible(args[i], constructor.getParameterTypes()[i])) {
                    iter.remove();
                    break;
                }
            }
        }

        if (executeConstructors.isEmpty()) {
            throw new RubyArgumentException(ruby, "wrong arguments.");
        }

        // take the first constructor.
        Constructor constructor = (Constructor) executeConstructors.getFirst();

        Object[] newArgs = new Object[argsLength];

        for (int i = 0; i < argsLength; i++) {
            newArgs[i] =
                JavaUtil.convertRubyToJava(ruby, args[i], constructor.getParameterTypes()[i]);
        }

        try {
            Object javaValue = constructor.newInstance(newArgs);
            RubyJavaObject javaObject =
                new RubyJavaObject(ruby, (RubyClass) recv, javaValue);
            javaObject.callInit(args);
            return javaObject;
        } catch (IllegalAccessException ex) {
            throw new RaiseException(ruby, "RuntimeError", ex.getMessage());
        } catch (InstantiationException ex) {
            throw new RaiseException(ruby, "RuntimeError", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new RaiseException(ruby, "RuntimeError", ex.getMessage());
        } catch (InvocationTargetException ex) {
            throw new RaiseException(ruby, "RuntimeError", ex.getMessage());
        }
    }
}