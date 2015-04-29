/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.cext;

public class CExtManager {

    private static boolean loaded;
    private static CExtSubsystem subsystem;

    public static synchronized CExtSubsystem getSubsystem() {
        if (!loaded) {
            try {
                final Class<? extends CExtSubsystem> clazz = (Class<? extends CExtSubsystem>)
                        Class.forName("com.oracle.truffle.jruby.interop.cext.CExtSubsystemImpl");
                subsystem = clazz.newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                subsystem = null;
            }
            loaded = true;
        }

        return subsystem;
    }

}
