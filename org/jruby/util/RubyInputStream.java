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

import java.io.*;
import java.io.*;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyInputStream extends FilterInputStream {
    private BufferedReader reader;

    public RubyInputStream(InputStream in) {
        super(in);

        reader = new BufferedReader(new InputStreamReader(in));
    }

    public String gets(String separator) throws IOException {
        int c = reader.read();

        if (c == -1) {
            return null;
        }

        char[] sep = separator.toCharArray();
        int sepLen = sep.length;

        StringBuffer sb = new StringBuffer();

        LineLoop : while (true) {
            while (c != sep[0] && c != -1) {
                sb.append((char) c);
                c = reader.read();
            }
            for (int i = 0; i < sepLen; i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != sep[i]) {
                    continue LineLoop;
                }
                sb.append((char) c);
                if (i < (sepLen - 1)) {
                    c = reader.read();
                }
            }
            break;
        }
        return sb.toString();
    }
}