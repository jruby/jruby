/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.ablaf.internal.ast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.ablaf.ast.IAstDecoder;
import org.ablaf.ast.IAstEncoder;
import org.ablaf.ast.IAstMarshal;
import org.jruby.ast.Node;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class SerializationAstMarshal implements IAstMarshal {
    /**
     * @see org.ablaf.ast.IAstMarshal#openEncoder(OutputStream)
     */
    public IAstEncoder openEncoder(OutputStream output) throws IOException {
        final ObjectOutputStream oout = new ObjectOutputStream(output);
        return new IAstEncoder() {
            public void writeNode(Node node) throws IOException {
            	oout.writeObject(node);
            }
        
            public void close() throws IOException {
            	oout.close();
            }
        };
    }

    /**
     * @see org.ablaf.ast.IAstMarshal#openDecoder(InputStream)
     */
    public IAstDecoder openDecoder(InputStream input) throws IOException {
    	final ObjectInputStream oin = new ObjectInputStream(input);
    	return new IAstDecoder() {
			public Node readNode() throws IOException {
				try {
					return (Node) oin.readObject();
				} catch (ClassNotFoundException e) {
					throw (IOException) new IOException("Missing AST class: " + e.getMessage()).initCause(e);
				}
			}

			public void close() throws IOException {
				oin.close();
			}
		};
    }
}
