/*
 * Main.java - No description
 * Created on 18. September 2001, 21:48
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import java.io.*;

import org.jruby.parser.*;

/**
 * Class used to launch the interpreter.
 * This is the main class as defined in the jruby.mf manifest.
 * It is very basic and does not support yet the same array of switches
 * as the C interpreter.
 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
 *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
 * @author  jpetersen
 * @version 0.1
 */
public class Main {

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        System.out.println();
        System.out.println("----------------------------------------------------");
        System.out.println("--- WARNING this is an ALPHA version of JRuby!!! ---");
        System.out.println("----------------------------------------------------");
        System.out.println();
        
        if (args.length == 0) {
            printUsage();
        } else {
            int lenArg = args.length;
            for (int i = 0; i < lenArg; i++) {
                if (args[i].equals("-h") || args[i].equals("-help")) {
                    printUsage();
                } else if (args[i].equals("-e")) {
                    if (i++ >= lenArg) {
                        System.err.println("invalid argument " + i);
                        System.err.println(" -e must be followed by an expression to evaluate");
                        printUsage();
                    } else {
                        runInterpreter(args[i], "command line " + i);
                    }
                } else {
                    runInterpreterOnFile(args[0]);
                }
            }
        }
    }
    
    /**
     * Prints the usage for the class.
     *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
     *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
     */ 
    protected static void printUsage() {
        System.out.println("Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]");
        System.out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
    }
    
    /**
     * Launch the interpreter on a specific String.
     * @param iString2Eval
     * the string to evaluate
     * @param iFileName
     * the name of the File from which the string comes.
     */
    protected static void runInterpreter(String iString2Eval, String iFileName) {
        // Initialize Runtime
        Ruby ruby = new Ruby();

        // Initialize Parser
        parse p = new parse(ruby);

        // Parse and interpret file
        RubyString rs = RubyString.m_newString(ruby, iString2Eval);
        ruby.getInterpreter().eval(ruby.getObjectClass(), p.rb_compile_string(iFileName, rs, 0));
    }

    /**
     * Run the interpreter on a File.
     * open a file and feeds it to the interpreter.
     * @param fileName
     * the name of the file to interpret
     */
    protected static void runInterpreterOnFile(String fileName) {
        File rubyFile = new File(fileName);
        if (!rubyFile.canRead()) {
            System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
        } else {
            try {
                StringBuffer sb = new StringBuffer((int)rubyFile.length());
                BufferedReader br = new BufferedReader(new FileReader(rubyFile));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                br.close();
                runInterpreter(sb.toString(), fileName);
                
            } catch (IOException ioExcptn) {
                System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
                System.out.println("IOEception: " + ioExcptn.getMessage());
            }
        }
    }
}