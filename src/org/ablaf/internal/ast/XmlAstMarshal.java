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

import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.ablaf.ast.IAstDecoder;
import org.ablaf.ast.IAstEncoder;
import org.ablaf.ast.IAstMarshal;
import org.jruby.ast.Node;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class XmlAstMarshal implements IAstMarshal {
    private Map delegates;

    public XmlAstMarshal() {
        this(null);
    }

    public XmlAstMarshal(Map delegates) {
        this.delegates = delegates;
    }

    public IAstDecoder openDecoder(InputStream input) {
        final XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(input));
        return new IAstDecoder() {
            public Node readNode() {
                return (Node) decoder.readObject();
            }

            public void close() {
                decoder.close();
            }
        };
    }

    public IAstEncoder openEncoder(OutputStream output) {
        final XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(output));

        if (delegates != null) {
            Iterator iter = delegates.entrySet().iterator();
            for (int i = 0, size = delegates.size(); i < size; i++) {
                Map.Entry entry = (Map.Entry) iter.next();
                encoder.setPersistenceDelegate((Class) entry.getKey(), (PersistenceDelegate) entry.getValue());
            }
        }

        return new IAstEncoder() {
            public void writeNode(Node node) {
                encoder.writeObject(node);
            }

            public void close() {
                encoder.close();
            }
        };
    }
}
