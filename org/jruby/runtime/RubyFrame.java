/*
 * RubyFrame.java - No description
 * Created on 10. September 2001, 17:54
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.runtime;

import java.util.*;

import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyFrame {
    public static final int FRAME_ALLOCA = 0;
    public static final int FRAME_MALLOC = 1;

    private RubyObject self = null;
    private List args = null;
    private String lastFunc = null;
    private RubyModule lastClass = null;
    private Namespace namespace = null;
    private RubyFrame prev = null;
    private RubyFrame tmp = null;
    private String file = null;
    private int line = 0;
    private int iter = 0;
    private int flags = 0;

    private Ruby ruby = null;

    public RubyFrame(Ruby ruby) {
        this.ruby = ruby;
    }

    public RubyFrame(
        Ruby ruby,
        RubyObject self,
        List args,
        String lastFunc,
        RubyModule lastClass,
        Namespace namespace,
        RubyFrame prev,
        RubyFrame tmp,
        String file,
        int line,
        int iter,
        int flags) {
        this(ruby);

        this.self = self;
        this.args = args;
        this.lastFunc = lastFunc;
        this.lastClass = lastClass;
        this.namespace = namespace;
        this.prev = prev;
        this.tmp = tmp;
        this.file = file;
        this.line = line;
        this.iter = iter;
        this.flags = flags;
    }

    public RubyFrame(RubyFrame frame) {
        this(
            frame.ruby,
            frame.self,
            frame.args,
            frame.lastFunc,
            frame.lastClass,
            frame.namespace,
            frame.prev,
            frame.tmp,
            frame.file,
            frame.line,
            frame.iter,
            frame.flags);
    }

    /** Getter for property args.
     * @return Value of property args.
     */
    public List getArgs() {
        return args;
    }

    /** Setter for property args.
     * @param args New value of property args.
     */
    public void setArgs(List args) {
        this.args = args;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    /** Getter for property file.
     * @return Value of property file.
     */
    public String getFile() {
        return file;
    }

    /** Setter for property file.
     * @param file New value of property file.
     */
    public void setFile(String file) {
        this.file = file;
    }

    /** Getter for property flags.
     * @return Value of property flags.
     */
    public int getFlags() {
        return flags;
    }

    /** Setter for property flags.
     * @param flags New value of property flags.
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /** Getter for property iter.
     * @return Value of property iter.
     */
    public int getIter() {
        return iter;
    }

    /** Setter for property iter.
     * @param iter New value of property iter.
     */
    public void setIter(int iter) {
        this.iter = iter;
    }

    /** Getter for property lastClass.
     * @return Value of property lastClass.
     */
    public RubyModule getLastClass() {
        return lastClass;
    }

    /** Setter for property lastClass.
     * @param lastClass New value of property lastClass.
     */
    public void setLastClass(RubyModule lastClass) {
        this.lastClass = lastClass;
    }

    /** Getter for property lastFunc.
     * @return Value of property lastFunc.
     */
    public String getLastFunc() {
        return lastFunc;
    }

    /** Setter for property lastFunc.
     * @param lastFunc New value of property lastFunc.
     */
    public void setLastFunc(String lastFunc) {
        this.lastFunc = lastFunc;
    }

    /** Getter for property line.
     * @return Value of property line.
     */
    public int getLine() {
        return line;
    }

    /** Setter for property line.
     * @param line New value of property line.
     */
    public void setLine(int line) {
        this.line = line;
    }

    /** Getter for property prev.
     * @return Value of property prev.
     */
    public RubyFrame getPrev() {
        return prev;
    }

    /** Setter for property prev.
     * @param prev New value of property prev.
     */
    public void setPrev(RubyFrame prev) {
        this.prev = prev;
    }

    /** Getter for property self.
     * @return Value of property self.
     */
    public RubyObject getSelf() {
        return self;
    }

    /** Setter for property self.
     * @param self New value of property self.
     */
    public void setSelf(RubyObject self) {
        this.self = self;
    }

    /** Getter for property tmp.
     * @return Value of property tmp.
     */
    public RubyFrame getTmp() {
        return tmp;
    }

    /** Setter for property tmp.
     * @param tmp New value of property tmp.
     */
    public void setTmp(RubyFrame tmp) {
        this.tmp = tmp;
    }

    /**
     * pushes a copy of this frame in the tmp stack.
     * 
     **/
    public void tmpPush() {
        RubyFrame tmpFrame = new RubyFrame(ruby, self, args, lastFunc, lastClass, namespace, prev, tmp, file, line, iter, flags);

        tmp = tmpFrame;
    }
    /**
     * pops the top of the tmp stack
     **/
    public void tmpPop() {
        self = tmp.self;
        args = tmp.args;
        lastFunc = tmp.lastFunc;
        lastClass = tmp.lastClass;
        namespace = tmp.namespace;
        prev = tmp.prev;
        file = tmp.file;
        line = tmp.line;
        iter = tmp.iter;
        flags = tmp.flags;
        ruby = tmp.ruby; //like it really could be different, maybe with threads...
        tmp = tmp.tmp;
    }

    /** Push a new empty frame to the frame stack.
     *
     */
    public void push() {
        RubyFrame oldFrame = new RubyFrame(ruby, self, args, lastFunc, lastClass, namespace, prev, tmp, file, line, iter, flags);

        prev = oldFrame;
        tmp = null;
        file = ruby.getSourceFile();
        line = ruby.getSourceLine();
        iter = ruby.getIter().getIter();
        args = null;
        flags = FRAME_ALLOCA;
    }

    /** Pop the frame.
     *
     */
    public void pop() {
		//Benoit: according to the POP_FRAME macro 
		//(and to logic, poping should restore the
		//previous state) the ruby sourcefile and 
		//sourceline should be the one saved on the pushed 
		//frame
        ruby.setSourceFile(file);
        ruby.setSourceLine(line);
        self = prev.self;
        args = prev.args;
        lastFunc = prev.lastFunc;
        lastClass = prev.lastClass;
        namespace = prev.namespace;
        tmp = prev.tmp;
        file = prev.file;
        line = prev.line;
        iter = prev.iter;
        flags = prev.flags;

        prev = prev.prev;

    }
}
