/*
 * RubyArgsFile.java - No description
 * Created on 13.01.2002, 17:08:47
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
package org.jruby;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jruby.exceptions.IOError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyInputStream;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby ruby) {
        super(ruby, ruby.getClasses().getObjectClass());
    }

    private RubyIO currentFile = null;
    private int currentLineNumber;
    
    public void setCurrentLineNumber(int newLineNumber) {
        this.currentLineNumber = newLineNumber;
    }
    
    public void initArgsFile() {
        extendObject(runtime.getModule("Enumerable"));
        
        runtime.defineReadonlyVariable("$<", this);
        runtime.defineGlobalConstant("ARGF", this);
        
        defineSingletonMethod("each", CallbackFactory.getOptMethod(RubyArgsFile.class, "each_line"));
        defineSingletonMethod("each_line", CallbackFactory.getOptMethod(RubyArgsFile.class, "each_line"));

		defineSingletonMethod("filename", CallbackFactory.getMethod(RubyArgsFile.class, "filename"));
		defineSingletonMethod("gets", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "gets"));
		defineSingletonMethod("readline", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readline"));
		defineSingletonMethod("readlines", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		
		defineSingletonMethod("to_a", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		defineSingletonMethod("to_s", CallbackFactory.getMethod(RubyArgsFile.class, "filename"));

        runtime.defineReadonlyVariable("$FILENAME", RubyString.newString(runtime, "-"));
        currentFile = (RubyIO) runtime.getGlobalVariables().get("$stdin");
    }

    protected boolean nextArgsFile() {
        RubyArray args = (RubyArray)runtime.getGlobalVariables().get("$*");

        if (args.getLength() == 0) {
            if (currentFile == runtime.getGlobalVariables().get("$stdin")) {
                return true;
            }
            currentFile = (RubyIO) runtime.getGlobalVariables().get("$stdin");
            ((RubyString) runtime.getGlobalVariables().get("$FILENAME")).setValue("-");
            currentLineNumber = 0;
            return false;
        }

        String filename = ((RubyString) args.shift()).getValue();
        ((RubyString) runtime.getGlobalVariables().get("$FILENAME")).setValue(filename);

        if (filename.equals("-")) {
            currentFile = (RubyIO) runtime.getGlobalVariables().get("$stdin");
        } else {
            File file = new File(filename);
            try {
                RubyInputStream inStream = new RubyInputStream(new BufferedInputStream(new FileInputStream(file)));

                currentFile = new RubyFile(runtime, runtime.getClass("File"));
                currentFile.initIO(inStream, null, filename);

            } catch (FileNotFoundException fnfExcptn) {
                throw new IOError(runtime, fnfExcptn.getMessage());
            }
        }

        return true;
    }
    
    public RubyString internalGets(IRubyObject[] args) {
        if (!nextArgsFile()) {
            return RubyString.nilString(runtime);
        }

        RubyString line = (RubyString)currentFile.callMethod("gets", args);
        
        while (line.isNil()) {
            currentFile.callMethod("close");
            if (! nextArgsFile()) {
            	return line;
        	}
            line = (RubyString) currentFile.callMethod("gets", args);
        }
        
        currentLineNumber++;
        runtime.getGlobalVariables().set("$.", RubyFixnum.newFixnum(runtime, currentLineNumber));
        
        return line;
    }
    
    // ARGF methods
    
    /** Invoke a block for each line.
     * 
     */
    public IRubyObject each_line(IRubyObject[] args) {
        RubyString nextLine = internalGets(args);
        
        while (!nextLine.isNil()) {
        	getRuntime().yield(nextLine);
        	nextLine = internalGets(args);
        }
        
        return this;
    }
    
	public RubyString filename() {
        return (RubyString)runtime.getGlobalVariables().get("$FILENAME");
    }
}
