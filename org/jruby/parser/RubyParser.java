/*
 * RubyParser.java - No description
 * Created on 04. Oktober 2001, 23:23
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.parser;

import org.jruby.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public interface RubyParser {
    NODE compileString(String f, RubyObject s, int line);

    /**
     *  Compiles the given Java String "s"
     *
     *@param  f     Description of Parameter
     *@param  s     Description of Parameter
     *@param  len   Description of Parameter
     *@param  line  Description of Parameter
     *@return       Description of the Returned Value
     */
    NODE compileJavaString(String f, String s, int len, int line);
    
    /**
     *  Compiles the given file "file"
     *
     *@param  f      Description of Parameter
     *@param  file   Description of Parameter
     *@param  start  Description of Parameter
     *@return        Description of the Returned Value
     */
    NODE compileFile(String f, RubyObject file, int start);
}