/*
 * IObject.java - description
 * Created on 12.03.2002, 01:26:26
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
package org.jruby.runtime.classes;

/** Represents an object in Ruby. All the methods defined by this interface
 * are avaiable for all Ruby objects.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IObject {

    /** Returns the type (class) of this object.
     * 
     */
    public IClass getType();

    /** Returns if the object is an instance of type. 
     * 
     * type could be a class, module, true, false or nil.
     * 
     */
    public boolean isInstanceOf(IObject type);

    /** Returns if the object is a kind of type. 
     * 
     */
    public boolean isKindOf(IModule type);

    /** Returns a clone of this object.
     * 
     */
    public IObject getClone();

    /** Returns a duplicate of this object.
     * 
     */
    public IObject getDuplicate();

    /** Returns an object id.
     * 
     * <dl>
     * <dt>false</dt><dd>0</dd>
     * <dt>true</dt><dd>2</dd>
     * <dt>nil</dt><dd>4</dd>
     * <dt>Fixnum objects: </dt><dd>1, 3, 5, 7, ...</dd>
     * <dt>Other objects: </dt><dd>6, 8, 10, 12, ...</dd>
     * </dl>
     */
    public long getId();

    /** Returns a hash code.
     * 
     */
    public long getHashcode();

    /** Returns if this object is frozen.
     * 
     */
    public boolean isFrozen();

    /** Set the frozen state of this object.
     * 
     * @return this object.
     */
    public IObject setFrozen(boolean frozen);

    /** Returns the taint state of this object.
     * 
     */
    public boolean isTaint();

    /** Set the taint state of this object.
     * 
     * @return this object.
     */
    public IObject setTaint(boolean taint);

    /** Compares two objects. (==)
     * 
     */
    public boolean equal(IObject other);

    /** Compares two objects. (===)
     * 
     */
    public boolean caseEqual(IObject other);

    /** Compares two objects. (equal?)
     * 
     * @return true if the objects are the same, false otherwise.
     */
    public boolean identityEqual(IObject other);

    /** Compares two objects. (eql?)
     * 
     * @return true if the objects have the same hashcode, false otherwise.
     * 
     */
    public boolean hashEqual(IObject other);
    
    /** Convert the object into a String.
     * 
     */
    public String toString();

    /** Convert the object into an Array.
     * 
     */
    public IArray toArray();

    /** Send a message to an object.
     * 
     * @return this.
     */
    public IObject send(ISymbol message, IObject[] args);
}