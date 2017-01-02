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
package org.jruby.truffle.options;

import com.oracle.truffle.api.TruffleOptions;
import jnr.posix.util.Platform;
import org.jruby.truffle.core.string.KCode;
import org.jruby.truffle.core.string.StringSupport;
import org.jruby.truffle.language.control.JavaException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A structure used to configure new JRuby instances. All publicly-tweakable
 * aspects of Ruby can be modified here, including those settable by command-
 * line options, those available through JVM properties, and those suitable for
 * embedding.
 */
@SuppressWarnings("unused")
public class RubyInstanceConfig {

    public RubyInstanceConfig(boolean isSecurityRestricted) {
        this.isSecurityRestricted = isSecurityRestricted;
        currentDirectory = isSecurityRestricted ? "/" : getFileProperty("user.dir");

        if (isSecurityRestricted) {
            //
        } else {
            if (COMPILE_EXCLUDE != null) {
                excludedMethods.addAll(StringSupport.split(COMPILE_EXCLUDE, ','));
            }
        }
        initEnvironment();
    }

    public static String getFileProperty(String property) {
        return normalizeSeps(System.getProperty(property, "/"));
    }

    public static String normalizeSeps(String path) {
        if (Platform.IS_WINDOWS) {
            return path.replace(File.separatorChar, '/');
        }
        return path;
    }

    private void initEnvironment() {
        environment = new HashMap<>();
        try {
            environment.putAll(System.getenv());
        }
        catch (SecurityException se) { /* ignore missing getenv permission */ }
        setupEnvironment(getJRubyHome());
    }

    public void processArguments(String[] arguments) {
        final ArgumentProcessor processor = new ArgumentProcessor(arguments, this);
        processor.processArguments();
        tryProcessArgumentsWithRubyopts();
        if (!TruffleOptions.AOT && !hasScriptArgv && System.console() != null) {
            setScriptFileName(processor.resolveScript("irb"));
        }
    }

    public void tryProcessArgumentsWithRubyopts() {
        try {
            processArgumentsWithRubyopts();
        } catch (SecurityException se) {
            // ignore and do nothing
        }
    }

    public void processArgumentsWithRubyopts() {
        // environment defaults to System.getenv normally
        Object rubyoptObj = environment.get("RUBYOPT");
        String rubyopt = rubyoptObj == null ? null : rubyoptObj.toString();

        if (rubyopt == null || rubyopt.length() == 0) return;

        String[] rubyoptArgs = rubyopt.split("\\s+");
        if (rubyoptArgs.length != 0) {
            new ArgumentProcessor(rubyoptArgs, false, true, true, this).processArguments();
        }
    }

    private static final Pattern RUBY_SHEBANG = Pattern.compile("#!.*ruby.*");

    protected static boolean isRubyShebangLine(String line) {
        return RUBY_SHEBANG.matcher(line).matches();
    }

    private static final String URI_PREFIX_STRING = "^(uri|jar|file|classpath):([^:/]{2,}:([^:/]{2,}:)?)?";
    public static Pattern PROTOCOL_PATTERN = Pattern.compile(URI_PREFIX_STRING + ".*");

    private String calculateJRubyHome() {
        String newJRubyHome = null;

        // try the normal property first
        if (!isSecurityRestricted) {
            newJRubyHome = System.getProperty("jruby.home", null);
        }

        if (!TruffleOptions.AOT && newJRubyHome == null && getLoader().getResource("META-INF/jruby.home/.jrubydir") != null) {
            newJRubyHome = "uri:classloader://META-INF/jruby.home";
        }
        if (newJRubyHome != null) {
            // verify it if it's there
            newJRubyHome = verifyHome(newJRubyHome, error);
        } else {
            try {
                newJRubyHome = System.getenv("JRUBY_HOME");
            } catch (Exception e) {}

            if (newJRubyHome != null) {
                // verify it if it's there
                newJRubyHome = verifyHome(newJRubyHome, error);
            } else {
                // otherwise fall back on system temp location
                newJRubyHome = System.getProperty("java.io.tmpdir", null);
            }
        }

        // RegularFileResource absolutePath will canonicalize resources so that will change c: paths to C:.
        // We will cannonicalize on windows so that jruby.home is also C:.
        // assume all those uri-like pathnames are already in absolute form
        if (Platform.IS_WINDOWS && !PROTOCOL_PATTERN.matcher(newJRubyHome).matches()) {
            try {
                newJRubyHome = new File(newJRubyHome).getCanonicalPath();
            }
            catch (IOException e) {} // just let newJRubyHome stay the way it is if this fails
        }

        return newJRubyHome == null ? null : normalizeSeps(newJRubyHome);
    }

