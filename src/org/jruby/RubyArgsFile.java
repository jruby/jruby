/*
 * RubyArgsFile.java - No description
 * Created on 13.01.2002, 17:08:47
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby runtime) {
        super(runtime, runtime.getClasses().getObjectClass());
    }

    private RubyIO currentFile = null;
    private int currentLineNumber;
    
    public void setCurrentLineNumber(int newLineNumber) {
        this.currentLineNumber = newLineNumber;
    }
    
    public void initArgsFile() {
        extendObject(getRuntime().getModule("Enumerable"));
        
        getRuntime().defineReadonlyVariable("$<", this);
        getRuntime().defineGlobalConstant("ARGF", this);
        
        CallbackFactory callbackFactory = getRuntime().callbackFactory(RubyArgsFile.class);
        defineSingletonMethod("each", callbackFactory.getOptMethod("each_line"));
        defineSingletonMethod("each_line", callbackFactory.getOptMethod("each_line"));

		defineSingletonMethod("filename", callbackFactory.getMethod("filename"));
//		defineSingletonMethod("gets", callbackFactory.getOptSingletonMethod(RubyGlobal.class, "gets"));
//		defineSingletonMethod("readline", callbackFactory.getOptSingletonMethod(RubyGlobal.class, "readline"));
//		defineSingletonMethod("readlines", callbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		
//		defineSingletonMethod("to_a", callbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		defineSingletonMethod("to_s", callbackFactory.getMethod("filename"));

        getRuntime().defineReadonlyVariable("$FILENAME", getRuntime().newString("-"));

        // This is ugly.  nextArgsFile both checks existence of another
        // file and the setup of any files.  On top of that it handles
        // the logic for figuring out stdin versus a list of files.
        // I hacked this to make a null currentFile indicate that things
        // have not been set up yet.  This seems fragile, but it at least
        // it passes tests now.
        //currentFile = (RubyIO) runtime.getGlobalVariables().get("$stdin");
    }

    protected boolean nextArgsFile() {
        RubyArray args = (RubyArray)getRuntime().getGlobalVariables().get("$*");

        if (args.getLength() == 0) {
            if (currentFile == null) { 
                currentFile = (RubyIO) getRuntime().getGlobalVariables().get("$stdin");
                ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue("-");
                currentLineNumber = 0;
                return true;
            }

            return false;
        }

        String filename = ((RubyString) args.shift()).getValue();
        ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue(filename);

        if (filename.equals("-")) {
            currentFile = (RubyIO) getRuntime().getGlobalVariables().get("$stdin");
        } else {
            currentFile = new RubyFile(getRuntime(), filename); 
        }

        return true;
    }
    
    public IRubyObject internalGets(IRubyObject[] args) {
        if (currentFile == null && !nextArgsFile()) {
            return getRuntime().getNil();
        }
        
        RubyString line = (RubyString)currentFile.callMethod("gets", args);
        
        while (line.isNil()) {
            currentFile.callMethod("close");
            if (! nextArgsFile()) {
                currentFile = null;
                return line;
        	}
            line = (RubyString) currentFile.callMethod("gets", args);
        }
        
        currentLineNumber++;
        getRuntime().getGlobalVariables().set("$.", getRuntime().newFixnum(currentLineNumber));
        
        return line;
    }
    
    // ARGF methods
    
    /** Invoke a block for each line.
     * 
     */
    public IRubyObject each_line(IRubyObject[] args) {
        IRubyObject nextLine = internalGets(args);
        
        while (!nextLine.isNil()) {
        	getRuntime().yield(nextLine);
        	nextLine = internalGets(args);
        }
        
        return this;
    }
    
	public RubyString filename() {
        return (RubyString)getRuntime().getGlobalVariables().get("$FILENAME");
    }
}
