/*
 * RubyGlobalEntry.java - No description
 * Created on 16. September 2001, 17:26
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
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

package org.jruby.runtime;

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class RubyGlobalEntry {
    private Ruby ruby = null;

    private String name = null;
    private RubyObject internalData = null;

    private GetterMethod getter = null;
    private SetterMethod setter = null;

    public static UndefAccessor undefMethods = new UndefAccessor();
    public static ValueAccessor valueMethods = new ValueAccessor();
    public static ReadonlySetter readonlySetter = new ReadonlySetter();

    public RubyGlobalEntry(Ruby ruby, String name) {
        this.ruby = ruby;
        this.name = name;

        this.getter = undefMethods;
        this.setter = undefMethods;
    }

    public Ruby getRuby() {
        return ruby;
    }

    public String getName() {
        return name;
    }

    public RubyObject getInternalData() {
        return internalData;
    }

    public void setInternalData(RubyObject internalData) {
        this.internalData = internalData;
    }

    /** Getter for property getter.
     * @return Value of property getter.
     */
    private GetterMethod getGetter() {
        return getter;
    }

    /** Setter for property getter.
     * @param getter New value of property getter.
     */
    public void setGetter(GetterMethod getter) {
        this.getter = getter;
    }

    /** Getter for property setter.
     * @return Value of property setter.
     */
    private SetterMethod getSetter() {
        return setter;
    }

    /** Setter for property setter.
     * @param setter New value of property setter.
     */
    public void setSetter(SetterMethod setter) {
        this.setter = setter;
    }

    /** rb_gvar_get
     *
     */
    public RubyObject get() {
        return getGetter().get(ruby, this);
    }

    /** rb_gvar_set
     *
     */
    public RubyObject set(RubyObject value) {
        if (ruby.getSafeLevel() >= 4) {
            throw new RubySecurityException(ruby, "Insecure: can't change global variable value");
        }

        getSetter().set(ruby, this, value);

        return value;
    }

    public boolean isDefined() {
        return getter != undefMethods && setter != undefMethods;
    }
    
    public void undefine() {
        this.getter = undefMethods;
        this.setter = undefMethods;
    }

    public interface GetterMethod {
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry);
    }

    public interface SetterMethod {
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value);
    }

    private static class UndefAccessor implements GetterMethod, SetterMethod {
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            if (entry.getRuby().isVerbose()) {
            	// entry.getRuby().warn("global variable '" + id + "' not initialized");
            }

            return entry.ruby.getNil();
        }

        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            entry.setInternalData(value);

            entry.setGetter(valueMethods);
            entry.setSetter(valueMethods);
        }
    }

    private static class ValueAccessor implements GetterMethod, SetterMethod {
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            return entry.getInternalData();
        }

        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            entry.setInternalData(value);
        }
    }

    public static class AliasAccessor implements GetterMethod, SetterMethod {
        private RubyGlobalEntry originalEntry;
        
        public AliasAccessor(RubyGlobalEntry originalEntry) {
            this.originalEntry = originalEntry;
        }

        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            return originalEntry.get();
        }

        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            originalEntry.set(value);
        }
    }

    private static class ReadonlySetter implements SetterMethod {
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            throw new NameError(ruby, "can't set variable " + entry.getName());
        }
    }
}
