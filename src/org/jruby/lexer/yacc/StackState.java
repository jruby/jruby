/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.lexer.yacc;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class StackState implements Cloneable {
    private long stack = 0;

    public void reset() {
        reset(0);
    }

    public void reset(long backup) {
        stack = backup;
    }

    public long begin() {
        long old = stack;
        stack <<= 1;
        stack |= 1;
        return old;
    }

    public void end() {
        stack >>= 1;
    }

    public void stop() {
        stack <<= 1;
    }

    public void restart() {
        stack |= (stack & 1) << 1;
        stack >>= 1;
    }
    
    public boolean isInState() {
        return (stack & 1) != 0;
    }
}