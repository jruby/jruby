/*
 * RubyMethod.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby;

import org.jruby.core.*;
import org.jruby.nodes.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/** 
 * The RubyMethod class represents a Method object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
public class RubyMethod extends RubyObject {
    private RubyClass receiverClass;
    private RubyObject receiver;
    private RubyId methodId;
    private Node bodyNode;
    private RubyClass originalClass;
    private RubyId originalId;

    public static class Nil extends RubyMethod {
        public Nil(Ruby ruby) {
            super(ruby, ruby.getClasses().getNilClass());
        }
    }

    public RubyMethod(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /** Create the Method class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby ruby) {
        Callback arity =
            new ReflectionCallbackMethod(RubyMethod.class, "arity");
        Callback call =
            new ReflectionCallbackMethod(RubyMethod.class, "call", true);

        RubyClass methodClass =
            ruby.defineClass("Method", ruby.getClasses().getObjectClass());

        methodClass.defineMethod("arity", arity);
        methodClass.defineMethod("[]", call);
        methodClass.defineMethod("call", call);

        return methodClass;
    }

    /**
     * Gets the bodyNode
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(Node bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the methodId
     * @return Returns a RubyId
     */
    public RubyId getMethodId() {
        return methodId;
    }

    /**
     * Sets the methodId
     * @param methodId The methodId to set
     */
    public void setMethodId(RubyId methodId) {
        this.methodId = methodId;
    }

    /**
     * Gets the originalClass
     * @return Returns a RubyClass
     */
    public RubyClass getOriginalClass() {
        return originalClass;
    }

    /**
     * Sets the originalClass
     * @param originalClass The originalClass to set
     */
    public void setOriginalClass(RubyClass originalClass) {
        this.originalClass = originalClass;
    }

    /**
     * Gets the originalId
     * @return Returns a RubyId
     */
    public RubyId getOriginalId() {
        return originalId;
    }

    /**
     * Sets the originalId
     * @param originalId The originalId to set
     */
    public void setOriginalId(RubyId originalId) {
        this.originalId = originalId;
    }

    /**
     * Gets the receiver
     * @return Returns a RubyObject
     */
    public RubyObject getReceiver() {
        return receiver;
    }

    /**
     * Sets the receiver
     * @param receiver The receiver to set
     */
    public void setReceiver(RubyObject receiver) {
        this.receiver = receiver;
    }

    /**
     * Gets the receiverClass
     * @return Returns a RubyClass
     */
    public RubyClass getReceiverClass() {
        return receiverClass;
    }

    /**
     * Sets the receiverClass
     * @param receiverClass The receiverClass to set
     */
    public void setReceiverClass(RubyClass receiverClass) {
        this.receiverClass = receiverClass;
    }

    /** Call the method.
     * 
     */
    public RubyObject call(RubyObject[] args) {
        getRuby().getIter().push(
            getRuby().isBlockGiven() ? RubyIter.ITER_PRE : RubyIter.ITER_NOT);
        RubyObject result =
            getReceiverClass().call0(
                getReceiver(),
                getMethodId(),
                new RubyPointer(args),
                getBodyNode(),
                false);
        getRuby().getIter().pop();

        return result;
    }
    
    /**Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    public RubyFixnum arity() {
        switch (bodyNode.getType()) {
            case Constants.NODE_CFUNC:
                return RubyFixnum.newFixnum(getRuby(), -1);
            case Constants.NODE_ZSUPER:
                return RubyFixnum.newFixnum(getRuby(), -1);
            case Constants.NODE_ATTRSET:
                return RubyFixnum.one(getRuby());
            case Constants.NODE_IVAR:
                return RubyFixnum.zero(getRuby());
            default:
                Node body = bodyNode.getNextNode(); /* skip NODE_SCOPE */
                if (body instanceof BlockNode) {
                    body = body.getHeadNode();
                }
                if (body == null) {
                    return RubyFixnum.zero(getRuby());
                }
                int n = body.getCount();
                if (body.getOptNode() != null || body.getRest() != -1) {
                    n = -n-1;
                }
                return RubyFixnum.newFixnum(getRuby(), n);
        }
    }
}