/*
 * DataInputBridgeStream
 *
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Thomas E Enebo <enebo@acm.org>
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
package org.jruby.util;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author enebo
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DataInputBridgeStream extends InputStream {
    DataInput input;
    
    public DataInputBridgeStream(DataInput input) {
        this.input = input;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        return input.readUnsignedByte();
    }

}
