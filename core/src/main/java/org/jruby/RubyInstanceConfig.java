/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.Constants;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.profile.builtin.ProfileOutput;
import org.jruby.util.ClassesLoader;
import org.jruby.util.ClasspathLauncher;
import org.jruby.util.FileResource;
import org.jruby.util.Loader;
import org.jruby.util.InputStreamMarkCursor;
import org.jruby.util.JRubyFile;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.StringSupport;
import org.jruby.util.UriLikePathHelper;
import org.jruby.util.cli.ArgumentProcessor;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;

import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * A structure used to configure new JRuby instances. All publicly-tweakable
 * aspects of Ruby can be modified here, including those settable by command-
 * line options, those available through JVM properties, and those suitable for
 * embedding.
 */
public class RubyInstanceConfig {

    public RubyInstanceConfig() {
        this(Ruby.isSecurityRestricted());
    }

    public RubyInstanceConfig(boolean isSecurityRestricted) {
        this.isSecurityRestricted = isSecurityRestricted;
        currentDirectory = isSecurityRestricted ? "/" : JRubyFile.getFileProperty("user.dir");

        if (isSecurityRestricted) {
            compileMode = CompileMode.OFF;
            jitLogging = false;
            jitLoggingVerbose = false;
            jitLogEvery = 0;
            jitThreshold = -1;
            jitMax = 0;
            jitMaxSize = -1;
            managementEnabled = false;
        } else {
            if (COMPILE_EXCLUDE != null) {
                excludedMethods.addAll(StringSupport.split(COMPILE_EXCLUDE, ','));
            }

            managementEnabled = Options.MANAGEMENT_ENABLED.load();
            runRubyInProcess = Options.LAUNCH_INPROC.load();
            compileMode = Options.COMPILE_MODE.load();

            jitLogging = Options.JIT_LOGGING.load();
            jitLoggingVerbose = Options.JIT_LOGGING_VERBOSE.load();
            jitLogEvery = Options.JIT_LOGEVERY.load();
            jitThreshold = Options.JIT_THRESHOLD.load();
            jitMax = Options.JIT_MAX.load();
            jitMaxSize = Options.JIT_MAXSIZE.load();
        }

        initEnvironment();
    }

    public RubyInstanceConfig(RubyInstanceConfig parentConfig) {
        isSecurityRestricted = parentConfig.isSecurityRestricted;
        currentDirectory = parentConfig.getCurrentDirectory();
        compileMode = parentConfig.getCompileMode();
        jitLogging = parentConfig.jitLogging;
        jitLoggingVerbose = parentConfig.jitLoggingVerbose;
        jitLogEvery = parentConfig.jitLogEvery;
        jitThreshold = parentConfig.jitThreshold;
        jitMax = parentConfig.jitMax;
        jitMaxSize = parentConfig.jitMaxSize;
        managementEnabled = parentConfig.managementEnabled;
        runRubyInProcess = parentConfig.runRubyInProcess;
        excludedMethods = parentConfig.excludedMethods;
        updateNativeENVEnabled = parentConfig.updateNativeENVEnabled;

        profilingService = parentConfig.profilingService;
        profilingMode = parentConfig.profilingMode;

        initEnvironment();
    }