    // We require the home directory to be absolute
    private static String verifyHome(String home, PrintStream error) {
        if ("uri:classloader://META-INF/jruby.home".equals(home) || "uri:classloader:/META-INF/jruby.home".equals(home)) {
            return home;
        }
        if (home.equals(".")) {
            home = System.getProperty("user.dir", null);
        }
        else if (home.startsWith("cp:")) {
            home = home.substring(3);
        }
        if (home.startsWith("jar:") || ( home.startsWith("file:") && home.contains(".jar!/") ) ||
                home.startsWith("classpath:") || home.startsWith("uri:")) {
            error.println("Warning: JRuby home with uri like paths may not have full functionality - use at your own risk");
        }
        // do not normalize on plain jar like pathes coming from jruby-rack
        else if (!home.contains(".jar!/") && !home.startsWith("uri:")) {
            File file = new File(home);
            if (!file.exists()) {
                final String tmpdir = System.getProperty("java.io.tmpdir", null);
                error.println("Warning: JRuby home \"" + file + "\" does not exist, using " + tmpdir);
                return tmpdir;
            }
            if (!file.isAbsolute()) {
                home = file.getAbsolutePath();
            }
        }
        return home;
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
                return System.in;
            } else {
                final String script = getScriptFileName();
                return new FileInputStream(script);
            }
        } catch (IOException e) {
            throw new JavaException(e);
        }
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
    // Getters and setters for config settings.
    ////////////////////////////////////////////////////////////////////////////

    public String getJRubyHome() {
        if (jrubyHome == null) {
            jrubyHome = calculateJRubyHome();
        }
        return jrubyHome;
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

    public void setEnvironment(Map<String, String> newEnvironment) {
        environment = new HashMap<>();
        if (newEnvironment != null) {
            environment.putAll(newEnvironment);
        }
        setupEnvironment(getJRubyHome());
    }

    private void setupEnvironment(String jrubyHome) {
        if (PROTOCOL_PATTERN.matcher(jrubyHome).matches() && !environment.containsKey("RUBY")) {
            // the assumption that if JRubyHome is not a regular file that jruby
            // got launched in an embedded fashion
            //environment.put("RUBY", ClasspathLauncher.jrubyCommand(defaultClassLoader()));
            throw new UnsupportedOperationException();
        }
    }

    public Map<String, String> getEnvironment() {
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

    public Collection<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public List<String> getLoadPaths() {
        return loadPaths;
    }

    public void setShouldPrintUsage(boolean shouldPrintUsage) {
        this.shouldPrintUsage = shouldPrintUsage;
    }

    public boolean getShouldPrintUsage() {
        return shouldPrintUsage;
    }

    public void setShouldPrintProperties(boolean shouldPrintProperties) {
        this.shouldPrintProperties = shouldPrintProperties;
    }

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

    public void setAssumeLoop(boolean assumeLoop) {
        this.assumeLoop = assumeLoop;
    }

    public void setAssumePrinting(boolean assumePrinting) {
        this.assumePrinting = assumePrinting;
    }

    public void setProcessLineEnds(boolean processLineEnds) {
        this.processLineEnds = processLineEnds;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public boolean isSplit() {
        return split;
    }

    public void setVerbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    public boolean isVerbose() {
        return verbosity == Verbosity.TRUE;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowBytecode(boolean showBytecode) {
        this.showBytecode = showBytecode;
    }

    public void setShowCopyright(boolean showCopyright) {
        this.showCopyright = showCopyright;
    }

    public boolean isShowCopyright() {
        return showCopyright;
    }

    public void setShouldRunInterpreter(boolean shouldRunInterpreter) {
        this.shouldRunInterpreter = shouldRunInterpreter;
    }

    public boolean getShouldRunInterpreter() {
        return shouldRunInterpreter && (hasScriptArgv || !showVersion);
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

    public void setInPlaceBackupExtension(String inPlaceBackupExtension) {
        this.inPlaceBackupExtension = inPlaceBackupExtension;
    }

    public String getInPlaceBackupExtension() {
        return inPlaceBackupExtension;
    }

    public Map<String, String> getOptionGlobals() {
        return optionGlobals;
    }

    public boolean isArgvGlobalsOn() {
        return argvGlobalsOn;
    }

    public void setArgvGlobalsOn(boolean argvGlobalsOn) {
        this.argvGlobalsOn = argvGlobalsOn;
    }

    public boolean isDisableGems() {
        return disableGems;
    }

    public void setDisableGems(boolean dg) {
        this.disableGems = dg;
    }

    public void setXFlag(boolean xFlag) {
        this.xFlag = xFlag;
    }

    public boolean isXFlag() {
        return xFlag;
    }

    public boolean isFrozenStringLiteral() {
        return frozenStringLiteral;
    }

    public void setFrozenStringLiteral(boolean frozenStringLiteral) {
        this.frozenStringLiteral = frozenStringLiteral;
    }


    public static ClassLoader defaultClassLoader() {
        if (TruffleOptions.AOT) {
            return null;
        }

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
    private boolean xFlag = false;

    /**
     * Indicates whether the script has a shebang line or not
     */
    private InputStream input          = System.in;
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;

    private String currentDirectory;

    /** Environment variables; defaults to System.getenv() in constructor */
    private Map<String, String> environment;
    private String[] argv = {};

    private String internalEncoding = null;
    private String externalEncoding = null;

    private ClassLoader loader = defaultClassLoader();

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<>();
    private Set<String> excludedMethods = new HashSet<>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private Collection<String> requiredLibraries = new LinkedHashSet<>();
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private Map<String, String> optionGlobals = new HashMap<>();
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
    private boolean shouldCheckSyntax = false;
    private String inputFieldSeparator = null;
    private String inPlaceBackupExtension = null;
    private boolean disableGems = false;
    private boolean hasScriptArgv = false;
    private boolean frozenStringLiteral = false;
    private String jrubyHome;
    private KCode kcode;
    private String sourceEncoding;

    private boolean forceStdin = false;

    ////////////////////////////////////////////////////////////////////////////
    // Static configuration fields, used as defaults for new JRuby instances.
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Comma-separated list of methods to exclude from JIT compilation.
     * Specify as "Module", "Module#method" or "method".
     *
     * Set with the <tt>jruby.jit.exclude</tt> system property.
     */
    public static final String COMPILE_EXCLUDE = "";

    /**
     * Indicates the global default for whether native code is enabled. Default
     * is true. This value is used to default new runtime configurations.
     *
     * Set with the <tt>jruby.native.enabled</tt> system property.
     */
    public static final boolean NATIVE_ENABLED = false;

    @Deprecated
    public final static boolean CEXT_ENABLED = false;

    /**
     * Turn on debugging of script resolution with "-S".
     *
     * Set with the <tt>jruby.debug.scriptResolution</tt> system property.
     */
    public static final boolean DEBUG_SCRIPT_RESOLUTION = false;

    @Deprecated
    public static final boolean JIT_CACHE_ENABLED = false;

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

    public Verbosity getVerbosity() {
        return verbosity;
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

    @Deprecated public static final String JIT_CODE_CACHE = "";

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }
}
