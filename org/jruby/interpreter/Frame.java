/*
 * Frame.java - No description
 * Created on 10. September 2001, 17:54
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby.interpreter;

import java.util.*;

import org.jruby.*;
import org.jruby.original.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class Frame {
    public static final int FRAME_ALLOCA = 0;
    public static final int FRAME_MALLOC = 1;
    
    private RubyObject self         = null;
    private List args               = null;
    private RubyId lastFunc         = null;
    private RubyModule lastClass    = null;
    private VALUE cbase             = null;
    private Frame prev              = null;
    private Frame tmp               = null;
    private String file             = null;
    private int line                = 0;
    private int iter                = 0;
    private int flags               = 0;

    public Frame() {
    }
    
    public Frame(RubyObject self, List args, RubyId lastFunc, RubyModule lastClass,
                 VALUE cbase, Frame prev, Frame tmp, String file, int line, int iter, int flags) {
        this();
        
        this.self = self;
        this.args = args;
        this.lastFunc = lastFunc;
        this.lastClass = lastClass;
        this.cbase = cbase;
        this.prev = prev;
        this.tmp = tmp;
        this.file = file;
        this.line = line;
        this.iter = iter;
        this.flags = flags;
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
    
    /** Getter for property cbase.
     * @return Value of property cbase.
     */
    public VALUE getCbase() {
        return cbase;
    }
    
    /** Setter for property cbase.
     * @param cbase New value of property cbase.
     */
    public void setCbase(VALUE cbase) {
        this.cbase = cbase;
    }
    
    /** Getter for property file.
     * @return Value of property file.
     */
    public java.lang.String getFile() {
        return file;
    }
    
    /** Setter for property file.
     * @param file New value of property file.
     */
    public void setFile(java.lang.String file) {
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
    public RubyId getLastFunc() {
        return lastFunc;
    }
    
    /** Setter for property lastFunc.
     * @param lastFunc New value of property lastFunc.
     */
    public void setLastFunc(RubyId lastFunc) {
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
    public org.jruby.interpreter.Frame getPrev() {
        return prev;
    }
    
    /** Setter for property prev.
     * @param prev New value of property prev.
     */
    public void setPrev(org.jruby.interpreter.Frame prev) {
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
    public org.jruby.interpreter.Frame getTmp() {
        return tmp;
    }
    
    /** Setter for property tmp.
     * @param tmp New value of property tmp.
     */
    public void setTmp(org.jruby.interpreter.Frame tmp) {
        this.tmp = tmp;
    }
    
    /** Push a new empty frame to the frame stack.
     *
     */
    public void push() {
        Frame oldFrame = new Frame(self, args, lastFunc, lastClass, cbase, prev,
                                   tmp, file, line, iter, flags);
        
        prev    = oldFrame;
        tmp     = null;
        // file    = null
        // line    =
        // iter    =
        // cbase = cbase;
        args = null;
        flags = FRAME_ALLOCA;
    }
    
    /** Pop the frame.
     *
     */
    public void pop() {
        self = prev.self;
        args = prev.args;
        lastFunc = prev.lastFunc;
        lastClass = prev.lastClass;
        cbase = prev.cbase;
        tmp = prev.tmp;
        file = prev.file;
        line = prev.line;
        iter = prev.iter;
        flags = prev.flags;
        
        prev = prev.prev;
        
        // ruby_file = file;
        // ruby_line = line;
    }
}