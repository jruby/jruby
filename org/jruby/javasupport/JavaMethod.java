/*
 * JavaMethod.java - No description
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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaMethod implements Callback {
    private Method[] methods = null;
    private boolean callSuper = false;
    private boolean singleton = false;

    public JavaMethod(Method[] methods, boolean callSuper) {
        this(methods, callSuper, false);
    }

    public JavaMethod(Method[] methods, boolean callSuper, boolean singleton) {
        this.methods = methods;
        this.callSuper = callSuper;
        this.singleton = singleton;
    }

    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        LinkedList executeMethods = new LinkedList(Arrays.asList(methods));
        
        int argsLength = args != null ? args.length : 0;
        
        // remove mehods with wrong parameter count.
        Iterator iter = executeMethods.iterator();
        while (iter.hasNext()) {
            Method method = (Method)iter.next();
            if (method.getParameterTypes().length != argsLength) {
                iter.remove();
            }
        }
        
        // remove mehods with wrong parameter types.
        iter = executeMethods.iterator();
        while (iter.hasNext()) {
            Method method = (Method)iter.next();
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                if (!JavaUtil.isCompatible(args[i], method.getParameterTypes()[i])) {
                    iter.remove();
                    break;
                }
            }
        }
        
        if (executeMethods.isEmpty()) {
            if (callSuper) {
            	ruby.getRuntime().callSuper(args);
            } else {
            	throw new RubyArgumentException(ruby, "wrong argument count or types.");
            }
        }
        
        // take the first method.
        Method method = (Method)executeMethods.getFirst();
        
        Object[] newArgs = new Object[argsLength];
        
        for (int i = 0; i < argsLength; i++) {
            newArgs[i] = JavaUtil.convertRubyToJava(ruby, args[i], method.getParameterTypes()[i]);
        }

        try {
            Object receiver = !singleton ? ((RubyJavaObject)recv).getValue() : null;
            
            return JavaUtil.convertJavaToRuby(ruby, method.invoke(receiver, newArgs));
        } catch (Exception excptn) {
            throw new RaiseException(ruby, "RuntimeError", excptn.getMessage());
        }
    }
}