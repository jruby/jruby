/*
 * Copyright (C) 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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
package org.jruby.parser;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyParserPool {
    private static RubyParserPool instance = new RubyParserPool();

    private List pool;

    /**
     * Constructor for RubyParserPool.
     */
    private RubyParserPool() {
        pool = new LinkedList();
    }

    public static RubyParserPool getInstance() {
        return instance;
    }

    public DefaultRubyParser borrowParser() {
        synchronized (pool) {
            Iterator iter = pool.iterator();
            while (iter.hasNext()) {
                DefaultRubyParser parser = (DefaultRubyParser) ((Reference) iter.next()).get();
                iter.remove();
                if (parser != null) {
                    return parser;
                }
            }
        }
        return new DefaultRubyParser();
    }

    public void returnParser(DefaultRubyParser parser) {
        synchronized (pool) {
            pool.add(new SoftReference(parser));
        }
    }
}