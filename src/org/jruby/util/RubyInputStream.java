/*
 * RubyInputStream.java - No description
 * Created on 12.01.2002, 17:32:05
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
package org.jruby.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;


/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyInputStream extends PushbackInputStream {
    public RubyInputStream(InputStream in) {
        super(new BufferedInputStream(in));
    }

    public String gets(String separatorString) throws IOException {
        if (separatorString == null) {
            return getsEntireStream();
        }
        final char[] separator = separatorString.toCharArray();

        int c = read();
        if (c == -1) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        LineLoop : while (true) {
            while (c != separator[0] && c != -1) {
                buffer.append((char) c);
                c = read();
            }
            for (int i = 0; i < separator.length; i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != separator[i]) {
                    continue LineLoop;
                }
                buffer.append((char) c);
                if (i < (separator.length - 1)) {
                    c = read();
                }
            }
            break;
        }
        return buffer.toString();
    }

    private String getsEntireStream() throws IOException {
        StringBuffer result = new StringBuffer();
        int c;
        while ((c = read()) != -1) {
            result.append((char) c);
        }
        return result.toString();
    }
}