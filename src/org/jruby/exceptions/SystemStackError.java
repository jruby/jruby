/*
 * SystemStackError.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2004 Charles O Nutter
 * Charles O Nutter <headius@headius.com>
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

package org.jruby.exceptions;

import org.jruby.Ruby;

/**
 *
 * @author  cnutter
 */
public class SystemStackError extends RaiseException {

    /**
     * Constructs a <code>SystemStackException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SystemStackError(Ruby ruby, String msg) {
        super(ruby, ruby.getExceptions().getSystemStackError(), msg);
    }
}
