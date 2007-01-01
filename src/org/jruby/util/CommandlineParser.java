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
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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
package org.jruby.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Main;
import org.jruby.exceptions.MainExitException;

public class CommandlineParser {
    private final String[] arguments;
    private Main main;

    private ArrayList loadPaths = new ArrayList();
    private StringBuffer inlineScript = new StringBuffer();
    private String scriptFileName = null;
    private ArrayList requiredLibraries = new ArrayList();
    private boolean benchmarking = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private boolean processLineEnds = false;
    private boolean split = false;
    private boolean verbose = false;
    private boolean debug = false;
    private boolean showVersion = false;
    private String[] scriptArguments = null;
    private boolean shouldRunInterpreter = true;

    private boolean objectSpaceEnabled = true;
    private boolean compilerEnabled = false;
    private String encoding = "ISO8859_1";

    public int argumentIndex = 0;
    public int characterIndex = 0;

    public CommandlineParser(Main main, String[] arguments) {
        this.arguments = arguments;
        this.main = main;
        processArguments();
    }

    private void processArguments() {

        while (argumentIndex < arguments.length && isInterpreterArgument(arguments[argumentIndex])) {
            processArgument();
            argumentIndex++;
        }

        if (! hasInlineScript()) {
            if (argumentIndex < arguments.length) {
                setScriptFileName(arguments[argumentIndex]); //consume the file name
                argumentIndex++;
            }
        }


        // Remaining arguments are for the script itself
        scriptArguments = new String[arguments.length - argumentIndex];
        System.arraycopy(arguments, argumentIndex, getScriptArguments(), 0, getScriptArguments().length);
    }

    private static boolean isInterpreterArgument(String argument) {
        return argument.charAt(0) == '-';
    }

    private void processArgument() {
        String argument = arguments[argumentIndex];
        FOR : for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case 'h' :
                    main.printUsage();
                    shouldRunInterpreter = false;
                    break;
                case 'I' :
                    String s = grabValue(" -I must be followed by a directory name to add to lib path");
                    String[] ls = s.split(java.io.File.pathSeparator);
                    for(int i=0;i<ls.length;i++) {
                        loadPaths.add(ls[i]);
                    }
                    break FOR;
                case 'r' :
                    requiredLibraries.add(grabValue("-r must be followed by a package to require"));
                    break FOR;
                case 'e' :
                    inlineScript.append(grabValue(" -e must be followed by an expression to evaluate"));
                    inlineScript.append('\n');
                    break FOR;
                case 'b' :
                    benchmarking = true;
                    break;
                case 'p' :
                    assumePrinting = true;
                    assumeLoop = true;
                    break;
                case 'O' :
                    objectSpaceEnabled = false;
                    break;
                case 'C' :
                    compilerEnabled = true;
                    break;
                case 'n' :
                    assumeLoop = true;
                    break;
                case 'a' :
                    split = true;
                    break;
                case 'd' :
                    debug = true;
                    verbose = true;
                    break;
                case 'l' :
                    processLineEnds = true;
                    break;
                case 'v' :
                    verbose = true;
                    setShowVersion(true);
                    break;
                case 'w' :
                    verbose = true;
                    break;
                case 'K':
                    // FIXME: No argument seems to work for -K in MRI plus this should not
                    // siphon off additional args 'jruby -K ~/scripts/foo'.  Also better error
                    // processing.
                    String eArg = grabValue("provide a value for -K");
                    
                    if ("u".equals(eArg) || "U".equals(eArg) || "UTF8".equals(eArg)) {
                        encoding = "UTF-8";
                    }
                    
                    break;
                case '-' :
                    if (argument.equals("--version")) {
                        setShowVersion(true);
                        break FOR;
                    } else if(argument.equals("--debug")) {
                        debug = true;
                        verbose = true;
                        break;
                    } else {
                        if (argument.equals("--")) {
                            // ruby interpreter compatibilty 
                            // Usage: ruby [switches] [--] [programfile] [arguments])
                            break FOR;
                        }                    	
                    }
                default :
                    throw new MainExitException(1, "unknown option " + argument.charAt(characterIndex));
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
		
		MainExitException mee = new MainExitException(1, "invalid argument " + argumentIndex + "\n" + errorMessage);
		mee.setUsageError(true);
		
		throw mee;
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
        return isShouldRunInterpreter();
    }
    
    private boolean isSourceFromStdin() {
        return getScriptFileName() == null;
    }

    public Reader getScriptSource() {
        try {
            if (hasInlineScript()) {
                return new StringReader(inlineScript());
            } else if (isSourceFromStdin()) {
                return new InputStreamReader(System.in, encoding);
            } else {
                File file = new File(getScriptFileName());
                return new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
            }
        } catch (IOException e) {
            throw new MainExitException(1, "Error opening script file: " + e.getMessage());
        }
    }

    public String displayedFileName() {
        if (hasInlineScript()) {
            return "-e";
        } else if (isSourceFromStdin()) {
            return "-";
        } else {
            return getScriptFileName();
        }
    }

    private void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public boolean isBenchmarking() {
        return benchmarking;
    }

    public boolean isAssumeLoop() {
        return assumeLoop;
    }

    public boolean isAssumePrinting() {
        return assumePrinting;
    }

    public boolean isProcessLineEnds() {
        return processLineEnds;
    }

    public boolean isSplit() {
        return split;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    protected void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
        this.shouldRunInterpreter = false;
    }

    public String[] getScriptArguments() {
        return scriptArguments;
    }

    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }
    
    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }
    
    public boolean isCompilerEnabled() {
        return compilerEnabled;
    }
    
    public String getEncoding() {
        return encoding;
    }
}
