/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.ablaf.parser.IParser;
import org.ablaf.parser.IParserPool;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyParserPool implements IParserPool {
    private static IParserPool instance = new RubyParserPool();

    private List pool;

    /**
     * Constructor for RubyParserPool.
     */
    private RubyParserPool() {
        pool = new LinkedList();
    }

    public static IParserPool getInstance() {
        return instance;
    }

    /**
     * @see org.ablaf.parser.IParserPool#borrowParser()
     */
    public IParser borrowParser() {
        synchronized (pool) {
            Iterator iter = pool.iterator();
            while (iter.hasNext()) {
                IParser parser = (IParser) ((Reference) iter.next()).get();
                if (parser != null) {
                    return parser;
                } else {
                    iter.remove();
                }
            }
        }
        return new DefaultRubyParser();
    }

    /**
     * @see org.ablaf.parser.IParserPool#returnParser(IParser)
     */
    public void returnParser(IParser parser) {
        synchronized (pool) {
            pool.add(new SoftReference(parser));
        }
    }
}