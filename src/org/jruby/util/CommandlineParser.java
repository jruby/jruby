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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class CommandlineParser {
    private final String[] arguments;

    private ArrayList loadPaths = new ArrayList();
    private StringBuffer inlineScript = new StringBuffer();
    public String scriptFileName = null;
    private ArrayList requiredLibraries = new ArrayList();
    public boolean isBenchmarking = false;
    public boolean assumeLoop = false;
    public boolean assumePrinting = false;
    public boolean processLineEnds = false;
    public boolean sDoSplit = false;
    public boolean verbose = false;
    public boolean showVersion = false;
    public String[] scriptArguments = null;
    private boolean shouldRunInterpreter = true;

    public int argumentIndex = 0;
    public int characterIndex = 0;

    public CommandlineParser(String[] arguments) {
        this.arguments = arguments;
        processArguments();
    }

    private void processArguments() {
        while (argumentIndex < arguments.length && isInterpreterArgument(arguments[argumentIndex])) {
            processArgument();
            argumentIndex++;
        }
        if (! hasInlineScript()) {
            if (argumentIndex < arguments.length) {
                scriptFileName = arguments[argumentIndex]; //consume the file name
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

    private void processArgument() {
        String argument = arguments[argumentIndex];
        FOR : for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case 'h' :
                    Main.printUsage();
                    break;
                case 'I' :
                    loadPaths.add(grabValue(" -I must be followed by a directory name to add to lib path"));
                    break FOR;
                case 'r' :
                    requiredLibraries.add(grabValue("-r must be followed by a package to require"));
                    break FOR;
                case 'e' :
                    inlineScript.append(grabValue(" -e must be followed by an expression to evaluate"));
                    inlineScript.append('\n');
                    break FOR;
                case 'b' :
                    isBenchmarking = true;
                    break;
                case 'p' :
                    assumePrinting = true;
                    assumeLoop = true;
                    break;
                case 'n' :
                    assumeLoop = true;
                    break;
                case 'a' :
                    sDoSplit = true;
                    break;
                case 'l' :
                    processLineEnds = true;
                    break;
                case 'v' :
                    showVersion = true;
                    verbose = true;
                    break;
                case 'w' :
                    verbose = true;
                    break;
                case '-' :
                    if (argument.equals("--version")) {
                        showVersion = true;
                        shouldRunInterpreter = false;
                        break FOR;
                    }
                default :
                    System.err.println("unknown option " + argument.charAt(characterIndex));
                    System.exit(1);
            }
        }
    }

    private String grabValue(String errorMessage) {
        characterIndex++;
        if (characterIndex < arguments[argumentIndex].length()) {
            return arguments[argumentIndex].substring(characterIndex);
        }
        argumentIndex++;
        if (argumentIndex < arguments.length) {
            return arguments[argumentIndex];
        }
		System.err.println("invalid argument " + argumentIndex);
		System.err.println(errorMessage);
		Main.printUsage();
		System.exit(1);
        return null;
    }

    public boolean hasInlineScript() {
        return inlineScript.length() > 0;
    }

    public String inlineScript() {
        return inlineScript.toString();
    }

    public List requiredLibraries() {
        return requiredLibraries;
    }

    public List loadPaths() {
        return loadPaths;
    }

    public boolean shouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    private boolean isSourceFromStdin() {
        return scriptFileName == null;
    }

    public Reader getScriptSource() {
        if (hasInlineScript()) {
            return new StringReader(inlineScript());
        } else if (isSourceFromStdin()) {
            return new InputStreamReader(System.in);
        } else {
            File file = new File(scriptFileName);
            try {
                return new BufferedReader(new FileReader(file));
            } catch (IOException e) {
                System.err.println("Error opening script file: " + e.getMessage());
                System.exit(1);
            }
        }
        Asserts.notReached();
        return null;
    }

    public String displayedFileName() {
        if (hasInlineScript()) {
            return "-e";
        } else if (isSourceFromStdin()) {
            return "-";
        } else {
            return scriptFileName;
        }
    }
}