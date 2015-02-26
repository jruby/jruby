/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import jnr.posix.util.Platform;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.Constants;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.profile.builtin.ProfileOutput;
import org.jruby.util.InputStreamMarkCursor;
import org.jruby.util.JRubyFile;
import org.jruby.util.KCode;
import org.jruby.util.NormalizedFile;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.ArgumentProcessor;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;
import org.objectweb.asm.Opcodes;

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
import java.math.BigDecimal;
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

/**
 * A structure used to configure new JRuby instances. All publicly-tweakable
 * aspects of Ruby can be modified here, including those settable by command-
 * line options, those available through JVM properties, and those suitable for
 * embedding.
 */
public class RubyInstanceConfig {
    public RubyInstanceConfig() {
        currentDirectory = Ruby.isSecurityRestricted() ? "/" : JRubyFile.getFileProperty("user.dir");

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
            if (COMPILE_EXCLUDE != null) {
                String[] elements = COMPILE_EXCLUDE.split(",");
                excludedMethods.addAll(Arrays.asList(elements));
            }

            managementEnabled = Options.MANAGEMENT_ENABLED.load();
            runRubyInProcess = Options.LAUNCH_INPROC.load();
            compileMode = Options.COMPILE_MODE.load();

            jitLogging = Options.JIT_LOGGING.load();
            jitDumping = Options.JIT_DUMPING.load();
            jitLoggingVerbose = Options.JIT_LOGGING_VERBOSE.load();
            jitLogEvery = Options.JIT_LOGEVERY.load();
            jitThreshold = Options.JIT_THRESHOLD.load();
            jitMax = Options.JIT_MAX.load();
            jitMaxSize = Options.JIT_MAXSIZE.load();
        }

