/*
 * RubyVarmap.java - No description
 * Created on 10. September 2001, 17:54
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

import org.jruby.Ruby;
import org.jruby.RubyObject;


/**
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyVarmap {
    public String name = null;
    private RubyObject value = null;
    public RubyVarmap next = null;

    /** Creates new RubyVarmap */
    public RubyVarmap(String name, RubyObject val, RubyVarmap next) {
        this.name = name;
        this.value = val;
        this.next = next;
    }

    /** Getter for property id.
     * @return Value of property id.
     */
    public String getName() {
        return name;
    }

    /** Setter for property id.
     * @param id New value of property id.
     */
    public void setName(String id) {
        this.name = id;
    }

    /** Getter for property next.
     * @return Value of property next.
     */
    public RubyVarmap getNext() {
        return next;
    }

    /** Setter for property next.
     * @param next New value of property next.
     */
    public void setNext(RubyVarmap next) {
        this.next = next;
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public RubyObject getVal() {
        return value;
    }

    /** Setter for property value.
     * @param val New value of property value.
     */
    public void setVal(RubyObject val) {
        this.value = val;
    }

    /** rb_dvar_ref
     *
     */
    public RubyObject getRef(Ruby ruby, String name) {
        if (name.equals(this.name)) {
            return getVal();
        } else if (next != null) {
            return next.getRef(ruby, name);
        }
        return ruby.getNil();
    }

    public RubyVarmap assignVarmapInternal(String id, RubyObject value, boolean current) {
        int n = 0;
        RubyVarmap tmpMap = this;

        while (tmpMap != null) {
            if (current && tmpMap.getName() == null) {
                n++;
                if (n == 2) {
                    break;
                }
            }
            if (id.equals(tmpMap.getName())) {
                tmpMap.setVal(value);
                return this;
            }
            tmpMap = tmpMap.getNext();
        }
        tmpMap = new RubyVarmap(id, value, this.getNext());
        this.setNext(tmpMap);
        return this;
    }
}
