/*
 * Main.java - No description
 * Created on 18. September 2001, 21:48
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
package org.jruby;

import java.util.Iterator;
import java.io.StringReader;
import java.io.Reader;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThrowJump;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.ParserSupport;
import org.jruby.util.CommandlineParser;
import org.ablaf.ast.INode;

/**
 * Class used to launch the interpreter.
 * This is the main class as defined in the jruby.mf manifest.
 * It is very basic and does not support yet the same array of switches
 * as the C interpreter.
 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
 *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
 * @author  jpetersen
 * @version $Revision$
 */
public class Main {

    public static void main(String args[]) {
        CommandlineParser commandline = new CommandlineParser(args);

        if (commandline.showVersion) {
            showVersion();
            return;
        }
        long now = -1;
        if (commandline.sBenchmarkMode)
            now = System.currentTimeMillis();
        if (commandline.hasInlineScript()) {
            runInterpreter(new StringReader(commandline.inlineScript()), "-e", commandline.scriptArguments, commandline);
        } else if (commandline.scriptFilename != null) {
            runInterpreterOnFile(commandline.scriptFilename, commandline.scriptArguments, commandline);
        } else {
            System.err.println("nothing to interpret");
            printUsage();
            return;
        }
        if (commandline.sBenchmarkMode) {
            System.out.println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
        }
    }

    public static void showVersion() {
        System.out.print("ruby ");
        System.out.print(Constants.RUBY_VERSION);
        System.out.print(" (");
        System.out.print(Constants.COMPILE_DATE);
        System.out.print(") [");
        System.out.print("java");
        System.out.println("]");
    }

    static boolean hasPrintedUsage = false;
    /**
     * Prints the usage for the class.
     *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
     *           -e 'command'   one line of script. Several -e's allowed. Omit [programfile]
     *           -b             benchmark mode
     *           -Idirectory    specify $LOAD_PATH directory (may be used more than once)
     *           -R 'adapter'  used to select a regexp engine
     */
    public static void printUsage() {
        if (!hasPrintedUsage) {
            System.out.println("Usage: jruby [switches] [rubyfile.rb] [arguments]");
            System.out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
            System.out.println("    -b              benchmark mode, times the script execution");
            System.out.println("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)");
            System.out.println("    -R 'name'       The regexp engine to use, for now can be JDK, GNU or ORO");
            hasPrintedUsage = true;
        }
    }

    /**
     * Launch the interpreter on a specific String.
     *
     * @param reader the string to evaluate
     * @param filename the name of the File from which the string comes.
     */
    protected static void runInterpreter(Reader reader, String filename, String[] args, CommandlineParser commandline) {
        Ruby runtime = Ruby.getDefaultInstance(commandline.sRegexpAdapter);

        IRubyObject argumentArray = JavaUtil.convertJavaToRuby(runtime, args);

        runtime.setVerbose(commandline.verbose);
        runtime.defineReadonlyVariable("$VERBOSE", commandline.verbose ? runtime.getTrue() : runtime.getNil());

        runtime.defineGlobalConstant("ARGV", argumentArray);
        runtime.defineReadonlyVariable("$-p", (commandline.sDoPrint ? runtime.getTrue() : runtime.getNil()));
        runtime.defineReadonlyVariable("$-n", (commandline.sDoLoop ? runtime.getTrue() : runtime.getNil()));
        runtime.defineReadonlyVariable("$-a", (commandline.sDoSplit ? runtime.getTrue() : runtime.getNil()));
        runtime.defineReadonlyVariable("$-l", (commandline.sDoLine ? runtime.getTrue() : runtime.getNil()));
        runtime.defineReadonlyVariable("$*", argumentArray);
        runtime.defineVariable(new RubyGlobal.StringGlobalVariable(runtime, "$0", RubyString.newString(runtime, filename)));
        runtime.getLoadService().init(runtime, commandline.sLoadDirectories);
        try {
            Iterator iter = commandline.requiredLibraries().iterator();
            while (iter.hasNext()) {
                String scriptName = (String) iter.next();
                KernelModule.require(runtime.getTopSelf(), RubyString.newString(runtime, scriptName));
            }

            INode parsedScript = runtime.parse(reader, filename);
            if (commandline.sDoPrint) {
                parsedScript = new ParserSupport().appendPrintToBlock(parsedScript);
            }
            if (commandline.sDoLoop) {
                parsedScript = new ParserSupport().appendWhileLoopToBlock(parsedScript, commandline.sDoLine, commandline.sDoSplit);
            }
            runtime.eval(parsedScript);

        } catch (RaiseException rExcptn) {
            runtime.getRuntime().printError(rExcptn.getException());
        } catch (ThrowJump throwJump) {
            runtime.getRuntime().printError(throwJump.getNameError());
        }
    }

    /**
     * Run the interpreter on a File.
     * open a file and feeds it to the interpreter.
     *
     * @param fileName the name of the file to interpret
     */
    protected static void runInterpreterOnFile(String fileName, String[] args, CommandlineParser commandline) {
        File file = new File(fileName);
        if (!file.canRead()) {
            System.out.println("Cannot read source file: \"" + fileName + "\"");
        } else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                runInterpreter(reader, fileName, args, commandline);
                reader.close();
            } catch (IOException ioExcptn) {
                System.out.println("Error reading source file: " + ioExcptn.getMessage());
            }
        }
    }
}
