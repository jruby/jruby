/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(IRuby runtime) {
        super(runtime, runtime.getObject());
    }

    private IRubyObject currentFile = null;
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
                currentFile = getRuntime().getGlobalVariables().get("$stdin");
                ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue(new StringBuffer("-"));
                currentLineNumber = 0;
                return true;
            }

            return false;
        }

        String filename = ((RubyString) args.shift()).toString();
        ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue(new StringBuffer(filename));

        if (filename.equals("-")) {
            currentFile = getRuntime().getGlobalVariables().get("$stdin");
        } else {
            currentFile = new RubyFile(getRuntime(), filename); 
        }

        return true;
    }
    
    public IRubyObject internalGets(IRubyObject[] args) {
        if (currentFile == null && !nextArgsFile()) {
            return getRuntime().getNil();
        }
        
        ThreadContext context = getRuntime().getCurrentContext();
        
        IRubyObject line = currentFile.callMethod(context, "gets", args);
        
        while (line instanceof RubyNil) {
            currentFile.callMethod(context, "close");
            if (! nextArgsFile()) {
                currentFile = null;
                return line;
        	}
            line = currentFile.callMethod(context, "gets", args);
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
        	getRuntime().getCurrentContext().yield(nextLine);
        	nextLine = internalGets(args);
        }
        
        return this;
    }
    
	public RubyString filename() {
        return (RubyString)getRuntime().getGlobalVariables().get("$FILENAME");
    }
}
