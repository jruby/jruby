/*
 * SearchMethodResult.java - No description
 * Created on 05. Oktober 2001, 11:58
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

package org.jruby.util;

import org.jruby.*;
import org.jruby.interpreter.nodes.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class SearchMethodResult {
    private Node body;
    private RubyModule origin;
    
    public SearchMethodResult(Node body, RubyModule origin) {
        this.body = body;
        this.origin = origin;
    }
    
    /** Getter for property body.
     * @return Value of property body.
     */
    public Node getBody() {
        return body;
    }
    
    /** Getter for property origin.
     * @return Value of property origin.
     */
    public RubyModule getOrigin() {
        return origin;
    }
}