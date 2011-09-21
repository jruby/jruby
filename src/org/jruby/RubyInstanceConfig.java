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
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

import org.jruby.util.cli.ArgumentProcessor;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTCompiler19;
import org.jruby.exceptions.MainExitException;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.runtime.Constants;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.profile.IProfileData;
import org.jruby.runtime.profile.AbstractProfilePrinter;
import org.jruby.runtime.profile.FlatProfilePrinter;
import org.jruby.runtime.profile.GraphProfilePrinter;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.load.LoadService19;
import org.jruby.util.ClassCache;
import org.jruby.util.JRubyFile;
import org.jruby.util.KCode;
import org.jruby.util.NormalizedFile;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.OutputStrings;
import org.objectweb.asm.Opcodes;

/**
 * A structure used to configure new JRuby instances. All publicly-tweakable
 * aspects of Ruby can be modified here, including those settable by command-
 * line options, those available through JVM properties, and those suitable for
 * embedding.
 */
public class RubyInstanceConfig {
    public RubyInstanceConfig() {
        currentDirectory = Ruby.isSecurityRestricted() ? "/" : JRubyFile.getFileProperty("user.dir");
        samplingEnabled = SafePropertyAccessor.getBoolean("jruby.sampling.enabled", false);

        String compatString = SafePropertyAccessor.getProperty("jruby.compat.version", Constants.DEFAULT_RUBY_VERSION);
        if (compatString.equalsIgnoreCase("RUBY1_8") || compatString.equalsIgnoreCase("1.8")) {
            compatVersion = CompatVersion.RUBY1_8;
        } else if (compatString.equalsIgnoreCase("RUBY1_9") || compatString.equalsIgnoreCase("1.9")) {
            compatVersion = CompatVersion.RUBY1_9;
        } else {
            error.println("Compatibility version `" + compatString + "' invalid; use RUBY1_8 or RUBY1_9. Using RUBY1_8.");
            compatVersion = CompatVersion.RUBY1_8;
        }

        if (Ruby.isSecurityRestricted()) {
            compileMode = CompileMode.OFF;
            jitLogging = false;
            jitDumping = false;
            jitLoggingVerbose = false;
            jitLogEvery = 0;
            jitThreshold = -1;
            jitMax = 0;
            jitMaxSize = -1;
            managementEnabled = false;
        } else {
            String threshold = SafePropertyAccessor.getProperty("jruby.jit.threshold");
            String max = SafePropertyAccessor.getProperty("jruby.jit.max");
            String maxSize = SafePropertyAccessor.getProperty("jruby.jit.maxsize");
            
            if (COMPILE_EXCLUDE != null) {
                String[] elements = COMPILE_EXCLUDE.split(",");
                excludedMethods.addAll(Arrays.asList(elements));
            }
            
            managementEnabled = SafePropertyAccessor.getBoolean("jruby.management.enabled", false);
            runRubyInProcess = SafePropertyAccessor.getBoolean("jruby.launch.inproc", false);
            boolean jitProperty = SafePropertyAccessor.getProperty("jruby.jit.enabled") != null;
            if (jitProperty) {
                error.print("jruby.jit.enabled property is deprecated; use jruby.compile.mode=(OFF|JIT|FORCE) for -C, default, and +C flags");
                compileMode = SafePropertyAccessor.getBoolean("jruby.jit.enabled") ? CompileMode.JIT : CompileMode.OFF;
            } else {
                String jitModeProperty = SafePropertyAccessor.getProperty("jruby.compile.mode", "JIT");

                if (jitModeProperty.equals("OFF")) {
                    compileMode = CompileMode.OFF;
                } else if (jitModeProperty.equals("JIT")) {
                    compileMode = CompileMode.JIT;
                } else if (jitModeProperty.equals("FORCE")) {
                    compileMode = CompileMode.FORCE;
                } else {
                    error.print("jruby.compile.mode property must be OFF, JIT, FORCE, or unset; defaulting to JIT");
                    compileMode = CompileMode.JIT;
                }
            }
            jitLogging = SafePropertyAccessor.getBoolean("jruby.jit.logging");
            jitDumping = SafePropertyAccessor.getBoolean("jruby.jit.dumping");
            jitLoggingVerbose = SafePropertyAccessor.getBoolean("jruby.jit.logging.verbose");
            String logEvery = SafePropertyAccessor.getProperty("jruby.jit.logEvery");
            jitLogEvery = logEvery == null ? 0 : Integer.parseInt(logEvery);
            jitThreshold = threshold == null ?
                    JIT_THRESHOLD : Integer.parseInt(threshold);
            jitMax = max == null ?
                    JIT_MAX_METHODS_LIMIT : Integer.parseInt(max);
            jitMaxSize = maxSize == null ?
                    JIT_MAX_SIZE_LIMIT : Integer.parseInt(maxSize);
        }

        // default ClassCache using jitMax as a soft upper bound
        classCache = new ClassCache<Script>(loader, jitMax);
        threadDumpSignal = SafePropertyAccessor.getProperty("jruby.thread.dump.signal", "USR2");

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
    }
    
