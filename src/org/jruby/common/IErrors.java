/*
 * Error.java - description
 * Created on 23.02.2002, 13:47:36
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
package org.jruby.common;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IErrors {
    public static final int ERROR = 0;
	public static final int WARN = 1;
	public static final int WARNING = 2;
	public static final int WARN_UNLESS_E = 3;
	public static final int WARNING_UNLESS_E = 4;

	public static final int SYNTAX_ERROR = 10;
	public static final int COMPILE_ERROR = 11;
}