/*
 * Copyright (C) 2004 Charles O Nutter
 * Charles O Nutter (headius@headius.com)
 * 
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.util;

import org.ablaf.ast.IAstDecoder;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.ast.util.RubyAstMarshal;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.load.Library;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Loading of pre-parsed, serialized, Ruby scripts that are built into JRuby.
 */
public class PreparsedScript implements Library {
    private URL loc;

    public PreparsedScript(URL loc) {
        this.loc = loc;
    }

    public void load(Ruby runtime) {
        runtime.loadNode("preparsed", getNode(runtime), false);
    }

    private Node getNode(Ruby runtime) {
    	
        InputStream in = null;
        try {
        	in = loc.openStream();
        } catch (IOException ioe) {}
        
        if (in == null) {
            throw new IOError(runtime, "Resource not found: " + loc);
        }
        in = new BufferedInputStream(in);
        IAstDecoder decoder = RubyAstMarshal.getInstance().openDecoder(in);
        Node result = decoder.readNode();
        decoder.close();
        return result;
    }
}