    public RubyInstanceConfig(RubyInstanceConfig parentConfig) {
        currentDirectory = parentConfig.getCurrentDirectory();
        samplingEnabled = parentConfig.samplingEnabled;
        compatVersion = parentConfig.compatVersion;
        compileMode = parentConfig.getCompileMode();
        jitLogging = parentConfig.jitLogging;
        jitDumping = parentConfig.jitDumping;
        jitLoggingVerbose = parentConfig.jitLoggingVerbose;
        jitLogEvery = parentConfig.jitLogEvery;
        jitThreshold = parentConfig.jitThreshold;
        jitMax = parentConfig.jitMax;
        jitMaxSize = parentConfig.jitMaxSize;
        managementEnabled = parentConfig.managementEnabled;
        runRubyInProcess = parentConfig.runRubyInProcess;
        excludedMethods = parentConfig.excludedMethods;
        threadDumpSignal = parentConfig.threadDumpSignal;
        updateNativeENVEnabled = parentConfig.updateNativeENVEnabled;
        
        classCache = new ClassCache<Script>(loader, jitMax);

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
    }

    public LoadService createLoadService(Ruby runtime) {
        return creator.create(runtime);
    }

    @Deprecated
    public String getBasicUsageHelp() {
        return OutputStrings.getBasicUsageHelp();
    }

    @Deprecated
    public String getExtendedHelp() {
        return OutputStrings.getExtendedHelp();
    }

    @Deprecated
    public String getPropertyHelp() {
        return OutputStrings.getPropertyHelp();
    }

    @Deprecated
    public String getVersionString() {
        return OutputStrings.getVersionString(compatVersion);
    }

    @Deprecated
    public String getCopyrightString() {
        return OutputStrings.getCopyrightString();
    }

    public void processArguments(String[] arguments) {
        new ArgumentProcessor(arguments, this).processArguments();
        tryProcessArgumentsWithRubyopts();
    }

    public void tryProcessArgumentsWithRubyopts() {
        try {
            // environment defaults to System.getenv normally
            Object rubyoptObj = environment.get("RUBYOPT");
            String rubyopt = rubyoptObj == null ? null : rubyoptObj.toString();
            
            if (rubyopt == null || "".equals(rubyopt)) return;

            if (rubyopt.split("\\s").length != 0) {
                String[] rubyoptArgs = rubyopt.split("\\s+");
                new ArgumentProcessor(rubyoptArgs, false, true, this).processArguments();
            }
        } catch (SecurityException se) {
            // ignore and do nothing
        }
    }

