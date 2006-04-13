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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
 * Copyright (C) 2005 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
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

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Iterator;

import org.jruby.ast.Node;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.ParserSupport;
import org.jruby.runtime.Constants;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CommandlineParser;

/**
 * Class used to launch the interpreter.
 * This is the main class as defined in the jruby.mf manifest.
 * It is very basic and does not support yet the same array of switches
 * as the C interpreter.
 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
 *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
 * @author  jpetersen
 */
public class Main {
    private CommandlineParser commandline;
    private boolean hasPrintedUsage = false;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    
    public Main(InputStream in, PrintStream out, PrintStream err) {
    	this.in = in;
    	this.out = out;
    	this.err = err;
    }
    
    public Main() {
    	this(System.in, System.out, System.err);
    }

    public static void main(String[] args) {
    	Main main = new Main();
    	
    	try {
    		main.run(args);
    	} catch (MainExitException mee) {
    		main.err.println(mee.getMessage());
    		if (mee.isUsageError()) {
    			main.printUsage();
    		}
    		System.exit(mee.getStatus());
    	}
    }
    
    public int run(String[] args) {
        commandline = new CommandlineParser(this, args);

        if (commandline.isShowVersion()) {
            showVersion();
        }
        if (! commandline.shouldRunInterpreter()) {
            return 0;
        }

        long now = -1;
        if (commandline.isBenchmarking()) {
            now = System.currentTimeMillis();
        }

        int status = runInterpreter(commandline.getScriptSource(), commandline.displayedFileName());

        if (commandline.isBenchmarking()) {
            out.println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
        }
        
        return status;
    }

    private void showVersion() {
        out.print("ruby ");
        out.print(Constants.RUBY_VERSION);
        out.print(" (");
        out.print(Constants.COMPILE_DATE);
        out.print(") [");
        out.print("java");
        out.println("]");
    }

    public void printUsage() {
        if (!hasPrintedUsage) {
			out.println("Usage: jruby [switches] [--] [rubyfile.rb] [arguments]");
			out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
			out.println("    -b              benchmark mode, times the script execution");
			out.println("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)");
			out.println("    --              optional -- before rubyfile.rb for compatibility with ruby");
            hasPrintedUsage = true;
        }
    }

    private int runInterpreter(Reader reader, String filename) {
        IRuby runtime = Ruby.newInstance(in, out, err);

        try {
        	runInterpreter(runtime, reader, filename);
        	return 0;
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.RaiseJump) {
        		RubyException raisedException = ((RaiseException)je).getException();
        		
        		if (raisedException.isKindOf(runtime.getClass("SystemExit"))) {
                	RubyFixnum status = (RubyFixnum)raisedException.getInstanceVariable("status");
                	
                    if (status != null) {
                        return RubyNumeric.fix2int(status);
                    } else {
                        return 0;
                    }
        		} else {
		            runtime.printError(raisedException);
		            return 1;
        		}
        	} else if (je.getJumpType() == JumpException.JumpType.ThrowJump) {
	            runtime.printError((RubyException)je.getTertiaryData());
	            return 1;
        	} else {
        		throw je;
        	}
        }
    }
    
    private void runInterpreter(IRuby runtime, Reader reader, String filename) {
    	try {
    		initializeRuntime(runtime, filename);
    		Node parsedScript = getParsedScript(runtime, reader, filename);
    		runtime.eval(parsedScript);
    	
    	} finally {
    		runtime.tearDown();
    	}
    }

    private Node getParsedScript(IRuby runtime, Reader reader, String filename) {
        Node result = runtime.parse(reader, filename);
        if (commandline.isAssumePrinting()) {
            result = new ParserSupport().appendPrintToBlock(result);
        }
        if (commandline.isAssumeLoop()) {
            result = new ParserSupport().appendWhileLoopToBlock(result, commandline.isProcessLineEnds(), commandline.isSplit());
        }
        return result;
    }

    private void initializeRuntime(final IRuby runtime, String filename) {
        IRubyObject argumentArray = runtime.newArray(JavaUtil.convertJavaArrayToRuby(runtime, commandline.getScriptArguments()));
        runtime.setVerbose(runtime.newBoolean(commandline.isVerbose()));

        defineGlobalVERBOSE(runtime);
        runtime.getObject().setConstant("$VERBOSE", 
        		commandline.isVerbose() ? runtime.getTrue() : runtime.getNil());
        runtime.defineGlobalConstant("ARGV", argumentArray);

        defineGlobal(runtime, "$-p", commandline.isAssumePrinting());
        defineGlobal(runtime, "$-n", commandline.isAssumeLoop());
        defineGlobal(runtime, "$-a", commandline.isSplit());
        defineGlobal(runtime, "$-l", commandline.isProcessLineEnds());
        runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(argumentArray));
        // TODO this is a fake cause we have no real process number in Java
        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(runtime.hashCode())));
        runtime.defineVariable(new RubyGlobal.StringGlobalVariable(runtime, "$0", runtime.newString(filename)));
        runtime.getLoadService().init(commandline.loadPaths());
        Iterator iter = commandline.requiredLibraries().iterator();
        while (iter.hasNext()) {
            String scriptName = (String) iter.next();
            RubyKernel.require(runtime.getTopSelf(), runtime.newString(scriptName));
        }
    }

    private void defineGlobalVERBOSE(final IRuby runtime) {
        // $VERBOSE can be true, false, or nil.  Any non-false-nil value will get stored as true  
        runtime.getGlobalVariables().define("$VERBOSE", new IAccessor() {
            public IRubyObject getValue() {
                return runtime.getVerbose();
            }
            
            public IRubyObject setValue(IRubyObject newValue) {
                if (newValue.isNil()) {
                    runtime.setVerbose(newValue);
                } else {
                    runtime.setVerbose(runtime.newBoolean(newValue != runtime.getFalse()));
                }
            	
                return newValue;
            }
        });
    }

    private void defineGlobal(IRuby runtime, String name, boolean value) {
        runtime.getGlobalVariables().defineReadonly(name, new ValueAccessor(value ? runtime.getTrue() : runtime.getNil()));
    }

}
