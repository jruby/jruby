/*
 * Constants.java - No description
 * Created on 02. November 2001, 01:25
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Charles O Nutter
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.runtime;



public interface Constants {
    public static final String RUBY_MAJOR_VERSION = "${version.ruby.major}";
    public static final String RUBY_VERSION = "${version.ruby}";
    public static final String COMPILE_DATE = "${build.date}";
    public static final String VERSION = "${version.jruby}";
    public static final String PLATFORM = "java";

    public static final int MARSHAL_MAJOR = 4;
    public static final int MARSHAL_MINOR = 10;
}