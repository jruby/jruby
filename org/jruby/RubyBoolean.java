/*
 * RubyBoolean.java - No description
 * Created on 09. Juli 2001, 21:38
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

package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyBoolean extends RubyObject {
    private boolean value;

    public RubyBoolean(Ruby ruby, boolean value) {
        super(ruby, null, // Don't initialize with class
        false); // Don't put in object space
        this.value = value;
    }

    public Class getJavaClass() {
        return Boolean.TYPE;
    }

    public RubyClass getInternalClass() {
        return value ? getRuntime().getClasses().getTrueClass() : getRuntime().getClasses().getFalseClass();
    }

    public boolean isTrue() {
        return value;
    }

    public boolean isFalse() {
        return !value;
    }

    public static RubyClass createFalseClass(Ruby ruby) {
        RubyClass falseClass = ruby.defineClass("FalseClass", ruby.getClasses().getObjectClass());

        falseClass.defineMethod("type", CallbackFactory.getMethod(RubyBoolean.class, "type"));

        ruby.defineGlobalConstant("FALSE", ruby.getFalse());

        return falseClass;
    }

    public static RubyClass createTrueClass(Ruby ruby) {
        RubyClass trueClass = ruby.defineClass("TrueClass", ruby.getClasses().getObjectClass());

        trueClass.defineMethod("type", CallbackFactory.getMethod(RubyBoolean.class, "type"));

        ruby.defineGlobalConstant("TRUE", ruby.getTrue());

        return trueClass;
    }

    public static RubyBoolean newBoolean(Ruby ruby, boolean value) {
        if (value) {
            return ruby.getTrue();
        } else {
            return ruby.getFalse();
        }
    }

        /** false_type
     *  true_type
     *
     */
    public RubyClass type() {
        return getInternalClass();
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(isTrue() ? 'T' : 'F');
    }
}

