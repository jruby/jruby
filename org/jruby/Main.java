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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.io.*;
import java.util.ArrayList;

import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.*;
import org.jruby.javasupport.JavaUtil;
import org.jruby.regexp.*;
import org.jruby.runtime.RubyGlobalEntry;
import org.jruby.nodes.Node;
import org.jruby.nodes.DumpVisitor;

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

    private static Class sRegexpAdapter;

    // print bugs
    // FIXME: remove if really not used Benoit.
    //	private static boolean printBugs = false;
    private static ArrayList sLoadDirectories = new ArrayList();
    private static String sScript = null;
    private static String sFileName = null;
    //list of libraries to require first
    private static ArrayList sRequireFirst = new ArrayList();
    private static boolean sYyDebug = false;
    private static boolean sBenchmarkMode = false;
    private static boolean sCheckOnly = false;
    private static boolean sDoLoop = false;
    private static boolean sDoPrint = false;
    private static boolean sDoLine = false;
    private static boolean sDoSplit = false;
    private static class ArgIter {
        int idxArg;
        int idxChar;
    }
    /*
     * helper function for args processing.
     */
    private static String grabValue(String args[], String errorMessage, ArgIter ioIter) {
        if (++ioIter.idxChar < args[ioIter.idxArg].length()) {
            return args[ioIter.idxArg].substring(ioIter.idxChar);
        } else if (++ioIter.idxArg < args.length) {
            return args[ioIter.idxArg];
        } else {
            System.err.println("invalid argument " + ioIter.idxArg);
            System.err.println(errorMessage);
            printUsage();
            System.exit(1);
        }
        return null;
    }

    /**
     * process the command line arguments.
     * This method will consume the appropriate arguments and valuate
     * the static variables corresponding to the options.
     * @param args the command line arguments
     * @return the arguments left
     **/
    private static String[] processArgs(String args[]) {
        int lenArg = args.length;
        StringBuffer lBuf = new StringBuffer();
        ArgIter lIter = new ArgIter();
        for (; lIter.idxArg < lenArg; lIter.idxArg++) {
            if (args[lIter.idxArg].charAt(0) == '-') {
                FOR : for (lIter.idxChar = 1; lIter.idxChar < args[lIter.idxArg].length(); lIter.idxChar++)
                    switch (args[lIter.idxArg].charAt(lIter.idxChar)) {
                        case 'h' :
                            printUsage();
                            break;
                        case 'I' :
                            sLoadDirectories.add(grabValue(args, " -I must be followed by a directory name to add to lib path", lIter));
                            break FOR;
                        case 'r' :
                            sRequireFirst.add(grabValue(args, "-r must be followed by a package to require", lIter));
                            break FOR;
                        case 'e' :
                            lBuf.append(grabValue(args, " -e must be followed by an expression to evaluate", lIter)).append("\n");
                            break FOR;
                        case 'b' :
                            // Benchmark
                            sBenchmarkMode = true;
                            break;
                        case 'R' :
                            String lRegexpAdapter = grabValue(args, " -R must be followed by an expression to evaluate", lIter);
                            try {
                                sRegexpAdapter = Class.forName(lRegexpAdapter);
                            } catch (ClassNotFoundException cnfExcptn_0) {
                                try {
                                    sRegexpAdapter = Class.forName("org.jruby.regexp." + lRegexpAdapter + "RegexpAdapter");
                                } catch (ClassNotFoundException cnfExcptn_1) {
                                	System.err.println("invalid argument " + lIter.idxArg);
                                	System.err.println("failed to load RegexpAdapter: " + args[lIter.idxArg]);
                                	System.err.println("defaulting to default RegexpAdapter: GNURegexpAdapter");
                                }
                            }
                            break FOR;
                        case 'c' :
                            sCheckOnly = true;
                            break;
                        case 'y' :
                            sYyDebug = true;
                            break;
                        case 'p' :
                            sDoPrint = true;
                            //fall through on purpose
                        case 'n' :
                            sDoLoop = true;
                            break;
                        case 'a' :
                            sDoSplit = true;
                            break;
                        case 'l' :
                            sDoLine = true;
                            break;
                        default :
                            System.err.println("unknown option " + args[lIter.idxArg].charAt(lIter.idxChar));
                            System.exit(1);
                    }
            } else {
                if (lBuf.length() == 0) //only get a filename if there were no -e
                    sFileName = args[lIter.idxArg++]; //consume the file name
                break; //the rests are args for the script
            }
        }
        sScript = lBuf.toString();
        String[] lRet = new String[lenArg - lIter.idxArg];
        System.arraycopy(args, lIter.idxArg, lRet, 0, lRet.length);
        return lRet;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
           System.out.println();
           System.out.println("----------------------------------------------------");
           System.out.println("--- WARNING this is an ALPHA version of JRuby!!! ---");
           System.out.println("----------------------------------------------------");
           System.out.println();
         */

        // Benchmark
        long now = -1;
        String[] argv = processArgs(args);
        if (sBenchmarkMode)
            now = System.currentTimeMillis();
        if (sScript.length() > 0) {
            runInterpreter(sScript, "-e", argv);
        } else if (sFileName != null) {
            runInterpreterOnFile(sFileName, argv);
        } else {
            System.err.println("nothing to interpret");
            printUsage(); //interpreting from the command line not supported yet
            return;
        }
        // Benchmark
        if (now != -1) {
            System.out.println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
        }
    }
    static boolean sPrintedUsage = false;
    /**
     * Prints the usage for the class.
     *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
     *           -e 'command'   one line of script. Several -e's allowed. Omit [programfile]
     *           -b             benchmark mode
     *           -Idirectory    specify $LOAD_PATH directory (may be used more than once)
     *           -R 'adapter'  used to select a regexp engine
     *           -c 			check syntax and dump parse tree
     *           -y 			debug parser
     */
    protected static void printUsage() {
        if (!sPrintedUsage) {
            System.out.println("Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments] $Date$");
            System.out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
            System.out.println("    -b              benchmark mode, times the script execution");
            System.out.println("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)");
            System.out.println("    -R 'class'     The adapter class for the regexp engine, for now can be:");
            System.out.println("                    org.jruby.regexp.GNURegexpAdapter or org.jruby.regexp.JDKRegexpAdapter");
            System.out.println("    -c 				check syntax and dump parse tree");
            System.out.println("    -y 				activate parser traces.");
            sPrintedUsage = true;
        }
    }

    /**
     * Launch the interpreter on a specific String.
     *
     * @param iString2Eval the string to evaluate
     * @param iFileName the name of the File from which the string comes.
     */
    protected static void runInterpreter(String iString2Eval, String iFileName, String[] args) {
        // Initialize Runtime
        Ruby ruby = Ruby.getDefaultInstance(sRegexpAdapter != null ? sRegexpAdapter : GNURegexpAdapter.class);

        // Parse and interpret file
        RubyString rs = RubyString.newString(ruby, iString2Eval);
        RubyObject lArgv = JavaUtil.convertJavaToRuby(ruby, args, String[].class);
        ruby.defineGlobalConstant("ARGV", lArgv);
        ruby.defineReadonlyVariable("$-p", (sDoPrint ? ruby.getTrue() : ruby.getNil()));
        ruby.defineReadonlyVariable("$-n", (sDoLoop ? ruby.getTrue() : ruby.getNil()));
        ruby.defineReadonlyVariable("$-a", (sDoSplit ? ruby.getTrue() : ruby.getNil()));
        ruby.defineReadonlyVariable("$-l", (sDoLine ? ruby.getTrue() : ruby.getNil()));
        ruby.defineReadonlyVariable("$*", lArgv);
        ruby.initLoad(sLoadDirectories);
        //require additional libraries
        int lNbRequire = sRequireFirst.size();
        for (int i = 0; i < lNbRequire; i++)
            RubyGlobal.require(ruby, null, new RubyString(ruby, (String) sRequireFirst.get(i)));
        // +++
        try {
            Node lScript = ruby.getRubyParser().compileString(iFileName, rs, 0);
            //				DumpVisitor laVisitor = new DumpVisitor();
            //				lScript.accept(laVisitor);
            //				ruby.getRuntime().getOutputStream().println(laVisitor.dump());
            if (sDoPrint) {
                ruby.getParserHelper().rb_parser_append_print();
            }
            if (sDoLoop) {
                ruby.getParserHelper().rb_parser_while_loop(sDoLine, sDoSplit);
            }
            lScript = ruby.getParserHelper().getEvalTree();
            if (sCheckOnly) {
                DumpVisitor lVisitor = new DumpVisitor();
                lScript.accept(lVisitor);
                ruby.getRuntime().getOutputStream().println(lVisitor.dump());
            } else {
                ruby.getRubyTopSelf().eval(lScript);
            }
        } catch (RaiseException rExcptn) {
            ruby.getRuntime().printError(rExcptn.getActException());
        } catch (ThrowJump throwJump ) {
            ruby.getRuntime().printError(throwJump.getNameError());
        }
        // ---
		// to look nicer
		ruby.getRuntime().getOutputStream().println("");
    }

    /**
     * Run the interpreter on a File.
     * open a file and feeds it to the interpreter.
     *
     * @param fileName the name of the file to interpret
     */
    protected static void runInterpreterOnFile(String fileName, String[] args) {
        File rubyFile = new File(fileName);
        if (!rubyFile.canRead()) {
            System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
        } else {
            try {
                StringBuffer sb = new StringBuffer((int) rubyFile.length());
                BufferedReader br = new BufferedReader(new FileReader(rubyFile));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                br.close();
                runInterpreter(sb.toString(), fileName, args);

            } catch (IOException ioExcptn) {
                System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
                System.out.println("IOEception: " + ioExcptn.getMessage());
            }
        }
    }
}