/*
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.util;

import org.jruby.Main;

import java.util.ArrayList;
import java.util.List;

public class CommandlineParser {
    private String[] arguments;

    public String sRegexpAdapter;
    public ArrayList sLoadDirectories = new ArrayList();
    private StringBuffer inlineScript = new StringBuffer();
    public String scriptFilename = null;
    private ArrayList requiredLibraries = new ArrayList();
    public boolean sBenchmarkMode = false;
    public boolean sDoLoop = false;
    public boolean sDoPrint = false;
    public boolean sDoLine = false;
    public boolean sDoSplit = false;
    public boolean verbose = false;
    public boolean showVersion = false;
    public String[] scriptArguments = null;

    public int argumentIndex = 0;
    public int characterIndex = 0;

    public CommandlineParser(String[] arguments) {
        this.arguments = arguments;
        processArguments();
    }

    /*
     * helper function for args processing.
     */
    private String grabValue(String args[], String errorMessage) {
        characterIndex++;
        if (characterIndex < args[argumentIndex].length()) {
            return args[argumentIndex].substring(characterIndex);
        }
        argumentIndex++;
        if (argumentIndex < args.length) {
            return args[argumentIndex];
        } else {
            System.err.println("invalid argument " + argumentIndex);
            System.err.println(errorMessage);
            Main.printUsage();
            System.exit(1);
        }
        return null;
    }

    private void processArguments() {
        while (argumentIndex < arguments.length && isInterpreterArgument(arguments[argumentIndex])) {
            processArgument(arguments);
            argumentIndex++;
        }
        if (inlineScript.length() == 0) {//only get a filename if there were no -e
            if (argumentIndex < arguments.length) {
                scriptFilename = arguments[argumentIndex]; //consume the file name
                argumentIndex++;
            }
        }
        // Remaining arguments are for the script itself
        scriptArguments = new String[arguments.length - argumentIndex];
        System.arraycopy(arguments, argumentIndex, scriptArguments, 0, scriptArguments.length);
    }

    private static boolean isInterpreterArgument(String argument) {
        return argument.charAt(0) == '-';
    }

    private void processArgument(String[] args) {
        String argument = args[argumentIndex];
        FOR : for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case 'h' :
                    Main.printUsage();
                    break;
                case 'I' :
                    sLoadDirectories.add(grabValue(args, " -I must be followed by a directory name to add to lib path"));
                    break FOR;
                case 'r' :
                    requiredLibraries.add(grabValue(args, "-r must be followed by a package to require"));
                    break FOR;
                case 'e' :
                    inlineScript.append(grabValue(args, " -e must be followed by an expression to evaluate"));
                    inlineScript.append('\n');
                    break FOR;
                case 'b' :
                    sBenchmarkMode = true;
                    break;
                case 'R' :
                    sRegexpAdapter = grabValue(args, " -R must be followed by an expression to evaluate");
                    break FOR;
                case 'p' :
                    sDoPrint = true;
                    sDoLoop = true;
                    break;
                case 'n' :
                    sDoLoop = true;
                    break;
                case 'a' :
                    sDoSplit = true;
                    break;
                case 'l' :
                    sDoLine = true;
                    break;
                case 'v' :
                    Main.showVersion();
                    verbose = true;
                    break;
                case 'w' :
                    verbose = true;
                    break;
                case '-' :
                    if (argument.equals("--version")) {
                        showVersion = true;
                        break FOR;
                    }
                default :
                    System.err.println("unknown option " + argument.charAt(characterIndex));
                    System.exit(1);
            }
        }
    }

    public boolean hasInlineScript() {
        return (inlineScript.length() > 0);
    }

    public String inlineScript() {
        return inlineScript.toString();
    }

    public List requiredLibraries() {
        return requiredLibraries;
    }
}
