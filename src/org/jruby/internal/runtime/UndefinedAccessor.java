/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class UndefinedAccessor implements IAccessor {
    private final Ruby runtime;
    private final GlobalVariable globalVariable;
    private final String notInitializedWarning;

    /**
     * Constructor for UndefinedAccessor.
     */
    public UndefinedAccessor(Ruby runtime, GlobalVariable globalVariable, String name) {
        Asserts.notNull(runtime);
        Asserts.notNull(globalVariable);
        Asserts.notNull(name);

        this.runtime = runtime;
        this.globalVariable = globalVariable;
        this.notInitializedWarning = "global variable `" + name + "' not initialized";
    }

    /**
     * @see org.jruby.runtime.IAccessor#getValue()
     */
    public IRubyObject getValue() {
        runtime.getWarnings().warning(notInitializedWarning);
        return runtime.getNil();
    }

    /**
     * @see org.jruby.runtime.IAccessor#setValue(IRubyObject)
     */
    public IRubyObject setValue(IRubyObject newValue) {
        Asserts.notNull(newValue);
        globalVariable.setAccessor(new ValueAccessor(newValue));
        return newValue;
    }
}