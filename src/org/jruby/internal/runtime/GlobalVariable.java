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
import org.jruby.RubyProc;
import org.jruby.runtime.IAccessor;

import java.util.ArrayList;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class GlobalVariable {
    private IAccessor accessor;
    private ArrayList traces = null;
    private boolean tracing;

    public GlobalVariable(IAccessor accessor) {
        this.accessor = accessor;
    }
    
    public static GlobalVariable newUndefined(Ruby runtime, String name) {
        GlobalVariable variable = new GlobalVariable(null);
        variable.setAccessor(new UndefinedAccessor(runtime, variable, name));
        return variable;
    }

    public IAccessor getAccessor() {
        return accessor;
    }

    public ArrayList getTraces() {
        return traces;
    }

    public void addTrace(RubyProc trace) {
        if (traces == null) {
            traces = new ArrayList();
        }
        traces.add(trace);
    }

    public void setAccessor(IAccessor accessor) {
        this.accessor = accessor;
    }
    public boolean isTracing() {
        return tracing;
    }

}