/*
 * JavaFieldWriter.java - No description
 * Created on 21.01.2002, 15:08:51
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaFieldWriter implements Callback {
    private Field field;

    public JavaFieldWriter(Field field) {
        this.field = field;
    }
    
    public int getArity() {
        return 1;
    }

    /**
     * @see Callback#execute(RubyObject, RubyObject[], Ruby)
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        try {
			field.set(((RubyJavaObject)recv).getValue(), JavaUtil.convertRubyToJava(recv.getRuntime(), args[0], field.getType()));
        	return recv;
        } catch (IllegalAccessException iaExcptn) {
            throw new RubySecurityException(recv.getRuntime(), iaExcptn.getMessage());
        }
    }
}