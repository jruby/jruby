/*
 * JumpException.java - No description
 * Created on 13.01.2002, 22:13:44
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

/**
 * This class should be used for performance reasons if the 
 * Exception don't need a stack trace.
 * 
 * @author jpetersen
 * @version $Revision$
 */
public abstract class JumpException extends RuntimeException {

    /**
     * Constructor for JumpException.
     */
    public JumpException() {
        super();
    }

    /**
     * Constructor for JumpException.
     * @param msg
     */
    public JumpException(String msg) {
        super(msg);
    }

    /** This method don't do anything for performance reasons.
     * 
     * @see Throwable#fillInStackTrace()
     */
    public Throwable fillInStackTrace() {
        return this;
    }    

    protected Throwable originalFillInStackTrace() {
        return super.fillInStackTrace();
    }
}
