/*
 * IObjectTestCase.java - description
 * Created on 12.03.2002, 13:33:59
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.runtime.classes.test;

import org.jruby.runtime.classes.*;

import junit.framework.*;

/** A TestCase for the @link{IObject} class.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class IObjectTestCase extends TestCase {

    /**
     * Constructor for IObjectTestCase.
     * @param name
     */
    public IObjectTestCase(String name) {
        super(name);
    }

    protected abstract IObject getObject();
    protected abstract IObjectCreator getObjectCreator();

    /** Tests the @link{IObject#isInstanceOf(IObject)} method.
     * 
     */
    public void testIsInstanceOf() {
        assertTrue("anObject instance_of? anObject.type()", getObject().isInstanceOf(getObject().getType()));

        assertTrue("anObject instance_of? true", getObject().isInstanceOf(getObjectCreator().getTrue()));
        assertTrue("anObject instance_of? false", !getObject().isInstanceOf(getObjectCreator().getFalse()));
        assertTrue("anObject instance_of? nil", !getObject().isInstanceOf(getObjectCreator().getNil()));

        assertTrue("true instance_of? true", getObjectCreator().getTrue().isInstanceOf(getObjectCreator().getTrue()));
        assertTrue("true instance_of? false", !getObjectCreator().getTrue().isInstanceOf(getObjectCreator().getFalse()));
        assertTrue("true instance_of? nil", !getObjectCreator().getTrue().isInstanceOf(getObjectCreator().getNil()));

        assertTrue("false instance_of? true", !getObjectCreator().getFalse().isInstanceOf(getObjectCreator().getTrue()));
        assertTrue("false instance_of? false", getObjectCreator().getFalse().isInstanceOf(getObjectCreator().getFalse()));
        assertTrue("false instance_of? nil", !getObjectCreator().getFalse().isInstanceOf(getObjectCreator().getNil()));

        assertTrue("nil instance_of? true", !getObjectCreator().getNil().isInstanceOf(getObjectCreator().getTrue()));
        assertTrue("nil instance_of? false", getObjectCreator().getNil().isInstanceOf(getObjectCreator().getFalse()));
        assertTrue("nil instance_of? nil", getObjectCreator().getNil().isInstanceOf(getObjectCreator().getNil()));
    }

    /** Tests the @link{IObject#getId()} method.
     * 
     */
    public void testGetId() {
        assertEquals("false.id == 0", getObjectCreator().getFalse().getId(), 0);
        assertEquals("true.id == 2", getObjectCreator().getTrue().getId(), 2);
        assertEquals("nil.id == 4", getObjectCreator().getTrue().getId(), 4);

        assertTrue("anObject.id % 2 == 0", getObject().getId() % 2  == 0);
    }
}