        threadDumpSignal = Options.THREAD_DUMP_SIGNAL.load();

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
    }

    public RubyInstanceConfig(RubyInstanceConfig parentConfig) {
        currentDirectory = parentConfig.getCurrentDirectory();
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

        profilingService = parentConfig.profilingService;
        profilingMode = parentConfig.profilingMode;

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
    }

    public RubyInstanceConfig(final InputStream in, final PrintStream out, final PrintStream err) {
        this();
        setInput(in);
        setOutput(out);
        setError(err);
    }

    public LoadService createLoadService(Ruby runtime) {
        return creator.create(runtime);
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
                new ArgumentProcessor(rubyoptArgs, false, true, true, this).processArguments();
            }
        } catch (SecurityException se) {
            // ignore and do nothing
        }
    }

    // This method does not work like previous version in verifying it is
    // a Ruby shebang line.  Looking for ruby before \n is possible to add,
    // but I wanted to keep this short.
    private boolean isShebang(InputStreamMarkCursor cursor) throws IOException {
        if (cursor.read() == '#') {
            int c = cursor.read();
            if (c == '!') {
                cursor.endPoint(-2);
                return true;
            } else if (c == '\n') {
                cursor.rewind();
            }
        } else {
            cursor.rewind();
        }

        return false;
    }

    private boolean skipToNextLine(InputStreamMarkCursor cursor) throws IOException {
        int c = cursor.read();
        do {
            if (c == '\n') return true;
        } while ((c = cursor.read()) != -1);

        return false;
    }

    private void eatToShebang(InputStream in) {
        InputStreamMarkCursor cursor = new InputStreamMarkCursor(in, 8192);
        try {
            do {
                if (isShebang(cursor)) break;
		    } while (skipToNextLine(cursor));
        } catch (IOException e) {
        } finally {
            try { cursor.finish(); } catch (IOException e) {}
        }
    }

    /**
     * The intent here is to gather up any options that might have
     * been specified in the shebang line and return them so they can
     * be merged into the ones specified on the command-line.  This is
     * kind of a hopeless task because it's impossible to figure out
     * where the command invocation stops and the parameters start.
     * We try to work with the common scenarios where /usr/bin/env is
     * used to invoke the JRuby shell script, and skip any parameters
     * it might have.  Then we look for the interpreter invocation and
     * assume that the binary will have the word "ruby" in the name.
     * This is error prone but should cover more cases than the
     * previous code.
     */
    public String[] parseShebangOptions(InputStream in) {
        BufferedReader reader = null;
        String[] result = new String[0];
        if (in == null) return result;

        if (isXFlag()) eatToShebang(in);

        try {
            InputStreamMarkCursor cursor = new InputStreamMarkCursor(in, 8192);
            try {
                if (!isShebang(cursor)) return result;
            } finally {
                cursor.finish();
            }

            in.mark(8192);
            reader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8192);
            String firstLine = reader.readLine();

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
                    if (usesEnv && options[i].indexOf('=') > 0) continue;

                    // Skip any commandline args if /usr/bin/env is in play
                    if (usesEnv && options[i].startsWith("-")) continue;

                    String basename = (new File(options[i])).getName();
                    if (basename.indexOf("ruby") > 0) break;
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
                newJRubyHome = SystemPropertyCatcher.findJRubyHome(this);
            } catch (Exception e) {}

            if (newJRubyHome != null) {
                // verify it if it's there
                newJRubyHome = verifyHome(newJRubyHome, error);
            } else {
                // otherwise fall back on system temp location
                newJRubyHome = SafePropertyAccessor.getProperty("java.io.tmpdir");
            }
        }

        // RegularFileResource absolutePath will canonicalize resources so that will change c: paths to C:.
        // We will cannonicalize on windows so that jruby.home is also C:.
        // assume all those uri-like pathnames are already in absolute form
        if (Platform.IS_WINDOWS && !newJRubyHome.startsWith("jar:") && !newJRubyHome.startsWith("file:") && !newJRubyHome.startsWith("classpath:") && !newJRubyHome.startsWith("uri:")) {
            File file = new File(newJRubyHome);

            try {
                newJRubyHome = file.getCanonicalPath();
            } catch (IOException e) {} // just let newJRubyHome stay the way it is if this fails
        }

        return newJRubyHome;
    }

    // We require the home directory to be absolute
    private static String verifyHome(String home, PrintStream error) {
        if ("uri:classloader://META-INF/jruby.home".equals(home) || "uri:classloader:/META-INF/jruby.home".equals(home)) {
            return home;
        }
        if (home.equals(".")) {
            home = SafePropertyAccessor.getProperty("user.dir");
        }
        else if (home.startsWith("cp:")) {
            home = home.substring(3);
        }
        if (home.startsWith("jar:") || ( home.startsWith("file:") && home.contains(".jar!/") ) ||
                home.startsWith("classpath:") || home.startsWith("uri:")) {
            error.println("Warning: JRuby home with uri like pathes may not have full functionality - use at your own risk");
        }
        // do not normalize on plain jar like pathes coming from jruby-rack
        else if (!home.contains(".jar!/") && !home.startsWith("uri:")) {
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

    /** Indicates whether the JVM process' native environment will be updated when ENV[...] is set from Ruby. */
    public boolean isUpdateNativeENVEnabled() {
        return updateNativeENVEnabled;
    }

    /** Ensure that the JVM process' native environment will be updated when ENV is modified .*/
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
            } else if (isForceStdin() || getScriptFileName() == null) {
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
                    stream = getScriptSourceFromJar(script);
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

    private InputStream getScriptSourceFromJar(String script) {
        return Ruby.getClassLoader().getResourceAsStream(script.substring("classpath:".length()));
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
        } else if (isForceStdin() || getScriptFileName() == null) {
            return "-";
        } else {
            return getScriptFileName();
        }
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

    /**
     * @see Options#JIT_LOGGING
     */
    public boolean isJitLogging() {
        return jitLogging;
    }

    /**
     * @see Options#JIT_DUMPING
     */
    public boolean isJitDumping() {
        return jitDumping;
    }

    /**
     * @see Options#JIT_LOGGING_VERBOSE
     */
    public boolean isJitLoggingVerbose() {
        return jitLoggingVerbose;
    }

    /**
     * @see Options#JIT_LOGEVERY
     */
    public int getJitLogEvery() {
        return jitLogEvery;
    }

    /**
     * @see Options#JIT_LOGEVERY
     */
    public void setJitLogEvery(int jitLogEvery) {
        this.jitLogEvery = jitLogEvery;
    }

    /**
     * @see Options#JIT_THRESHOLD
     */
    public int getJitThreshold() {
        return jitThreshold;
    }

    /**
     * @see Options#JIT_THRESHOLD
     */
    public void setJitThreshold(int jitThreshold) {
        this.jitThreshold = jitThreshold;
    }

    /**
     * @see Options#JIT_MAX
     */
    public int getJitMax() {
        return jitMax;
    }

    /**
     * @see Options#JIT_MAX
     */
    public void setJitMax(int jitMax) {
        this.jitMax = jitMax;
    }

    /**
     * @see Options#JIT_MAXSIZE
     */
    public int getJitMaxSize() {
        return jitMaxSize;
    }

    /**
     * @see Options#JIT_MAXSIZE
     */
    public void setJitMaxSize(int jitMaxSize) {
        this.jitMaxSize = jitMaxSize;
    }

    /**
     * @see Options#LAUNCH_INPROC
     */
    public boolean isRunRubyInProcess() {
        return runRubyInProcess;
    }

    /**
     * @see Options#LAUNCH_INPROC
     */
    public void setRunRubyInProcess(boolean flag) {
        this.runRubyInProcess = flag;
    }

    public void setInput(InputStream newInput) {
        input = newInput;
    }

    public InputStream getInput() {
        return input;
    }

    @Deprecated
    public CompatVersion getCompatVersion() {
        return CompatVersion.RUBY2_1;
    }

    @Deprecated
    public void setCompatVersion(CompatVersion compatVersion) {
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

    /**
     * @see Options#OBJECTSPACE_ENABLED
     */
    public void setObjectSpaceEnabled(boolean newObjectSpaceEnabled) {
        objectSpaceEnabled = newObjectSpaceEnabled;
    }

    /**
     * @see Options#OBJECTSPACE_ENABLED
     */
    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    /**
     * @see Options#SIPHASH_ENABLED
     */
    public void setSiphashEnabled(boolean newSiphashEnabled) {
        siphashEnabled = newSiphashEnabled;
    }

    /**
     * @see Options#SIPHASH_ENABLED
     */
    public boolean isSiphashEnabled() {
        return siphashEnabled;
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
        this.hasScriptArgv = true;
        this.hasInlineScript = hasInlineScript;
    }

    public boolean hasInlineScript() {
        return hasInlineScript;
    }

    public Collection<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public List<String> getLoadPaths() {
        return loadPaths;
    }

    public void setLoadPaths(List<String> loadPaths) {
        this.loadPaths = loadPaths;
    }

    /**
     * @see Options#CLI_HELP
     */
    public void setShouldPrintUsage(boolean shouldPrintUsage) {
        this.shouldPrintUsage = shouldPrintUsage;
    }

    /**
     * @see Options#CLI_HELP
     */
    public boolean getShouldPrintUsage() {
        return shouldPrintUsage;
    }

    /**
     * @see Options#CLI_PROPERTIES
     */
    public void setShouldPrintProperties(boolean shouldPrintProperties) {
        this.shouldPrintProperties = shouldPrintProperties;
    }

    /**
     * @see Options#CLI_PROPERTIES
     */
    public boolean getShouldPrintProperties() {
        return shouldPrintProperties;
    }

    public boolean isInlineScript() {
        return hasInlineScript;
    }

    /**
     * True if we are only using source from stdin and not from a -e or file argument.
     */
    public boolean isForceStdin() {
        return forceStdin;
    }

    /**
     * Set whether we should only look at stdin for source.
     */
    public void setForceStdin(boolean forceStdin) {
        this.forceStdin = forceStdin;
    }

    public void setScriptFileName(String scriptFileName) {
        this.hasScriptArgv = true;
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    /**
     * @see Options#CLI_ASSUME_LOOP
     */
    public void setAssumeLoop(boolean assumeLoop) {
        this.assumeLoop = assumeLoop;
    }

    /**
     * @see Options#CLI_ASSUME_LOOP
     */
    public boolean isAssumeLoop() {
        return assumeLoop;
    }

    /**
     * @see Options#CLI_ASSUME_PRINT
     */
    public void setAssumePrinting(boolean assumePrinting) {
        this.assumePrinting = assumePrinting;
    }

    /**
     * @see Options#CLI_ASSUME_PRINT
     */
    public boolean isAssumePrinting() {
        return assumePrinting;
    }

    /**
     * @see Options#CLI_PROCESS_LINE_ENDS
     */
    public void setProcessLineEnds(boolean processLineEnds) {
        this.processLineEnds = processLineEnds;
    }

    /**
     * @see Options#CLI_PROCESS_LINE_ENDS
     */
    public boolean isProcessLineEnds() {
        return processLineEnds;
    }

    /**
     * @see Options#CLI_AUTOSPLIT
     */
    public void setSplit(boolean split) {
        this.split = split;
    }

    /**
     * @see Options#CLI_AUTOSPLIT
     */
    public boolean isSplit() {
        return split;
    }

    /**
     * @see Options#CLI_WARNING_LEVEL
     */
    public Verbosity getVerbosity() {
        return verbosity;
    }

    /**
     * @see Options#CLI_WARNING_LEVEL
     */
    public void setVerbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    /**
     * @see Options#CLI_VERBOSE
     */
    public boolean isVerbose() {
        return verbosity == Verbosity.TRUE;
    }

    /**
     * @see Options#CLI_DEBUG
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @see Options#CLI_DEBUG
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @see Options#CLI_PARSER_DEBUG
     */
    public boolean isParserDebug() {
        return parserDebug;
    }

    /**
     * @see Options#CLI_PARSER_DEBUG
     */
    public void setParserDebug(boolean parserDebug) {
        this.parserDebug = parserDebug;
    }

    /**
     * @see Options#CLI_PARSER_DEBUG
     */
    public boolean getParserDebug() {
        return parserDebug;
    }

    /**
     * @see Options#CLI_VERSION
     */
    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    /**
     * @see Options#CLI_VERSION
     */
    public boolean isShowVersion() {
        return showVersion;
    }

    /**
     * @see Options#CLI_BYTECODE
     */
    public void setShowBytecode(boolean showBytecode) {
        this.showBytecode = showBytecode;
    }

    /**
     * @see Options#CLI_BYTECODE
     */
    public boolean isShowBytecode() {
        return showBytecode;
    }

    /**
     * @see Options#CLI_COPYRIGHT
     */
    public void setShowCopyright(boolean showCopyright) {
        this.showCopyright = showCopyright;
    }

    /**
     * @see Options#CLI_COPYRIGHT
     */
    public boolean isShowCopyright() {
        return showCopyright;
    }

    public void setShouldRunInterpreter(boolean shouldRunInterpreter) {
        this.shouldRunInterpreter = shouldRunInterpreter;
    }

    public boolean getShouldRunInterpreter() {
        return shouldRunInterpreter && (hasScriptArgv || !showVersion);
    }

    /**
     * @see Options#CLI_CHECK_SYNTAX
     */
    public void setShouldCheckSyntax(boolean shouldSetSyntax) {
        this.shouldCheckSyntax = shouldSetSyntax;
    }

    /**
     * @see Options#CLI_CHECK_SYNTAX
     */
    public boolean getShouldCheckSyntax() {
        return shouldCheckSyntax;
    }

    /**
     * @see Options#CLI_AUTOSPLIT_SEPARATOR
     */
    public void setInputFieldSeparator(String inputFieldSeparator) {
        this.inputFieldSeparator = inputFieldSeparator;
    }

    /**
     * @see Options#CLI_AUTOSPLIT_SEPARATOR
     */
    public String getInputFieldSeparator() {
        return inputFieldSeparator;
    }

    /**
     * @see Options#CLI_KCODE
     */
    public KCode getKCode() {
        return kcode;
    }

    /**
     * @see Options#CLI_KCODE
     */
    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    /**
     * @see Options#CLI_ENCODING_INTERNAL
     */
    public void setInternalEncoding(String internalEncoding) {
        this.internalEncoding = internalEncoding;
    }

    /**
     * @see Options#CLI_ENCODING_INTERNAL
     */
    public String getInternalEncoding() {
        return internalEncoding;
    }

    /**
     * @see Options#CLI_ENCODING_EXTERNAL
     */
    public void setExternalEncoding(String externalEncoding) {
        this.externalEncoding = externalEncoding;
    }

    /**
     * @see Options#CLI_ENCODING_EXTERNAL
     */
    public String getExternalEncoding() {
        return externalEncoding;
    }

    /**
     * @see Options#CLI_ENCODING_SOURCE
     */
    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * @see Options#CLI_ENCODING_SOURCE
     */
    public String getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * @see Options#CLI_RECORD_SEPARATOR
     */
    public void setRecordSeparator(String recordSeparator) {
        this.recordSeparator = recordSeparator;
    }

    /**
     * @see Options#CLI_RECORD_SEPARATOR
     */
    public String getRecordSeparator() {
        return recordSeparator;
    }

    public int getSafeLevel() {
        return 0;
    }

    /**
     * @see Options#CLI_BACKUP_EXTENSION
     */
    public void setInPlaceBackupExtension(String inPlaceBackupExtension) {
        this.inPlaceBackupExtension = inPlaceBackupExtension;
    }

    /**
     * @see Options#CLI_BACKUP_EXTENSION
     */
    public String getInPlaceBackupExtension() {
        return inPlaceBackupExtension;
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

    /**
     * @see Options#CLI_PROFILING_MODE
     */
    public boolean isProfiling() {
        return profilingMode != ProfilingMode.OFF;
    }

    /**
     * @see Options#CLI_PROFILING_MODE
     */
    public boolean isProfilingEntireRun() {
        return profilingMode != ProfilingMode.OFF && profilingMode != ProfilingMode.API;
    }

    /**
     * @see Options#CLI_PROFILING_MODE
     */
    public void setProfilingMode(ProfilingMode profilingMode) {
        this.profilingMode = profilingMode;
    }

    /**
     * @see Options#CLI_PROFILING_MODE
     */
    public ProfilingMode getProfilingMode() {
        return profilingMode;
    }

    public void setProfileOutput(ProfileOutput output) {
        this.profileOutput = output;
    }

    public ProfileOutput getProfileOutput() {
        return profileOutput;
    }

    public boolean hasShebangLine() {
        return hasShebangLine;
    }

    public void setHasShebangLine(boolean hasShebangLine) {
        this.hasShebangLine = hasShebangLine;
    }

    /**
     * @see Options#CLI_RUBYGEMS_ENABLE
     */
    public boolean isDisableGems() {
        return disableGems;
    }

    /**
     * @see Options#CLI_RUBYOPT_ENABLE
     */
    public void setDisableRUBYOPT(boolean dr) {
        this.disableRUBYOPT = dr;
    }

    /**
     * @see Options#CLI_RUBYGEMS_ENABLE
     */
    public void setDisableGems(boolean dg) {
        this.disableGems = dg;
    }

    /**
     * @see Options#BACKTRACE_STYLE
     */
    public TraceType getTraceType() {
        return traceType;
    }

    /**
     * @see Options#BACKTRACE_STYLE
     */
    public void setTraceType(TraceType traceType) {
        this.traceType = traceType;
    }

    public void setHasScriptArgv(boolean argvRemains) {
        hasScriptArgv = argvRemains;
    }

    public boolean getHasScriptArgv() {
        return hasScriptArgv;
    }

    /**
     * Whether to mask .java lines in the Ruby backtrace, as MRI does for C calls.
     *
     * @see Options#BACKTRACE_MASK
     *
     * @return true if masking; false otherwise
     */
    public boolean getBacktraceMask() {
        return backtraceMask;
    }

    /**
     * Set whether to mask .java lines in the Ruby backtrace.
     *
     * @see Options#BACKTRACE_MASK
     *
     * @param backtraceMask true to mask; false otherwise
     */
    public void setBacktraceMask(boolean backtraceMask) {
        this.backtraceMask = backtraceMask;
    }

    /**
     * Set whether native code is enabled for this config.
     *
     * @see Options#NATIVE_ENABLED
     *
     * @param b new value indicating whether native code is enabled
     */
    public void setNativeEnabled(boolean b) {
        _nativeEnabled = b;
    }

    /**
     * Get whether native code is enabled for this config.
     *
     * @see Options#NATIVE_ENABLED
     *
     * @return true if native code is enabled; false otherwise.
     */
    public boolean isNativeEnabled() {
        return _nativeEnabled;
    }

    /**
     * Set whether to use the self-first jruby classloader.
     *
     * @see Options#CLASSLOADER_DELEGATE
     *
     * @param b new value indicating whether self-first classloader is used
     */
    public void setClassloaderDelegate(boolean b) {
        _classloaderDelegate = b;
    }

    /**
     * Get whether to use the self-first jruby classloader.
     *
     * @see Options#CLASSLOADER_DELEGATE
     *
     * @return true if self-first classloader is used; false otherwise.
     */
    public boolean isClassloaderDelegate() {
        return _classloaderDelegate;
    }

    /**
     * @see Options#CLI_STRIP_HEADER
     */
    public void setXFlag(boolean xFlag) {
        this.xFlag = xFlag;
    }

    /**
     * @see Options#CLI_STRIP_HEADER
     */
    public boolean isXFlag() {
        return xFlag;
    }

    /**
     * True if colorized backtraces are enabled. False otherwise.
     *
     * @see Options#BACKTRACE_COLOR
     */
    public boolean getBacktraceColor() {
        return backtraceColor;
    }

    /**
     * Set to true to enable colorized backtraces.
     *
     * @see Options#BACKTRACE_COLOR
     */
    public void setBacktraceColor(boolean backtraceColor) {
        this.backtraceColor = backtraceColor;
    }

    /**
     * Whether to use a single global lock for requires.
     *
     * @see Options#GLOBAL_REQUIRE_LOCK
     */
    public boolean isGlobalRequireLock() {
        return globalRequireLock;
    }

    /**
     * Set whether to use a single global lock for requires.
     *
     * @see Options#GLOBAL_REQUIRE_LOCK
     */
    public void setGlobalRequireLock(boolean globalRequireLock) {
        this.globalRequireLock = globalRequireLock;
    }

    /**
     * Set whether the JIT compiler should run in a background thread (Executor-based).
     *
     * @see Options#JIT_BACKGROUND
     *
     * @param jitBackground whether to run the JIT compiler in a background thread
     */
    public void setJitBackground(boolean jitBackground) {
        this.jitBackground = jitBackground;
    }

    /**
     * Get whether the JIT compiler will run in a background thread.
     *
     * @see Options#JIT_BACKGROUND
     *
     * @return whether the JIT compiler will run in a background thread
     */
    public boolean getJitBackground() {
        return jitBackground;
    }

    /**
     * Set whether to load and setup bundler on startup.
     *
     * @see Options#CLI_LOAD_GEMFILE
     */
    public void setLoadGemfile(boolean loadGemfile) {
        this.loadGemfile = loadGemfile;
    }

    /**
     * Whether to load and setup bundler on startup.
     *
     * @see Options#CLI_LOAD_GEMFILE
     */
    public boolean getLoadGemfile() {
        return loadGemfile;
    }

    /**
     * Set the maximum number of methods to consider when profiling.
     *
     * @see Options#PROFILE_MAX_METHODS
     */
    public void setProfileMaxMethods(int profileMaxMethods) {
        this.profileMaxMethods = profileMaxMethods;
    }

    /**
     * Get the maximum number of methods to consider when profiling.
     *
     * @see Options#PROFILE_MAX_METHODS
     */
    public int getProfileMaxMethods() {
        return profileMaxMethods;
    }

    /**
     * Set whether Kernel#gsub should be defined
     */
    public void setKernelGsubDefined(boolean setDefineKernelGsub) {
        this.kernelGsubDefined = setDefineKernelGsub;
    }

    /**
     * Get Kernel#gsub is defined or not
     */
    public boolean getKernelGsubDefined() {
        return kernelGsubDefined;
    }

    /**
     * get whether IPv4 is preferred
     *
     * @see Options#PREFER_IPV4
     */
    public boolean getIPv4Preferred() {
        return preferIPv4;
    }

    /**
     * get whether uppercase package names will be honored
     */
    public boolean getAllowUppercasePackageNames() {
        return allowUppercasePackageNames;
    }

    /**
     * set whether uppercase package names will be honored
     */
    public void setAllowUppercasePackageNames(boolean allow) {
        allowUppercasePackageNames = allow;
    }

    public String getProfilingService() {
        return profilingService;
    }

    public void setProfilingService( String service )  {
        this.profilingService = service;
    }
    
    private static ClassLoader setupLoader() {
        ClassLoader loader = RubyInstanceConfig.class.getClassLoader();

        // loader can be null for example when jruby comes from the boot-classLoader

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        return loader;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Configuration fields.
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Indicates whether the script must be extracted from script source
     */
    private boolean xFlag = Options.CLI_STRIP_HEADER.load();

    /**
     * Indicates whether the script has a shebang line or not
     */
    private boolean hasShebangLine;
    private InputStream input          = System.in;
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;
    private Profile profile            = Profile.DEFAULT;
    private boolean objectSpaceEnabled = Options.OBJECTSPACE_ENABLED.load();
    private boolean siphashEnabled     = Options.SIPHASH_ENABLED.load();

    private CompileMode compileMode = CompileMode.OFF;
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

    private String internalEncoding = Options.CLI_ENCODING_INTERNAL.load();
    private String externalEncoding = Options.CLI_ENCODING_EXTERNAL.load();
    private String sourceEncoding = Options.CLI_ENCODING_SOURCE.load();

    private ProfilingMode profilingMode = Options.CLI_PROFILING_MODE.load();
    private ProfileOutput profileOutput = new ProfileOutput(System.err);
    private String profilingService;

    private ClassLoader loader = setupLoader();

    public ClassLoader getCurrentThreadClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<String>();
    private Set<String> excludedMethods = new HashSet<String>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private Collection<String> requiredLibraries = new LinkedHashSet<String>();
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = Options.CLI_ASSUME_LOOP.load();
    private boolean assumePrinting = Options.CLI_ASSUME_PRINT.load();
    private Map optionGlobals = new HashMap();
    private boolean processLineEnds = Options.CLI_PROCESS_LINE_ENDS.load();
    private boolean split = Options.CLI_AUTOSPLIT.load();
    private Verbosity verbosity = Options.CLI_WARNING_LEVEL.load();
    private boolean debug = Options.CLI_DEBUG.load();
    private boolean showVersion = Options.CLI_VERSION.load();
    private boolean showBytecode = Options.CLI_BYTECODE.load();
    private boolean showCopyright = Options.CLI_COPYRIGHT.load();
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = Options.CLI_HELP.load();
    private boolean shouldPrintProperties=Options.CLI_PROPERTIES.load();
    private boolean dumpConfig=false;
    private KCode kcode = Options.CLI_KCODE.load();
    private String recordSeparator = Options.CLI_RECORD_SEPARATOR.load();
    private boolean shouldCheckSyntax = Options.CLI_CHECK_SYNTAX.load();
    private String inputFieldSeparator = Options.CLI_AUTOSPLIT_SEPARATOR.load();
    private boolean managementEnabled = false;
    private String inPlaceBackupExtension = Options.CLI_BACKUP_EXTENSION.load();
    private boolean parserDebug = false;
    private String threadDumpSignal = null;
    private boolean hardExit = false;
    private boolean disableGems = !Options.CLI_RUBYGEMS_ENABLE.load();
    private boolean disableRUBYOPT = !Options.CLI_RUBYOPT_ENABLE.load();
    private boolean updateNativeENVEnabled = true;
    private boolean kernelGsubDefined;
    private boolean hasScriptArgv = false;
    private boolean preferIPv4 = Options.PREFER_IPV4.load();

    private String jrubyHome;

    /**
     * Whether native code is enabled for this configuration.
     */
    private boolean _nativeEnabled = NATIVE_ENABLED;
    private boolean _classloaderDelegate = Options.CLASSLOADER_DELEGATE.load();

    private TraceType traceType =
            TraceType.traceTypeFor(Options.BACKTRACE_STYLE.load());

    private boolean backtraceMask = Options.BACKTRACE_MASK.load();

    private boolean backtraceColor = Options.BACKTRACE_COLOR.load();

    private LoadServiceCreator creator = LoadServiceCreator.DEFAULT;

    private boolean globalRequireLock = Options.GLOBAL_REQUIRE_LOCK.load();

    private boolean jitBackground = Options.JIT_BACKGROUND.load();

    private boolean loadGemfile = Options.CLI_LOAD_GEMFILE.load();

    private int profileMaxMethods = Options.PROFILE_MAX_METHODS.load();

    private boolean allowUppercasePackageNames = Options.JI_UPPER_CASE_PACKAGE_NAME_ALLOWED.load();

    private boolean forceStdin = false;

    ////////////////////////////////////////////////////////////////////////////
    // Support classes, etc.
    ////////////////////////////////////////////////////////////////////////////

    public enum Verbosity { NIL, FALSE, TRUE }

    public static interface LoadServiceCreator {
        LoadService create(Ruby runtime);

        LoadServiceCreator DEFAULT = new LoadServiceCreator() {
                public LoadService create(Ruby runtime) {
                    return new LoadService(runtime);
                }
            };
    }

    public enum ProfilingMode {
		OFF, API, FLAT, GRAPH, HTML, JSON, SERVICE
	}

    public enum CompileMode {
        JIT, FORCE, OFF, TRUFFLE;

        public boolean shouldPrecompileCLI() {
            return this == JIT || this == FORCE;
        }

        public boolean shouldJIT() {
            return this == JIT || this == FORCE;
        }

        public boolean shouldPrecompileAll() {
            return this == FORCE;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Static configuration fields, used as defaults for new JRuby instances.
    ////////////////////////////////////////////////////////////////////////////

    // NOTE: These BigDecimal fields must be initialized before calls to initGlobalJavaVersion

    /** A BigDecimal representing 1.5, for Java spec version matching */
    private static final BigDecimal BIGDECIMAL_1_5 = new BigDecimal("1.5");
    /** A BigDecimal representing 1.6, for Java spec version matching */
    private static final BigDecimal BIGDECIMAL_1_6 = new BigDecimal("1.6");
    /** A BigDecimal representing 1.7, for Java spec version matching */
    private static final BigDecimal BIGDECIMAL_1_7 = new BigDecimal("1.7");

    /**
     * The version to use for generated classes. Set to current JVM version by default
     */
    public static final int JAVA_VERSION = initGlobalJavaVersion();

    /**
     * The number of lines at which a method, class, or block body is split into
     * chained methods (to dodge 64k method-size limit in JVM).
     */
    public static final int CHAINED_COMPILE_LINE_COUNT = Options.COMPILE_CHAINSIZE.load();

    /**
     * Enable compiler peephole optimizations.
     *
     * Set with the <tt>jruby.compile.peephole</tt> system property.
     */
    public static final boolean PEEPHOLE_OPTZ = Options.COMPILE_PEEPHOLE.load();

    /**
     * Enable compiler "noguards" optimizations.
     *
     * Set with the <tt>jruby.compile.noguards</tt> system property.
     */
    public static boolean NOGUARDS_COMPILE_ENABLED = Options.COMPILE_NOGUARDS.load();

    /**
     * Enable compiler "fastest" set of optimizations.
     *
     * Set with the <tt>jruby.compile.fastest</tt> system property.
     */
    public static boolean FASTEST_COMPILE_ENABLED = Options.COMPILE_FASTEST.load();

    /**
     * Enable fast operator compiler optimizations.
     *
     * Set with the <tt>jruby.compile.fastops</tt> system property.
     */
    public static boolean FASTOPS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED || Options.COMPILE_FASTOPS.load();

    /**
     * Enable "threadless" compile.
     *
     * Set with the <tt>jruby.compile.threadless</tt> system property.
     */
    public static boolean THREADLESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED || Options.COMPILE_THREADLESS.load();

    /**
     * Enable "fast send" compiler optimizations.
     *
     * Set with the <tt>jruby.compile.fastsend</tt> system property.
     */
    public static boolean FASTSEND_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED || Options.COMPILE_FASTSEND.load();

    /**
     * Enable lazy handles optimizations.
     *
     * Set with the <tt>jruby.compile.lazyHandles</tt> system property.
     */
    public static boolean LAZYHANDLES_COMPILE = Options.COMPILE_LAZYHANDLES.load();

    /**
     * Enable fast multiple assignment optimization.
     *
     * Set with the <tt>jruby.compile.fastMasgn</tt> system property.
     */
    public static boolean FAST_MULTIPLE_ASSIGNMENT = Options.COMPILE_FASTMASGN.load();

    /**
     * Enable a thread pool. Each Ruby thread will be mapped onto a thread from this pool.
     *
     * Set with the <tt>jruby.thread.pool.enabled</tt> system property.
     */
    public static final boolean POOLING_ENABLED = false;

    /**
     * Maximum thread pool size (integer, default Integer.MAX_VALUE).
     *
     * Set with the <tt>jruby.thread.pool.max</tt> system property.
     */
    public static final int POOL_MAX = Options.THREADPOOL_MAX.load();
    /**
     * Minimum thread pool size (integer, default 0).
     *
     * Set with the <tt>jruby.thread.pool.min</tt> system property.
     */
    public static final int POOL_MIN = Options.THREADPOOL_MIN.load();
    /**
     * Thread pool time-to-live in seconds.
     *
     * Set with the <tt>jruby.thread.pool.max</tt> system property.
     */
    public static final int POOL_TTL = Options.THREADPOOL_TTL.load();
    /**
     * Fiber thread pool time-to-live in seconds.
     *
     * Set with the <tt>jruby.fiber.thread.pool.max</tt> system property.
     */
    public static final int FIBER_POOL_TTL = Options.FIBER_THREADPOOL_TTL.load();

    /**
     * Enable use of the native Java version of the 'net/protocol' library.
     *
     * Set with the <tt>jruby.thread.pool.max</tt> system property.
     */
    public static final boolean NATIVE_NET_PROTOCOL = Options.NATIVE_NET_PROTOCOL.load();

    /**
     * Enable tracing of method calls.
     *
     * Set with the <tt>jruby.debug.fullTrace</tt> system property.
     */
    public static boolean FULL_TRACE_ENABLED = Options.DEBUG_FULLTRACE.load();

    /**
     * Comma-separated list of methods to exclude from JIT compilation.
     * Specify as "Module", "Module#method" or "method".
     *
     * Set with the <tt>jruby.jit.exclude</tt> system property.
     */
    public static final String COMPILE_EXCLUDE = Options.JIT_EXCLUDE.load();

    /**
     * Indicates the global default for whether native code is enabled. Default
     * is true. This value is used to default new runtime configurations.
     *
     * Set with the <tt>jruby.native.enabled</tt> system property.
     */
    public static final boolean NATIVE_ENABLED = Options.NATIVE_ENABLED.load();

    @Deprecated
    public final static boolean CEXT_ENABLED = false;

    /**
     * Whether to reify (pre-compile and generate) a Java class per Ruby class.
     *
     * Set with the <tt>jruby.reify.classes</tt> system property.
     */
    public static final boolean REIFY_RUBY_CLASSES = Options.REIFY_CLASSES.load();

    /**
     * Log errors that occur during reification.
     *
     * Set with the <tt>jruby.reify.logErrors</tt> system property.
     */
    public static final boolean REIFY_LOG_ERRORS = Options.REIFY_LOGERRORS.load();

    /**
     * Whether to use a custom-generated handle for Java methods instead of
     * reflection.
     *
     * Set with the <tt>jruby.java.handles</tt> system property.
     */
    public static final boolean USE_GENERATED_HANDLES = Options.JAVA_HANDLES.load();

    /**
     * Turn on debugging of the load service (requires and loads).
     *
     * Set with the <tt>jruby.debug.loadService</tt> system property.
     */
    public static final boolean DEBUG_LOAD_SERVICE = Options.DEBUG_LOADSERVICE.load();

    /**
     * Turn on timings of the load service (requires and loads).
     *
     * Set with the <tt>jruby.debug.loadService.timing</tt> system property.
     */
    public static final boolean DEBUG_LOAD_TIMINGS = Options.DEBUG_LOADSERVICE_TIMING.load();

    /**
     * Turn on debugging of subprocess launching.
     *
     * Set with the <tt>jruby.debug.launch</tt> system property.
     */
    public static final boolean DEBUG_LAUNCHING = Options.DEBUG_LAUNCH.load();

    /**
     * Turn on debugging of script resolution with "-S".
     *
     * Set with the <tt>jruby.debug.scriptResolution</tt> system property.
     */
    public static final boolean DEBUG_SCRIPT_RESOLUTION = Options.DEBUG_SCRIPTRESOLUTION.load();

    public static final boolean DEBUG_PARSER = Options.DEBUG_PARSER.load();

    public static final boolean JUMPS_HAVE_BACKTRACE = Options.JUMP_BACKTRACE.load();

    public static final boolean JIT_CACHE_ENABLED = Options.JIT_CACHE.load();

    public static final String JIT_CODE_CACHE = Options.JIT_CODECACHE.load();

    public static final boolean REFLECTED_HANDLES = Options.REFLECTED_HANDLES.load();

    public static final boolean NO_UNWRAP_PROCESS_STREAMS = Options.PROCESS_NOUNWRAP.load();

    public static final boolean INTERFACES_USE_PROXY = Options.INTERFACES_USEPROXY.load();

    public static final boolean JIT_LOADING_DEBUG = Options.JIT_DEBUG.load();

    public static final boolean CAN_SET_ACCESSIBLE = Options.JI_SETACCESSIBLE.load();

    // properties for logging exceptions, backtraces, and caller invocations
    public static final boolean LOG_EXCEPTIONS = Options.LOG_EXCEPTIONS.load();
    public static final boolean LOG_BACKTRACES = Options.LOG_BACKTRACES.load();
    public static final boolean LOG_CALLERS = Options.LOG_CALLERS.load();
    public static final boolean LOG_WARNINGS = Options.LOG_WARNINGS.load();

    public static final boolean ERRNO_BACKTRACE = Options.ERRNO_BACKTRACE.load();
    public static final boolean STOPITERATION_BACKTRACE = Options.STOPITERATION_BACKTRACE.load();

    public static boolean IR_DEBUG = Options.IR_DEBUG.load();
    public static boolean IR_PROFILE = Options.IR_PROFILE.load();
    public static boolean IR_COMPILER_DEBUG = Options.IR_COMPILER_DEBUG.load();
    public static boolean IR_WRITING = Options.IR_WRITING.load();
    public static boolean IR_READING = Options.IR_READING.load();
    public static boolean IR_READING_DEBUG = Options.IR_READING_DEBUG.load();
    public static boolean IR_WRITING_DEBUG = Options.IR_WRITING_DEBUG.load();
    public static boolean IR_VISUALIZER = Options.IR_VISUALIZER.load();
    public static boolean IR_UNBOXING = Options.IR_UNBOXING.load();
    public static String IR_COMPILER_PASSES = Options.IR_COMPILER_PASSES.load();
    public static String IR_JIT_PASSES = Options.IR_JIT_PASSES.load();
    public static String IR_INLINE_COMPILER_PASSES = Options.IR_INLINE_COMPILER_PASSES.load();

    private TruffleHooksStub truffleHooks = null;

    public static final boolean COROUTINE_FIBERS = Options.FIBER_COROUTINES.load();

    /**
     * Whether to calculate consistent hashes across JVM instances, or to ensure
     * un-predicatable hash values using SecureRandom.
     *
     * Set with the <tt>jruby.consistent.hashing.enabled</tt> system property.
     */
    public static final boolean CONSISTENT_HASHING_ENABLED = Options.CONSISTENT_HASHING.load();

    private static volatile boolean loadedNativeExtensions = false;

    ////////////////////////////////////////////////////////////////////////////
    // Static initializers
    ////////////////////////////////////////////////////////////////////////////

    private static int initGlobalJavaVersion() {
        String specVersion = Options.BYTECODE_VERSION.load();

        // stack map calculation is failing for some compilation scenarios, so
        // forcing both 1.5 and 1.6 to use 1.5 bytecode for the moment.
        if (specVersion.equals("1.5")) {// || specVersion.equals("1.6")) {
           return Opcodes.V1_5;
        } else if (specVersion.equals("1.6")) {
            return Opcodes.V1_6;
        } else if (specVersion.equals("1.7") || specVersion.equals("1.8") || specVersion.equals("1.9")) {
            return Opcodes.V1_7;
        } else {
            System.err.println("unsupported Java version \"" + specVersion + "\", defaulting to 1.5");
            return Opcodes.V1_5;
        }
    }
    public void setTruffleHooks(TruffleHooksStub truffleHooks) {
        this.truffleHooks = truffleHooks;
    }

    public TruffleHooksStub getTruffleHooks() {
        return truffleHooks;
    }

    @Deprecated
    public void setSafeLevel(int safeLevel) {
    }

    @Deprecated
    public String getInPlaceBackupExtention() {
        return inPlaceBackupExtension;
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
        return OutputStrings.getVersionString();
    }

    @Deprecated
    public String getCopyrightString() {
        return OutputStrings.getCopyrightString();
    }

    @Deprecated
    public Collection<String> requiredLibraries() {
        return requiredLibraries;
    }

    @Deprecated
    public List<String> loadPaths() {
        return loadPaths;
    }

    @Deprecated
    public boolean shouldPrintUsage() {
        return shouldPrintUsage;
    }

    @Deprecated
    public boolean shouldPrintProperties() {
        return shouldPrintProperties;
    }

    @Deprecated
    public Boolean getVerbose() {
        return isVerbose();
    }

    @Deprecated
    public boolean shouldRunInterpreter() {
        return isShouldRunInterpreter();
    }

    @Deprecated
    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    @Deprecated
    public boolean isxFlag() {
        return xFlag;
    }

    /**
     * The max count of active methods eligible for JIT-compilation.
     */
    @Deprecated
    public static final int JIT_MAX_METHODS_LIMIT = Constants.JIT_MAX_METHODS_LIMIT;

    /**
     * The max size of JIT-compiled methods (full class size) allowed.
     */
    @Deprecated
    public static final int JIT_MAX_SIZE_LIMIT = Constants.JIT_MAX_SIZE_LIMIT;

    /**
     * The JIT threshold to the specified method invocation count.
     */
    @Deprecated
    public static final int JIT_THRESHOLD = Constants.JIT_THRESHOLD;

    /**
     * Default size for chained compilation.
     */
    @Deprecated
    public static final int CHAINED_COMPILE_LINE_COUNT_DEFAULT = Constants.CHAINED_COMPILE_LINE_COUNT_DEFAULT;

    @Deprecated
    public static final boolean nativeEnabled = NATIVE_ENABLED;

    @Deprecated
    public boolean isSamplingEnabled() {
        return false;
    }

    @Deprecated
    public void setBenchmarking(boolean benchmarking) {
    }

    @Deprecated
    public boolean isBenchmarking() {
        return false;
    }

    @Deprecated
    public void setCextEnabled(boolean b) {
    }

    @Deprecated
    public boolean isCextEnabled() {
        return false;
    }

}