    private void initEnvironment() {
        try {
            setEnvironment(System.getenv());
        }
        catch (SecurityException se) { /* ignore missing getenv permission */ }
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
            processArgumentsWithRubyopts();
        } catch (SecurityException se) {
            // ignore and do nothing
        }
    }

    public void processArgumentsWithRubyopts() {
        if (!enableRUBYOPT) return;
        
        // environment defaults to System.getenv normally
        Object rubyoptObj = environment.get("RUBYOPT");

        if (rubyoptObj == null) return;

        // Our argument processor bails if an arg starts with space, so we trim the RUBYOPT line
        // See #4849
        String rubyopt = rubyoptObj.toString().trim();

        if (rubyopt.length() == 0) return;

        String[] rubyoptArgs = rubyopt.split("\\s+");
        if (rubyoptArgs.length != 0) {
            new ArgumentProcessor(rubyoptArgs, false, true, true, this).processArguments();
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
        String[] result = EMPTY_STRING_ARRAY;
        if (in == null) return result;

        if (isXFlag()) eatToShebang(in);

        BufferedReader reader;
        try {
            InputStreamMarkCursor cursor = new InputStreamMarkCursor(in, 8192);
            try {
                if (!isShebang(cursor)) return result;
            } finally {
                cursor.finish();
            }

            in.mark(8192);
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1), 8192);
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
        if (!isSecurityRestricted) {
            newJRubyHome = SafePropertyAccessor.getProperty("jruby.home");
        }

        if (newJRubyHome == null && getLoader().getResource("META-INF/jruby.home/.jrubydir") != null) {
            newJRubyHome = "uri:classloader://META-INF/jruby.home";
        }
        if (newJRubyHome != null) {
            // verify it if it's there
            newJRubyHome = verifyHome(newJRubyHome, error);
        } else {
            try {
                newJRubyHome = SafePropertyAccessor.getenv("JRUBY_HOME");
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
        if (Platform.IS_WINDOWS && !RubyFile.PROTOCOL_PATTERN.matcher(newJRubyHome).matches()) {
            try {
                newJRubyHome = new File(newJRubyHome).getCanonicalPath();
            }
            catch (IOException e) {} // just let newJRubyHome stay the way it is if this fails
        }

        return newJRubyHome == null ? null : JRubyFile.normalizeSeps(newJRubyHome);
    }

    // We require the home directory to be absolute
    private static String verifyHome(String home, PrintStream error) {
        if ("uri:classloader://META-INF/jruby.home".equals(home) || "uri:classloader:/META-INF/jruby.home".equals(home)) {
            return home;
        }
        if (".".equals(home)) {
            home = SafePropertyAccessor.getProperty("user.dir");
        }
        else if (home.startsWith("cp:")) {
            home = home.substring(3);
        }
        if (home.startsWith("jar:") || ( home.startsWith("file:") && home.contains(".jar!/") ) ||
                home.startsWith("classpath:") || home.startsWith("uri:")) {
            error.println("Warning: JRuby home with uri like paths may not have full functionality - use at your own risk");
        }
        // do not normalize on plain jar like paths coming from jruby-rack
        else if (!home.contains(".jar!/") && !home.startsWith("uri:")) {
            File file = new File(home);
            if (!file.exists()) {
                final String tmpdir = SafePropertyAccessor.getProperty("java.io.tmpdir");
                error.println("Warning: JRuby home \"" + file + "\" does not exist, using " + tmpdir);
                return tmpdir;
            }
            if (!file.isAbsolute()) {
                home = file.getAbsolutePath();
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
                final String script = getScriptFileName();
                FileResource resource = JRubyFile.createRestrictedResource(getCurrentDirectory(), getScriptFileName());
                if (resource != null && resource.exists()) {
                    if (resource.canRead() && !resource.isDirectory()) {
                        if (isXFlag()) {
                            // search for a shebang line and
                            // return the script between shebang and __END__ or CTRL-Z (0x1A)
                            return findScript(resource.openInputStream());
                        }
                        return resource.openInputStream();
                    }
                    else {
                        throw new FileNotFoundException(script + " (Not a file)");
                    }
                }
                else {
                    throw new FileNotFoundException(script + " (No such file or directory)");
                }
            }
        } catch (IOException e) {
            throw new MainExitException(1, "Error opening script file: " + e.getMessage());
        }
    }

    private static InputStream findScript(InputStream is) throws IOException {
        StringBuilder buf = new StringBuilder(64);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        boolean foundRubyShebang = false;
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            if (isRubyShebangLine(currentLine)) {
                foundRubyShebang = true;
                break;
            }
        }

        if (!foundRubyShebang) {
            throw new MainExitException(1, "jruby: no Ruby script found in input (LoadError)");
        }

        buf.append(currentLine).append('\n');

        do {
            currentLine = br.readLine();
            if (currentLine != null) {
                buf.append(currentLine).append('\n');
            }
        } while (!(currentLine == null || currentLine.contains("__END__") || currentLine.contains("\026")));
        return new BufferedInputStream(new ByteArrayInputStream(buf.toString().getBytes()), 8192);
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
        jrubyHome = home != null ? verifyHome(home, error) : null;
        resetEnvRuby();
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
     * @return true if JIT compilation is enabled
     */
    public boolean isJitEnabled() {
        return getJitThreshold() >= 0 && getCompileMode().shouldJIT();
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

    public void setEnvironment(Map<String, String> newEnvironment) {
        environment = new HashMap<>();
        if (newEnvironment != null) {
            environment.putAll(newEnvironment);
        }
    }

    public Map<String, String> getEnvironment() {
        if (!environment.containsKey("RUBY") && RubyFile.PROTOCOL_PATTERN.matcher(getJRubyHome()).matches()) {
            // assumption: if JRubyHome is not a regular file than jruby got launched in an embedded fashion
            environment.put("RUBY", ClasspathLauncher.jrubyCommand(defaultClassLoader()));
            setEnvRuby = true;
        }
        return environment;
    }

    private transient boolean setEnvRuby;

    private void resetEnvRuby() { // when jruby-home changes, we might need to recompute
        if (setEnvRuby) environment.remove("RUBY");
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    private final List<String> extraLoadPaths = new CopyOnWriteArrayList<>();
    public List<String> getExtraLoadPaths() {
        return extraLoadPaths;
    }

    private final List<String> extraGemPaths = new CopyOnWriteArrayList<>();
    public List<String> getExtraGemPaths() {
        return extraGemPaths;
    }

    private final List<Loader> extraLoaders = new CopyOnWriteArrayList<>();
    public List<Loader> getExtraLoaders() {
        return extraLoaders;
    }

    /**
     * adds a given ClassLoader to jruby. i.e. adds the root of
     * the classloader to the LOAD_PATH so embedded ruby scripts
     * can be found. dito for embedded gems.
     *
     * since classloaders do not provide directory information (some
     * do and some do not) the source of the classloader needs to have
     * a '.jrubydir' in each with the list of files and directories of the
     * same directory. (see jruby-stdlib.jar or jruby-complete.jar inside
     * META-INF/jruby.home for examples).
     *
     * these files can be generated by <code>jruby -S generate_dir_info {path/to/ruby/files}</code>
     *
     * @param loader
     */
    public void addLoader(ClassLoader loader) {
        addLoader(new ClassesLoader(loader));
    }

    /**
     * adds a given "bundle" to jruby. an OSGi bundle and a classloader
     * both have common set of method but do not share a common interface.
     * for adding a bundle or classloader to jruby is done via the base URL of
     * the classloader/bundle. all we need is the 'getResource'/'getResources'
     * method to do so.
     * @param bundle
     */
    public void addLoader(Loader bundle) {
        // loader can be a ClassLoader or an Bundle from OSGi
        UriLikePathHelper helper = new UriLikePathHelper(bundle);
        String uri = helper.getUriLikePath();
        if (uri != null) extraLoadPaths.add(uri);
        uri = helper.getUriLikeGemPath();
        if (uri != null) extraGemPaths.add(uri);
        extraLoaders.add(bundle);
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

    public void setBacktraceLimit(Integer limit) {
        this.backtraceLimit = limit;
    }

    public Integer getBacktraceLimit() {
        return this.backtraceLimit;
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
     * Get the set of enabled warning categories.
     *
     * @return the set of enabled warning categories
     */
    public Set<RubyWarnings.Category> getWarningCategories() {
        return warningCategories;
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

    public Map<String, String> getOptionGlobals() {
        return optionGlobals;
    }

    public boolean isManagementEnabled() {
        return managementEnabled;
    }

    public Set<String> getExcludedMethods() {
        return excludedMethods;
    }

    public boolean isArgvGlobalsOn() {
        return argvGlobalsOn;
    }

    public void setArgvGlobalsOn(boolean argvGlobalsOn) {
        this.argvGlobalsOn = argvGlobalsOn;
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

    public void setAllFeaturesEnabled(boolean ea) {
        // disable all features
        for (ArgumentProcessor.Feature feature: ArgumentProcessor.Feature.values()) {
            if (feature == ArgumentProcessor.Feature.ALL) continue; // skip self
            feature.enabler.accept(this, ea);
        }
    }

    /**
     * @see Options#CLI_RUBYGEMS_ENABLE
     */
    public boolean isGemsEnabled() {
        return enableGems;
    }

    /**
     * @see Options#CLI_RUBYGEMS_ENABLE
     */
    public void setGemsEnabled(boolean eg) {
        this.enableGems = eg;
    }

    /**
     * @see Options#CLI_DID_YOU_MEAN_ENABLE
     */
    public boolean isDidYouMeanEnabled() {
        return enableDidYouMean;
    }

    /**
     * @see Options#CLI_DID_YOU_MEAN_ENABLE
     */
    public void setDidYouMeanEnabled(boolean edym) {
        this.enableDidYouMean = edym;
    }

    /**
     * @see Options#CLI_ERROR_HIGHLIGHT_ENABLE
     */
    public boolean isErrorHighlightEnabled() {
        return enableErrorHighlight;
    }

    /**
     * @see Options#CLI_ERROR_HIGHLIGHT_ENABLE
     */
    public void setErrorHighlightEnabled(boolean eeh) {
        this.enableErrorHighlight = eeh;
    }

    /**
     * @see Options#CLI_SYNTAX_SUGGEST_ENABLE
     */
    public boolean isSyntaxSuggestEnabled() {
        return enableSyntaxSuggest;
    }

    /**
     * @see Options#CLI_SYNTAX_SUGGEST_ENABLE
     */
    public void setSyntaxSuggestEnabled(boolean ess) {
        this.enableSyntaxSuggest = ess;
    }

    /**
     * @see Options#CLI_RUBYOPT_ENABLE
     */
    public void setRUBYOPTEnabled(boolean er) {
        this.enableRUBYOPT = er;
    }

    /**
     * @see Options#CLI_RACTOR_ENABLE
     */
    public boolean isRactorEnabled() {
        return enableRactor;
    }

    /**
     * @see Options#CLI_RACTOR_ENABLE
     */
    public void setRactorEnabled(boolean er) {
        this.enableRactor = er;
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

    public Boolean isFrozenStringLiteral() {
        return frozenStringLiteral;
    }

    public void setFrozenStringLiteral(boolean frozenStringLiteral) {
        this.frozenStringLiteral = frozenStringLiteral;
    }

    public boolean isDebuggingFrozenStringLiteral() {
        return debuggingFrozenStringLiteral;
    }

    public void setDebuggingFrozenStringLiteral(boolean debuggingFrozenStringLiteral) {
        this.debuggingFrozenStringLiteral = debuggingFrozenStringLiteral;
    }

    public boolean isInterruptibleRegexps() {
        return interruptibleRegexps;
    }

    public static ClassLoader defaultClassLoader() {
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

    private final boolean isSecurityRestricted;

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

    private CompileMode compileMode;
    private String currentDirectory;

    /** Environment variables; defaults to System.getenv() in constructor */
    private Map<String, String> environment;
    private String[] argv = {};

    private final boolean jitLogging;
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
    private String profilingService = Options.CLI_PROFILING_SERVICE.load();

    private ClassLoader loader = defaultClassLoader();

    public ClassLoader getCurrentThreadClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<String>(0);
    private Set<String> excludedMethods = new HashSet<String>(0);
    private final StringBuffer inlineScript = new StringBuffer(0);
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private final Collection<String> requiredLibraries = new LinkedHashSet<String>(0);
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = Options.CLI_ASSUME_LOOP.load();
    private boolean assumePrinting = Options.CLI_ASSUME_PRINT.load();
    private final Map<String, String> optionGlobals = new HashMap<String, String>(0);
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
    private KCode kcode = Options.CLI_KCODE.load();
    private String recordSeparator = Options.CLI_RECORD_SEPARATOR.load();
    private boolean shouldCheckSyntax = Options.CLI_CHECK_SYNTAX.load();
    private Integer backtraceLimit = Options.CLI_BACKTRACE_LIMIT.load();
    private String inputFieldSeparator = Options.CLI_AUTOSPLIT_SEPARATOR.load();
    private boolean managementEnabled = false;
    private String inPlaceBackupExtension = Options.CLI_BACKUP_EXTENSION.load();
    private boolean parserDebug = false;
    private boolean hardExit = false;
    private boolean enableGems = Options.CLI_RUBYGEMS_ENABLE.load();
    private boolean enableDidYouMean = Options.CLI_DID_YOU_MEAN_ENABLE.load();
    private boolean enableErrorHighlight = Options.CLI_ERROR_HIGHLIGHT_ENABLE.load();
    private boolean enableSyntaxSuggest = Options.CLI_SYNTAX_SUGGEST_ENABLE.load();
    private boolean enableRUBYOPT = Options.CLI_RUBYOPT_ENABLE.load();
    private boolean enableRactor = Options.CLI_RACTOR_ENABLE.load();
    private boolean updateNativeENVEnabled = true;
    private boolean kernelGsubDefined;
    private boolean hasScriptArgv = false;
    private Boolean frozenStringLiteral = null;
    private boolean debuggingFrozenStringLiteral = false;
    private final boolean interruptibleRegexps = Options.REGEXP_INTERRUPTIBLE.load();
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

    private boolean jitBackground = Options.JIT_BACKGROUND.load();

    private boolean loadGemfile = Options.CLI_LOAD_GEMFILE.load();

    private int profileMaxMethods = Options.PROFILE_MAX_METHODS.load();

    private boolean allowUppercasePackageNames = Options.JI_UPPER_CASE_PACKAGE_NAME_ALLOWED.load();

    private boolean forceStdin = false;

    private final Set<RubyWarnings.Category> warningCategories = Collections.synchronizedSet(EnumSet.of(RubyWarnings.Category.EXPERIMENTAL));

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
        JIT, FORCE, OFF;

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

    /**
     * The version to use for generated classes. Set to current JVM version by default
     */
    public static final int JAVA_VERSION = initJavaBytecodeVersion();

    /**
     * Enable a thread pool. Each Ruby thread will be mapped onto a thread from this pool.
     *
     * Set with the <code>jruby.thread.pool.enabled</code> system property.
     */
    public static final boolean POOLING_ENABLED = false;

    /**
     * Maximum thread pool size (integer, default Integer.MAX_VALUE).
     *
     * Set with the <code>jruby.thread.pool.max</code> system property.
     */
    public static final int POOL_MAX = Options.THREADPOOL_MAX.load();
    /**
     * Minimum thread pool size (integer, default 0).
     *
     * Set with the <code>jruby.thread.pool.min</code> system property.
     */
    public static final int POOL_MIN = Options.THREADPOOL_MIN.load();
    /**
     * Thread pool time-to-live in seconds.
     *
     * Set with the <code>jruby.thread.pool.max</code> system property.
     */
    public static final int POOL_TTL = Options.THREADPOOL_TTL.load();
    /**
     * Fiber thread pool time-to-live in seconds.
     *
     * Set with the <code>jruby.fiber.thread.pool.max</code> system property.
     */
    public static final int FIBER_POOL_TTL = Options.FIBER_THREADPOOL_TTL.load();

    /**
     * Enable tracing of method calls.
     *
     * Set with the <code>jruby.debug.fullTrace</code> system property.
     */
    public static boolean FULL_TRACE_ENABLED = Options.DEBUG_FULLTRACE.load();

    /**
     * Comma-separated list of methods to exclude from JIT compilation.
     * Specify as "Module", "Module#method" or "method".
     *
     * Also supports excluding based on implementation_file.rb syntax.
     *
     * Set with the <code>jruby.jit.exclude</code> system property.
     */
    public static final String COMPILE_EXCLUDE = Options.JIT_EXCLUDE.load();

    /**
     * Indicates the global default for whether native code is enabled. Default
     * is true. This value is used to default new runtime configurations.
     *
     * Set with the <code>jruby.native.enabled</code> system property.
     */
    public static final boolean NATIVE_ENABLED = Options.NATIVE_ENABLED.load();

    @Deprecated(since = "9.0.0.0")
    public final static boolean CEXT_ENABLED = false;

    /**
     * Whether to reify (pre-compile and generate) a Java class per Ruby class.
     *
     * Set with the <code>jruby.reify.classes</code> system property.
     */
    public static final boolean REIFY_RUBY_CLASSES = Options.REIFY_CLASSES.load();

    /**
     * Log errors that occur during reification.
     *
     * Set with the <code>jruby.reify.logErrors</code> system property.
     */
    public static final boolean REIFY_LOG_ERRORS = Options.REIFY_LOGERRORS.load();

    /**
     * Turn on debugging of the load service (requires and loads).
     *
     * Set with the <code>jruby.debug.loadService</code> system property.
     */
    public static final boolean DEBUG_LOAD_SERVICE = Options.DEBUG_LOADSERVICE.load();

    /**
     * Turn on timings of the load service (requires and loads).
     *
     * Set with the <code>jruby.debug.loadService.timing</code> system property.
     */
    public static final boolean DEBUG_LOAD_TIMINGS = Options.DEBUG_LOADSERVICE_TIMING.load();

    /**
     * Turn on debugging of subprocess launching.
     *
     * Set with the <code>jruby.debug.launch</code> system property.
     */
    public static final boolean DEBUG_LAUNCHING = Options.DEBUG_LAUNCH.load();

    /**
     * Turn on debugging of script resolution with "-S".
     *
     * Set with the <code>jruby.debug.scriptResolution</code> system property.
     */
    public static final boolean DEBUG_SCRIPT_RESOLUTION = Options.DEBUG_SCRIPTRESOLUTION.load();

    public static final boolean DEBUG_PARSER = Options.DEBUG_PARSER.load();

    public static final boolean JUMPS_HAVE_BACKTRACE = Options.JUMP_BACKTRACE.load();

    public static final boolean NO_UNWRAP_PROCESS_STREAMS = Options.PROCESS_NOUNWRAP.load();

    public static final boolean INTERFACES_USE_PROXY = Options.INTERFACES_USEPROXY.load();

    public static final boolean SET_ACCESSIBLE = Options.JI_SETACCESSIBLE.load();

    // properties for logging exceptions, backtraces, and caller invocations
    public static final boolean LOG_EXCEPTIONS = Options.LOG_EXCEPTIONS.load();
    public static final boolean LOG_BACKTRACES = Options.LOG_BACKTRACES.load();
    public static final boolean LOG_CALLERS = Options.LOG_CALLERS.load();
    public static final boolean LOG_WARNINGS = Options.LOG_WARNINGS.load();

    public static final boolean ERRNO_BACKTRACE = Options.ERRNO_BACKTRACE.load();
    public static final boolean STOPITERATION_BACKTRACE = Options.STOPITERATION_BACKTRACE.load();

    public static boolean IR_DEBUG = Options.IR_DEBUG.load(); // ast tool can toggle this
    public static final String IR_DEBUG_IGV = Options.IR_DEBUG_IGV.load();
    public static final boolean IR_DEBUG_IGV_STDOUT = Options.IR_DEBUG_IGV_STDOUT.load();
    public static final boolean IR_PROFILE = Options.IR_PROFILE.load();
    public static boolean IR_COMPILER_DEBUG = Options.IR_COMPILER_DEBUG.load(); // ast tool can toggle this
    public static String IR_PRINT_PATTERN = Options.IR_PRINT_PATTERN.load();
    public static final boolean IR_WRITING = Options.IR_WRITING.load();
    public static final boolean IR_READING = Options.IR_READING.load();
    public static final boolean IR_READING_DEBUG = Options.IR_READING_DEBUG.load();
    public static final boolean IR_WRITING_DEBUG = Options.IR_WRITING_DEBUG.load();
    public static final boolean IR_VISUALIZER = Options.IR_VISUALIZER.load();
    public static final boolean IR_UNBOXING = Options.IR_UNBOXING.load();
    public static final String IR_COMPILER_PASSES = Options.IR_COMPILER_PASSES.load();
    public static final String IR_JIT_PASSES = Options.IR_JIT_PASSES.load();
    public static String IR_INLINE_COMPILER_PASSES = "";
    public static boolean RECORD_LEXICAL_HIERARCHY = false;

    /**
     * Whether to calculate consistent hashes across JVM instances, or to ensure
     * un-predicatable hash values using SecureRandom.
     *
     * Set with the <code>jruby.consistent.hashing.enabled</code> system property.
     */
    public static final boolean CONSISTENT_HASHING_ENABLED = Options.CONSISTENT_HASHING.load();

    private static volatile boolean loadedNativeExtensions = false;

    ////////////////////////////////////////////////////////////////////////////
    // Static initializers
    ////////////////////////////////////////////////////////////////////////////

    private static int initJavaBytecodeVersion() {
        final String specVersion = Options.BYTECODE_VERSION.load();

        int version = Integer.parseInt(specVersion);
        switch (version) {
            default:
            case 21:
            case 22:
                return Opcodes.V21;
        }
    }

    @Deprecated(since = "1.7.0")
    public void setSafeLevel(int safeLevel) {
    }

    @Deprecated(since = "1.7.0")
    public String getInPlaceBackupExtention() {
        return inPlaceBackupExtension;
    }

    @Deprecated(since = "1.7.5")
    public String getBasicUsageHelp() {
        return OutputStrings.getBasicUsageHelp();
    }

    @Deprecated(since = "1.7.5")
    public String getExtendedHelp() {
        return OutputStrings.getExtendedHelp();
    }

    @Deprecated(since = "1.7.5")
    public String getPropertyHelp() {
        return OutputStrings.getPropertyHelp();
    }

    @Deprecated(since = "1.7.5")
    public String getVersionString() {
        return OutputStrings.getVersionString();
    }

    @Deprecated(since = "1.7.5")
    public String getCopyrightString() {
        return OutputStrings.getCopyrightString();
    }

    @Deprecated(since = "1.7.5")
    public Collection<String> requiredLibraries() {
        return requiredLibraries;
    }

    @Deprecated(since = "1.7.5")
    public List<String> loadPaths() {
        return loadPaths;
    }

    @Deprecated(since = "1.7.5")
    public boolean shouldPrintUsage() {
        return shouldPrintUsage;
    }

    @Deprecated(since = "1.7.5")
    public boolean shouldPrintProperties() {
        return shouldPrintProperties;
    }

    @Deprecated(since = "1.7.5")
    public Boolean getVerbose() {
        return isVerbose();
    }

    @Deprecated(since = "1.7.5")
    public boolean shouldRunInterpreter() {
        return isShouldRunInterpreter();
    }

    @Deprecated(since = "1.7.5")
    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    @Deprecated(since = "1.7.5")
    public boolean isxFlag() {
        return xFlag;
    }

    /**
     * The max count of active methods eligible for JIT-compilation.
     */
    @Deprecated(since = "1.7.5")
    public static final int JIT_MAX_METHODS_LIMIT = Constants.JIT_MAX_METHODS_LIMIT;

    /**
     * The max size of JIT-compiled methods (full class size) allowed.
     */
    @Deprecated(since = "1.7.5")
    public static final int JIT_MAX_SIZE_LIMIT = Constants.JIT_MAX_SIZE_LIMIT;

    /**
     * The JIT threshold to the specified method invocation count.
     */
    @Deprecated(since = "1.7.5")
    public static final int JIT_THRESHOLD = Constants.JIT_THRESHOLD;

    /**
     * Default size for chained compilation.
     */
    @Deprecated(since = "1.7.5")
    public static final int CHAINED_COMPILE_LINE_COUNT_DEFAULT = Constants.CHAINED_COMPILE_LINE_COUNT_DEFAULT;

    @Deprecated(since = "1.7.5")
    public static final boolean nativeEnabled = NATIVE_ENABLED;

    @Deprecated(since = "1.7.5")
    public boolean isSamplingEnabled() {
        return false;
    }

    @Deprecated(since = "1.7.5")
    public void setBenchmarking(boolean benchmarking) {
    }

    @Deprecated(since = "1.7.5")
    public boolean isBenchmarking() {
        return false;
    }

    @Deprecated(since = "9.0.0.0")
    public void setCextEnabled(boolean b) {
    }

    @Deprecated(since = "9.0.0.0")
    public boolean isCextEnabled() {
        return false;
    }

    @Deprecated(since = "1.7.22") public static final String JIT_CODE_CACHE = "";

    @Deprecated(since = "9.3.0.0")
    public boolean isJitDumping() {
        return jitDumping;
    }

    @Deprecated(since = "9.3.0.0")
    public String getThreadDumpSignal() {
        return threadDumpSignal;
    }

    @Deprecated(since = "9.3.0.0")
    public boolean isGlobalRequireLock() {
        return globalRequireLock;
    }

    @Deprecated(since = "9.3.0.0")
    public void setGlobalRequireLock(boolean globalRequireLock) {
        this.globalRequireLock = globalRequireLock;
    }

    @Deprecated(since = "9.2.6.0")
    public static final boolean NATIVE_NET_PROTOCOL = Options.NATIVE_NET_PROTOCOL.load();

    @Deprecated(since = "9.2.9.0")
    public static final boolean CAN_SET_ACCESSIBLE = Options.JI_SETACCESSIBLE.load();

    @Deprecated(since = "9.3.0.0")
    public static boolean THREADLESS_COMPILE_ENABLED = false;

    @Deprecated(since = "9.3.0.0")
    public static final int CHAINED_COMPILE_LINE_COUNT = 500;

    @Deprecated(since = "9.3.0.0")
    public static final boolean PEEPHOLE_OPTZ = true;

    @Deprecated(since = "9.3.0.0")
    public static boolean NOGUARDS_COMPILE_ENABLED = false;

    @Deprecated(since = "9.3.0.0")
    public static final boolean FASTEST_COMPILE_ENABLED = false;

    @Deprecated(since = "9.3.0.0")
    public static boolean FASTSEND_COMPILE_ENABLED = false;

    @Deprecated(since = "9.3.0.0")
    public static boolean FAST_MULTIPLE_ASSIGNMENT = false;

    @Deprecated(since = "9.3.0.0")
    private final boolean jitDumping = false;

    @Deprecated(since = "9.3.0.0")
    public static final boolean JIT_LOADING_DEBUG = false;

    @Deprecated(since = "9.3.0.0")
    public static final boolean JIT_CACHE_ENABLED = false;

    @Deprecated(since = "9.3.0.0")
    private boolean runRubyInProcess = true;

    @Deprecated(since = "9.3.0.0")
    public static final boolean REFLECTED_HANDLES = false;

    @Deprecated(since = "9.3.0.0")
    private String threadDumpSignal = null;

    @Deprecated(since = "9.3.0.0")
    public static final boolean COROUTINE_FIBERS = false;

    @Deprecated(since = "9.3.0.0")
    private boolean globalRequireLock = false;

    @Deprecated(since = "9.3.0.0")
    public static final boolean USE_GENERATED_HANDLES = false;

    @Deprecated(since = "9.4.3.0")
    public static final boolean FASTOPS_COMPILE_ENABLED = Options.COMPILE_FASTOPS.load();

    @Deprecated
    public boolean isDisableGems() {
        return !enableGems;
    }

    @Deprecated(since = "10.0.3.0")
    public void setDisableGems(boolean dg) {
        this.enableGems = !dg;
    }

    @Deprecated(since = "10.0.3.0")
    public boolean isDisableDidYouMean() {
        return !enableDidYouMean;
    }

    @Deprecated(since = "10.0.3.0")
    public void setDisableDidYouMean(boolean ddym) {
        this.enableDidYouMean = !ddym;
    }

    @Deprecated(since = "10.0.3.0")
    public boolean isDisableErrorHighlight() {
        return !enableErrorHighlight;
    }

    @Deprecated(since = "10.0.3.0")
    public void setDisableErrorHighlight(boolean deh) {
        this.enableErrorHighlight = !deh;
    }

    @Deprecated(since = "10.0.3.0")
    public boolean isDisableSyntaxSuggest() {
        return !enableSyntaxSuggest;
    }

    @Deprecated(since = "10.0.3.0")
    public void setDisableSyntaxSuggest(boolean dss) {
        this.enableSyntaxSuggest = !dss;
    }

    @Deprecated(since = "10.0.3.0")
    public void setDisableRUBYOPT(boolean dr) {
        this.enableRUBYOPT = !dr;
    }
}