    /**
     * The intent here is to gather up any options that might have
     * been specified in the shebang line and return them so they can
     * be merged into the ones specified on the commandline.  This is
     * kind of a hopeless task because it's impossible to figure out
     * where the command invocation stops and the parameters start.
     * We try to work with the common scenarios where /usr/bin/env is
     * used to invoke the jruby shell script, and skip any parameters
     * it might have.  Then we look for the interpreter invokation and
     * assume that the binary will have the word "ruby" in the name.
     * This is error prone but should cover more cases than the
     * previous code.
     */
    public String[] parseShebangOptions(InputStream in) {
        BufferedReader reader = null;
        String[] result = new String[0];
        if (in == null) return result;
        try {
            in.mark(1024);
            reader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8192);
            String firstLine = reader.readLine();

            // Search for the shebang line in the given stream
            // if it wasn't found on the first line and the -x option
            // was specified
            if (isXFlag()) {
                while (firstLine != null && !isRubyShebangLine(firstLine)) {
                    firstLine = reader.readLine();
                }
            }

            boolean usesEnv = false;
            if (firstLine.length() > 2 && firstLine.charAt(0) == '#' && firstLine.charAt(1) == '!') {
                String[] options = firstLine.substring(2).split("\\s+");
                int i;
                for (i = 0; i < options.length; i++) {
                    // Skip /usr/bin/env if it's first
                    if (i == 0 && options[i].endsWith("/env")) {
                        usesEnv = true;
                        continue;
                    }
                    // Skip any assignments if /usr/bin/env is in play
                    if (usesEnv && options[i].indexOf('=') > 0) {
                        continue;
                    }
                    // Skip any commandline args if /usr/bin/env is in play
                    if (usesEnv && options[i].startsWith("-")) {
                        continue;
                    }
                    String basename = (new File(options[i])).getName();
                    if (basename.indexOf("ruby") > 0) {
                        break;
                    }
                }
                setHasShebangLine(true);
                System.arraycopy(options, i, result, 0, options.length - i);
            } else {
                // No shebang line found
                setHasShebangLine(false);
            }
        } catch (Exception ex) {
            // ignore error
        } finally {
            try {
                in.reset();
            } catch (IOException ex) {}
        }
        return result;
    }
    
    private static final Pattern RUBY_SHEBANG = Pattern.compile("#!.*ruby.*");

    protected static boolean isRubyShebangLine(String line) {
        return RUBY_SHEBANG.matcher(line).matches();
    }
    
    private String calculateJRubyHome() {
        String newJRubyHome = null;
        
        // try the normal property first
        if (!Ruby.isSecurityRestricted()) {
            newJRubyHome = SafePropertyAccessor.getProperty("jruby.home");
        }

        if (newJRubyHome != null) {
            // verify it if it's there
            newJRubyHome = verifyHome(newJRubyHome, error);
        } else {
            try {
                newJRubyHome = SystemPropertyCatcher.findFromJar(this);
            } catch (Exception e) {}

            if (newJRubyHome != null) {
                // verify it if it's there
                newJRubyHome = verifyHome(newJRubyHome, error);
            } else {
                // otherwise fall back on system temp location
                newJRubyHome = SafePropertyAccessor.getProperty("java.io.tmpdir");
            }
        }
        
        return newJRubyHome;
    }

    // We require the home directory to be absolute
    private static String verifyHome(String home, PrintStream error) {
        if (home.equals(".")) {
            home = SafePropertyAccessor.getProperty("user.dir");
        }
        if (home.startsWith("cp:")) {
            home = home.substring(3);
        } else if (!home.startsWith("file:") && !home.startsWith("classpath:")) {
            NormalizedFile f = new NormalizedFile(home);
            if (!f.isAbsolute()) {
                home = f.getAbsolutePath();
            }
            if (!f.exists()) {
                error.println("Warning: JRuby home \"" + f + "\" does not exist, using " + SafePropertyAccessor.getProperty("java.io.tmpdir"));
                return System.getProperty("java.io.tmpdir");
            }
        }
        return home;
    }

    
    public boolean isUpdateNativeENVEnabled() {
        return updateNativeENVEnabled;
    }

    public void setUpdateNativeENVEnabled(boolean updateNativeENVEnabled) {
        this.updateNativeENVEnabled = updateNativeENVEnabled;
    }

    public byte[] inlineScript() {
        return inlineScript.toString().getBytes();
    }

    public InputStream getScriptSource() {
        try {
            // KCode.NONE is used because KCODE does not affect parse in Ruby 1.8
            // if Ruby 2.0 encoding pragmas are implemented, this will need to change
            if (hasInlineScript) {
                return new ByteArrayInputStream(inlineScript());
            } else if (isSourceFromStdin()) {
                // can't use -v and stdin
                if (isShowVersion()) {
                    return null;
                }
                return getInput();
            } else {
                String script = getScriptFileName();
                InputStream stream = null;
                if (script.startsWith("file:") && script.indexOf(".jar!/") != -1) {
                    stream = new URL("jar:" + script).openStream();
                } else if (script.startsWith("classpath:")) {
                    stream = Ruby.getClassLoader().getResourceAsStream(script.substring("classpath:".length()));
                } else {
                    File file = JRubyFile.create(getCurrentDirectory(), getScriptFileName());
                    if (isXFlag()) {
                        // search for a shebang line and
                        // return the script between shebang and __END__ or CTRL-Z (0x1A)
                        return findScript(file);
                    }
                    stream = new FileInputStream(file);
                }

                return new BufferedInputStream(stream, 8192);
            }
        } catch (IOException e) {
            // We haven't found any file directly on the file system,
            // now check for files inside the JARs.
            InputStream is = getJarScriptSource(scriptFileName);
            if (is != null) {
                return new BufferedInputStream(is, 8129);
            }
            throw new MainExitException(1, "Error opening script file: " + e.getMessage());
        }
    }

    private static InputStream findScript(File file) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String currentLine = br.readLine();
        while (currentLine != null && !isRubyShebangLine(currentLine)) {
            currentLine = br.readLine();
        }

        buf.append(currentLine);
        buf.append("\n");

        do {
            currentLine = br.readLine();
            if (currentLine != null) {
            buf.append(currentLine);
            buf.append("\n");
            }
        } while (!(currentLine == null || currentLine.contains("__END__") || currentLine.contains("\026")));
        return new BufferedInputStream(new ByteArrayInputStream(buf.toString().getBytes()), 8192);
    }

    private static InputStream getJarScriptSource(String scriptFileName) {
        boolean looksLikeJarURL = scriptFileName.startsWith("file:") && scriptFileName.indexOf("!/") != -1;
        if (!looksLikeJarURL) {
            return null;
        }

        String before = scriptFileName.substring("file:".length(), scriptFileName.indexOf("!/"));
        String after =  scriptFileName.substring(scriptFileName.indexOf("!/") + 2);

        try {
            JarFile jFile = new JarFile(before);
            JarEntry entry = jFile.getJarEntry(after);

            if (entry != null && !entry.isDirectory()) {
                return jFile.getInputStream(entry);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public String displayedFileName() {
        if (hasInlineScript) {
            if (scriptFileName != null) {
                return scriptFileName;
            } else {
                return "-e";
            }
        } else if (isSourceFromStdin()) {
            return "-";
        } else {
            return getScriptFileName();
        }
    }

    public ASTCompiler newCompiler() {
        if (getCompatVersion() == CompatVersion.RUBY1_8) {
            return new ASTCompiler();
        } else {
            return new ASTCompiler19();
        }
    }
    
    public AbstractProfilePrinter makeDefaultProfilePrinter(IProfileData profileData) {
        if (profilingMode == ProfilingMode.FLAT) {
            return new FlatProfilePrinter(profileData.getResults());
        }
        else if (profilingMode == ProfilingMode.GRAPH) {
            return new GraphProfilePrinter(profileData.getResults());
        }
        return null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Static utilities and global state management methods.
    ////////////////////////////////////////////////////////////////////////////
    
    public static boolean hasLoadedNativeExtensions() {
        return loadedNativeExtensions;
    }
    
    public static void setLoadedNativeExtensions(boolean loadedNativeExtensions) {
        RubyInstanceConfig.loadedNativeExtensions = loadedNativeExtensions;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Getters and setters for config settings.
    ////////////////////////////////////////////////////////////////////////////

    public LoadServiceCreator getLoadServiceCreator() {
        return creator;
    }

    public void setLoadServiceCreator(LoadServiceCreator creator) {
        this.creator = creator;
    }

    public String getJRubyHome() {
        if (jrubyHome == null) {
            jrubyHome = calculateJRubyHome();
        }
        return jrubyHome;
    }

    public void setJRubyHome(String home) {
        jrubyHome = verifyHome(home, error);
    }

    public CompileMode getCompileMode() {
        return compileMode;
    }

    public void setCompileMode(CompileMode compileMode) {
        this.compileMode = compileMode;
    }

    public boolean isJitLogging() {
        return jitLogging;
    }

    public boolean isJitDumping() {
        return jitDumping;
    }

    public boolean isJitLoggingVerbose() {
        return jitLoggingVerbose;
    }

    public int getJitLogEvery() {
        return jitLogEvery;
    }

    public void setJitLogEvery(int jitLogEvery) {
        this.jitLogEvery = jitLogEvery;
    }

    public boolean isSamplingEnabled() {
        return samplingEnabled;
    }

    public int getJitThreshold() {
        return jitThreshold;
    }

    public void setJitThreshold(int jitThreshold) {
        this.jitThreshold = jitThreshold;
    }

    public int getJitMax() {
        return jitMax;
    }

    public void setJitMax(int jitMax) {
        this.jitMax = jitMax;
    }

    public int getJitMaxSize() {
        return jitMaxSize;
    }

    public void setJitMaxSize(int jitMaxSize) {
        this.jitMaxSize = jitMaxSize;
    }

    public boolean isRunRubyInProcess() {
        return runRubyInProcess;
    }

    public void setRunRubyInProcess(boolean flag) {
        this.runRubyInProcess = flag;
    }

    public void setInput(InputStream newInput) {
        input = newInput;
    }

    public InputStream getInput() {
        return input;
    }

    public CompatVersion getCompatVersion() {
        return compatVersion;
    }

    public void setCompatVersion(CompatVersion compatVersion) {
        if (compatVersion == null) compatVersion = CompatVersion.RUBY1_8;

        this.compatVersion = compatVersion;
    }

    public void setOutput(PrintStream newOutput) {
        output = newOutput;
    }

    public PrintStream getOutput() {
        return output;
    }

    public void setError(PrintStream newError) {
        error = newError;
    }

    public PrintStream getError() {
        return error;
    }

    public void setCurrentDirectory(String newCurrentDirectory) {
        currentDirectory = newCurrentDirectory;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setProfile(Profile newProfile) {
        profile = newProfile;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setObjectSpaceEnabled(boolean newObjectSpaceEnabled) {
        objectSpaceEnabled = newObjectSpaceEnabled;
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setEnvironment(Map newEnvironment) {
        if (newEnvironment == null) newEnvironment = new HashMap();
        environment = newEnvironment;
    }

    public Map getEnvironment() {
        return environment;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        // Setting the loader needs to reset the class cache
        if(this.loader != loader) {
            this.classCache = new ClassCache<Script>(loader, this.classCache.getMax());
        }
        this.loader = loader;
    }

    public String[] getArgv() {
        return argv;
    }

    public void setArgv(String[] argv) {
        this.argv = argv;
    }
    
    public StringBuffer getInlineScript() {
        return inlineScript;
    }
    
    public void setHasInlineScript(boolean hasInlineScript) {
        this.hasInlineScript = hasInlineScript;
    }
    
    public boolean hasInlineScript() {
        return hasInlineScript;
    }
    
    public Collection<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    @Deprecated
    public Collection<String> requiredLibraries() {
        return requiredLibraries;
    }
    
    public List<String> getLoadPaths() {
        return loadPaths;
    }

    @Deprecated
    public List<String> loadPaths() {
        return loadPaths;
    }

    public void setLoadPaths(List<String> loadPaths) {
        this.loadPaths = loadPaths;
    }
    
    public void setShouldPrintUsage(boolean shouldPrintUsage) {
        this.shouldPrintUsage = shouldPrintUsage;
    }
    
    public boolean getShouldPrintUsage() {
        return shouldPrintUsage;
    }

    @Deprecated
    public boolean shouldPrintUsage() {
        return shouldPrintUsage;
    }
    
    public void setShouldPrintProperties(boolean shouldPrintProperties) {
        this.shouldPrintProperties = shouldPrintProperties;
    }
    
    public boolean getShouldPrintProperties() {
        return shouldPrintProperties;
    }

    @Deprecated
    public boolean shouldPrintProperties() {
        return shouldPrintProperties;
    }

    public boolean isInlineScript() {
        return hasInlineScript;
    }

    private boolean isSourceFromStdin() {
        return getScriptFileName() == null;
    }

    public void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }
    
    public void setBenchmarking(boolean benchmarking) {
        this.benchmarking = benchmarking;
    }

    public boolean isBenchmarking() {
        return benchmarking;
    }
    
    public void setAssumeLoop(boolean assumeLoop) {
        this.assumeLoop = assumeLoop;
    }

    public boolean isAssumeLoop() {
        return assumeLoop;
    }
    
    public void setAssumePrinting(boolean assumePrinting) {
        this.assumePrinting = assumePrinting;
    }

    public boolean isAssumePrinting() {
        return assumePrinting;
    }
    
    public void setProcessLineEnds(boolean processLineEnds) {
        this.processLineEnds = processLineEnds;
    }

    public boolean isProcessLineEnds() {
        return processLineEnds;
    }
    
    public void setSplit(boolean split) {
        this.split = split;
    }

    public boolean isSplit() {
        return split;
    }
    
    public Verbosity getVerbosity() {
        return verbosity;
    }
    
    public void setVerbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    public boolean isVerbose() {
        return verbosity == Verbosity.TRUE;
    }

    @Deprecated
    public Boolean getVerbose() {
        return isVerbose();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isParserDebug() {
        return parserDebug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }
    
    public boolean isShowBytecode() {
        return showBytecode;
    }

    public boolean isShowCopyright() {
        return showCopyright;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }
    
    public void setShowBytecode(boolean showBytecode) {
        this.showBytecode = showBytecode;
    }

    public void setShowCopyright(boolean showCopyright) {
        this.showCopyright = showCopyright;
    }
    
    public void setShouldRunInterpreter(boolean shouldRunInterpreter) {
        this.shouldRunInterpreter = shouldRunInterpreter;
    }
    
    public boolean getShouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    @Deprecated
    public boolean shouldRunInterpreter() {
        return isShouldRunInterpreter();
    }

    @Deprecated
    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }
    
    public void setShouldCheckSyntax(boolean shouldSetSyntax) {
        this.shouldCheckSyntax = shouldSetSyntax;
    }

    public boolean getShouldCheckSyntax() {
        return shouldCheckSyntax;
    }
    
    public void setInputFieldSeparator(String inputFieldSeparator) {
        this.inputFieldSeparator = inputFieldSeparator;
    }

    public String getInputFieldSeparator() {
        return inputFieldSeparator;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }
    
    public void setInternalEncoding(String internalEncoding) {
        this.internalEncoding = internalEncoding;
    }

    public String getInternalEncoding() {
        return internalEncoding;
    }
    
    public void setExternalEncoding(String externalEncoding) {
        this.externalEncoding = externalEncoding;
    }

    public String getExternalEncoding() {
        return externalEncoding;
    }
    
    public void setRecordSeparator(String recordSeparator) {
        this.recordSeparator = recordSeparator;
    }

    public String getRecordSeparator() {
        return recordSeparator;
    }
    
    public void setSafeLevel(int safeLevel) {
        this.safeLevel = safeLevel;
    }

    public int getSafeLevel() {
        return safeLevel;
    }

    public ClassCache getClassCache() {
        return classCache;
    }
    
    public void setInPlaceBackupExtension(String inPlaceBackupExtension) {
        this.inPlaceBackupExtension = inPlaceBackupExtension;
    }

    public String getInPlaceBackupExtension() {
        return inPlaceBackupExtension;
    }

    @Deprecated
    public String getInPlaceBackupExtention() {
        return inPlaceBackupExtension;
    }

    public void setClassCache(ClassCache classCache) {
        this.classCache = classCache;
    }

    public Map getOptionGlobals() {
        return optionGlobals;
    }
    
    public boolean isManagementEnabled() {
        return managementEnabled;
    }
    
    public Set getExcludedMethods() {
        return excludedMethods;
    }
    
    public void setParserDebug(boolean parserDebug) {
        this.parserDebug = parserDebug;
    }
    
    public boolean getParserDebug() {
        return parserDebug;
    }

    public boolean isArgvGlobalsOn() {
        return argvGlobalsOn;
    }

    public void setArgvGlobalsOn(boolean argvGlobalsOn) {
        this.argvGlobalsOn = argvGlobalsOn;
    }

    public String getThreadDumpSignal() {
        return threadDumpSignal;
    }

    public boolean isHardExit() {
        return hardExit;
    }

    public void setHardExit(boolean hardExit) {
        this.hardExit = hardExit;
    }

    public boolean isProfiling() {
        return profilingMode != ProfilingMode.OFF;
    }
    
    public boolean isProfilingEntireRun() {
        return profilingMode != ProfilingMode.OFF && profilingMode != ProfilingMode.API;
    }
    
    public void setProfilingMode(ProfilingMode profilingMode) {
        this.profilingMode = profilingMode;
    }

    public ProfilingMode getProfilingMode() {
        return profilingMode;
    }

    public boolean hasShebangLine() {
        return hasShebangLine;
    }

    public void setHasShebangLine(boolean hasShebangLine) {
        this.hasShebangLine = hasShebangLine;
    }

    public boolean isDisableGems() {
        return disableGems;
    }

    public void setDisableGems(boolean dg) {
        this.disableGems = dg;
    }

    public TraceType getTraceType() {
        return traceType;
    }

    public void setTraceType(TraceType traceType) {
        this.traceType = traceType;
    }
    
    /**
     * Set whether native code is enabled for this config. Disabling it also
     * disables C extensions (@see RubyInstanceConfig#setCextEnabled).
     * 
     * @param b new value indicating whether native code is enabled
     */
    public void setNativeEnabled(boolean b) {
        _nativeEnabled = false;
    }
    
    /**
     * Get whether native code is enabled for this config.
     * 
     * @return true if native code is enabled; false otherwise.
     */
    public boolean isNativeEnabled() {
        return _nativeEnabled;
    }
    
    /**
     * Set whether C extensions are enabled for this config.
     * 
     * @param b new value indicating whether native code is enabled
     */
    public void setCextEnabled(boolean b) {
        _cextEnabled = b;
    }
    
    /**
     * Get whether C extensions are enabled for this config.
     * 
     * @return true if C extensions are enabled; false otherwise.
     */
    public boolean isCextEnabled() {
        return _cextEnabled;
    }
    
    public void setXFlag(boolean xFlag) {
        this.xFlag = xFlag;
    }

    public boolean isXFlag() {
        return xFlag;
    }
    
    @Deprecated
    public boolean isxFlag() {
        return xFlag;
    }
    
    /**
     * True if colorized backtraces are enabled. False otherwise.
     */
    public boolean getBacktraceColor() {
        return backtraceColor;
    }
    
    /**
     * Set to true to enable colorized backtraces.
     */
    public void setBacktraceColor(boolean backtraceColor) {
        this.backtraceColor = backtraceColor;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Configuration fields.
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Indicates whether the script must be extracted from script source
     */
    private boolean xFlag;

    /**
     * Indicates whether the script has a shebang line or not
     */
    private boolean hasShebangLine;
    private InputStream input          = System.in;
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;
    private Profile profile            = Profile.DEFAULT;
    private boolean objectSpaceEnabled
            = SafePropertyAccessor.getBoolean("jruby.objectspace.enabled", false);

    private CompileMode compileMode = CompileMode.JIT;
    private boolean runRubyInProcess   = true;
    private String currentDirectory;

    /** Environment variables; defaults to System.getenv() in constructor */
    private Map environment;
    private String[] argv = {};

    private final boolean jitLogging;
    private final boolean jitDumping;
    private final boolean jitLoggingVerbose;
    private int jitLogEvery;
    private int jitThreshold;
    private int jitMax;
    private int jitMaxSize;
    private final boolean samplingEnabled;
    private CompatVersion compatVersion;

    private String internalEncoding = null;
    private String externalEncoding = null;
		
    private ProfilingMode profilingMode = ProfilingMode.OFF;
    
    private ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    private ClassLoader loader = contextLoader == null ? RubyInstanceConfig.class.getClassLoader() : contextLoader;

    private ClassCache<Script> classCache;

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<String>();
    private Set<String> excludedMethods = new HashSet<String>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private Collection<String> requiredLibraries = new LinkedHashSet<String>();
    private boolean benchmarking = false;
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private Map optionGlobals = new HashMap();
    private boolean processLineEnds = false;
    private boolean split = false;
    private Verbosity verbosity = Verbosity.FALSE;
    private boolean debug = false;
    private boolean showVersion = false;
    private boolean showBytecode = false;
    private boolean showCopyright = false;
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = false;
    private boolean shouldPrintProperties=false;
    private KCode kcode = KCode.NONE;
    private String recordSeparator = "\n";
    private boolean shouldCheckSyntax = false;
    private String inputFieldSeparator = null;
    private boolean managementEnabled = false;
    private String inPlaceBackupExtension = null;
    private boolean parserDebug = false;
    private String threadDumpSignal = null;
    private boolean hardExit = false;
    private boolean disableGems = false;
    private boolean updateNativeENVEnabled = true;
    
    private int safeLevel = 0;

    private String jrubyHome;
    
    /**
     * Whether native code is enabled for this configuration.
     */
    private boolean _nativeEnabled = NATIVE_ENABLED;
    
    /**
     * Whether C extensions are enabled for this configuration.
     */
    private boolean _cextEnabled = CEXT_ENABLED;

    private TraceType traceType =
            TraceType.traceTypeFor(SafePropertyAccessor.getProperty("jruby.backtrace.style", "ruby_framed"));
    
    private boolean backtraceColor = SafePropertyAccessor.getBoolean("jruby.backtrace.color", false);

    private LoadServiceCreator creator = LoadServiceCreator.DEFAULT;
    
    ////////////////////////////////////////////////////////////////////////////
    // Support classes, etc.
    ////////////////////////////////////////////////////////////////////////////
    
    public enum Verbosity { NIL, FALSE, TRUE }

    public static interface LoadServiceCreator {
        LoadService create(Ruby runtime);

        LoadServiceCreator DEFAULT = new LoadServiceCreator() {
                public LoadService create(Ruby runtime) {
                    if (runtime.is1_9()) {
                        return new LoadService19(runtime);
                    }
                    return new LoadService(runtime);
                }
            };
    }

    public enum ProfilingMode {
		OFF, API, FLAT, GRAPH
	}

    public enum CompileMode {
        JIT, FORCE, OFF, OFFIR;

        public boolean shouldPrecompileCLI() {
            switch (this) {
            case JIT: case FORCE:
                if (DYNOPT_COMPILE_ENABLED) {
                    // don't precompile the CLI script in dynopt mode
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean shouldJIT() {
            switch (this) {
            case JIT: case FORCE:
                return true;
            }
            return false;
        }

        public boolean shouldPrecompileAll() {
            return this == FORCE;
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Static configuration fields, used as defaults for new JRuby instances.
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * The max count of active methods eligible for JIT-compilation.
     */
    public static final int JIT_MAX_METHODS_LIMIT = 4096;

    /**
     * The max size of JIT-compiled methods (full class size) allowed.
     */
    public static final int JIT_MAX_SIZE_LIMIT = 30000;

    /**
     * The JIT threshold to the specified method invocation count.
     */
    public static final int JIT_THRESHOLD = 50;
    
    /**
     * The version to use for generated classes. Set to current JVM version by default
     */
    public static final int JAVA_VERSION = initGlobalJavaVersion();
    
    /**
     * Default size for chained compilation.
     */
    public static final int CHAINED_COMPILE_LINE_COUNT_DEFAULT = 500;
    
    /**
     * The number of lines at which a method, class, or block body is split into
     * chained methods (to dodge 64k method-size limit in JVM).
     */
    public static final int CHAINED_COMPILE_LINE_COUNT
            = SafePropertyAccessor.getInt("jruby.compile.chainsize", CHAINED_COMPILE_LINE_COUNT_DEFAULT);

    public static final boolean PEEPHOLE_OPTZ
            = SafePropertyAccessor.getBoolean("jruby.compile.peephole", true);
    public static boolean DYNOPT_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.dynopt", false);
    public static boolean NOGUARDS_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.noguards");
    public static boolean FASTEST_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.fastest");
    public static boolean FASTOPS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.fastops", true);
    public static boolean THREADLESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.threadless");
    public static boolean FASTSEND_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.fastsend");
    public static boolean LAZYHANDLES_COMPILE = SafePropertyAccessor.getBoolean("jruby.compile.lazyHandles", false);
    public static boolean INLINE_DYNCALL_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.inlineDyncalls");
    public static boolean FAST_MULTIPLE_ASSIGNMENT
            = SafePropertyAccessor.getBoolean("jruby.compile.fastMasgn", false);
    
    public static final boolean POOLING_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.thread.pool.enabled");
    public static final int POOL_MAX
            = SafePropertyAccessor.getInt("jruby.thread.pool.max", Integer.MAX_VALUE);
    public static final int POOL_MIN
            = SafePropertyAccessor.getInt("jruby.thread.pool.min", 0);
    public static final int POOL_TTL
            = SafePropertyAccessor.getInt("jruby.thread.pool.ttl", 60);

    public static final boolean NATIVE_NET_PROTOCOL
            = SafePropertyAccessor.getBoolean("jruby.native.net.protocol", false);

    public static boolean FULL_TRACE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.debug.fullTrace", false);

    public static final String COMPILE_EXCLUDE
            = SafePropertyAccessor.getProperty("jruby.jit.exclude");
    
    /**
     * Indicates the global default for whether native code is enabled. Default
     * is true or the value of the property "jruby.native.enabled", if set.
     * 
     * This value is used to default new runtime configurations.
     */
    public static final boolean NATIVE_ENABLED = SafePropertyAccessor.getBoolean("jruby.native.enabled", true);
    
    @Deprecated
    public static final boolean nativeEnabled = NATIVE_ENABLED;
    
    /**
     * Indicates the global default for whether C extensions are enabled. Default
     * is the value of RubyInstanceConfig.NATIVE_ENABLED or the value of the
     * property "jruby.cext.enabled", if set.
     * 
     * This value is used to default new runtime configurations.
     */
    public final static boolean CEXT_ENABLED = SafePropertyAccessor.getBoolean("jruby.cext.enabled", NATIVE_ENABLED);

    public static final boolean REIFY_RUBY_CLASSES
            = SafePropertyAccessor.getBoolean("jruby.reify.classes", false);

    public static final boolean REIFY_LOG_ERRORS
            = SafePropertyAccessor.getBoolean("jruby.reify.logErrors", false);

    public static final boolean USE_GENERATED_HANDLES
            = SafePropertyAccessor.getBoolean("jruby.java.handles", false);

    public static final boolean DEBUG_LOAD_SERVICE
            = SafePropertyAccessor.getBoolean("jruby.debug.loadService", false);

    public static final boolean DEBUG_LOAD_TIMINGS
            = SafePropertyAccessor.getBoolean("jruby.debug.loadService.timing", false);

    public static final boolean DEBUG_LAUNCHING
            = SafePropertyAccessor.getBoolean("jruby.debug.launch", false);

    public static final boolean DEBUG_SCRIPT_RESOLUTION
            = SafePropertyAccessor.getBoolean("jruby.debug.scriptResolution", false);

    public static final boolean JUMPS_HAVE_BACKTRACE
            = SafePropertyAccessor.getBoolean("jruby.jump.backtrace", false);

    public static final boolean JIT_CACHE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.jit.cache", true);

    public static final String JIT_CODE_CACHE
            = SafePropertyAccessor.getProperty("jruby.jit.codeCache", null);

    public static final boolean REFLECTED_HANDLES
            = SafePropertyAccessor.getBoolean("jruby.reflected.handles", false)
            || SafePropertyAccessor.getBoolean("jruby.reflection", false);

    public static final boolean NO_UNWRAP_PROCESS_STREAMS
            = SafePropertyAccessor.getBoolean("jruby.process.noUnwrap", false);

    public static final boolean INTERFACES_USE_PROXY
            = SafePropertyAccessor.getBoolean("jruby.interfaces.useProxy");

    public static final boolean JIT_LOADING_DEBUG = SafePropertyAccessor.getBoolean("jruby.jit.debug", false);

    public static final boolean CAN_SET_ACCESSIBLE = SafePropertyAccessor.getBoolean("jruby.ji.setAccessible", true);
    
    public static final boolean USE_INVOKEDYNAMIC =
            JAVA_VERSION == Opcodes.V1_7
            && SafePropertyAccessor.getBoolean("jruby.compile.invokedynamic", true);
    
    // max times an indy call site can fail before it goes to simple IC
    public static final int MAX_FAIL_COUNT = SafePropertyAccessor.getInt("jruby.invokedynamic.maxfail", 2);
    
    // logging of various indy aspects
    public static final boolean LOG_INDY_BINDINGS = SafePropertyAccessor.getBoolean("jruby.invokedynamic.log.binding");
    public static final boolean LOG_INDY_CONSTANTS = SafePropertyAccessor.getBoolean("jruby.invokedynamic.log.constants");
    
    // properties enabling or disabling certain uses of invokedynamic
    public static final boolean INVOKEDYNAMIC_ALL = USE_INVOKEDYNAMIC && (SafePropertyAccessor.getBoolean("jruby.invokedynamic.all", false));
    public static final boolean INVOKEDYNAMIC_SAFE = USE_INVOKEDYNAMIC && (SafePropertyAccessor.getBoolean("jruby.invokedynamic.safe", false));
    
    public static final boolean INVOKEDYNAMIC_INVOCATION = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && SafePropertyAccessor.getBoolean("jruby.invokedynamic.invocation", true);
    public static final boolean INVOKEDYNAMIC_INVOCATION_SWITCHPOINT = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && SafePropertyAccessor.getBoolean("jruby.invokedynamic.invocation.switchpoint", false);
    public static final boolean INVOKEDYNAMIC_INDIRECT = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_INVOCATION && SafePropertyAccessor.getBoolean("jruby.invokedynamic.indirect", false);
    public static final boolean INVOKEDYNAMIC_JAVA = INVOKEDYNAMIC_ALL ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_INVOCATION && SafePropertyAccessor.getBoolean("jruby.invokedynamic.java", false);
    public static final boolean INVOKEDYNAMIC_ATTR = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_INVOCATION && SafePropertyAccessor.getBoolean("jruby.invokedynamic.attr", true);
    public static final boolean INVOKEDYNAMIC_FASTOPS = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_INVOCATION && SafePropertyAccessor.getBoolean("jruby.invokedynamic.fastops", true);
    
    public static final boolean INVOKEDYNAMIC_CACHE = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && SafePropertyAccessor.getBoolean("jruby.invokedynamic.cache", true);
    public static final boolean INVOKEDYNAMIC_CONSTANTS = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_CACHE && SafePropertyAccessor.getBoolean("jruby.invokedynamic.constants", false);
    public static final boolean INVOKEDYNAMIC_LITERALS = INVOKEDYNAMIC_ALL || INVOKEDYNAMIC_SAFE ||
            USE_INVOKEDYNAMIC && INVOKEDYNAMIC_CACHE && SafePropertyAccessor.getBoolean("jruby.invokedynamic.literals", true);
    
    // properties for logging exceptions, backtraces, and caller invocations
    public static final boolean LOG_EXCEPTIONS = SafePropertyAccessor.getBoolean("jruby.log.exceptions");
    public static final boolean LOG_BACKTRACES = SafePropertyAccessor.getBoolean("jruby.log.backtraces");
    public static final boolean LOG_CALLERS = SafePropertyAccessor.getBoolean("jruby.log.callers");
    
    public static final boolean ERRNO_BACKTRACE
            = SafePropertyAccessor.getBoolean("jruby.errno.backtrace", false);
    
    public static final boolean IR_DEBUG = SafePropertyAccessor.getBoolean("jruby.ir.debug");
    public static final boolean IR_COMPILER_DEBUG = SafePropertyAccessor.getBoolean("jruby.ir.compiler.debug");    
    public static final boolean IR_LIVE_VARIABLE = SafePropertyAccessor.getBoolean("jruby.ir.pass.live_variable");
    public static final boolean IR_DEAD_CODE = SafePropertyAccessor.getBoolean("jruby.ir.pass.dead_code");
    public static final String IR_TEST_INLINER = SafePropertyAccessor.getProperty("jruby.ir.pass.test_inliner");
    
    public static final boolean COROUTINE_FIBERS = SafePropertyAccessor.getBoolean("jruby.fiber.coroutines");
    
    private static volatile boolean loadedNativeExtensions = false;
    
    ////////////////////////////////////////////////////////////////////////////
    // Static initializers
    ////////////////////////////////////////////////////////////////////////////
    
    private static int initGlobalJavaVersion() {
        String specVersion = null;
        try {
            specVersion = System.getProperty("jruby.bytecode.version");
            if (specVersion == null) {
                specVersion = System.getProperty("java.specification.version");
            }
        } catch (SecurityException se) {
            specVersion = "1.5";
        }
        
        // stack map calculation is failing for some compilation scenarios, so
        // forcing both 1.5 and 1.6 to use 1.5 bytecode for the moment.
        if (specVersion.equals("1.5")) {// || specVersion.equals("1.6")) {
           return Opcodes.V1_5;
        } else if (specVersion.equals("1.6")) {
            return Opcodes.V1_6;
        } else if (specVersion.equals("1.7") || specVersion.equals("1.8")) {
            return Opcodes.V1_7;
        } else {
            throw new RuntimeException("unsupported Java version: " + specVersion);
        }
    }
}
