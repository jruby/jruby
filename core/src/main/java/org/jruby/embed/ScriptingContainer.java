/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2013 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

import java.io.UnsupportedEncodingException;
import org.jruby.embed.internal.LocalContextProvider;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.CompatVersion;
import org.jruby.Profile;
import org.jruby.Ruby;
import org.jruby.RubyGlobal.InputGlobalVariable;
import org.jruby.RubyGlobal.OutputGlobalVariable;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.RubyInstanceConfig.LoadServiceCreator;
import org.jruby.RubyInstanceConfig.ProfilingMode;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.internal.ConcurrentLocalContextProvider;
import org.jruby.embed.internal.EmbedRubyInterfaceAdapterImpl;
import org.jruby.embed.internal.EmbedRubyObjectAdapterImpl;
import org.jruby.embed.internal.EmbedRubyRuntimeAdapterImpl;
import org.jruby.embed.internal.SingleThreadLocalContextProvider;
import org.jruby.embed.internal.SingletonLocalContextProvider;
import org.jruby.embed.internal.ThreadSafeLocalContextProvider;
import org.jruby.embed.io.ReaderInputStream;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.profile.builtin.ProfileOutput;
import org.jruby.util.KCode;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.cli.Options;

/**
 * ScriptingContainer provides various methods and resources that are useful
 * for embedding Ruby in Java. Using this class, users can run Ruby scripts from
 * Java programs easily. Also, users can use methods defined or implemented by Ruby.
 *
 * ScriptingContainer allows users to set various configuration parameters.
 * Some of them are per-container properties, while others are per-evaluation attributes.
 * For example, a local context scope, local variable behavior, load paths are
 * per-container properties. Please see {@link PropertyName} and {@link AttributeName}
 * for more details. Be aware that the per-container properties should be set prior to
 * get Ruby runtime be instantiated; otherwise, default values are applied to.
 * ScriptingContainer delays Ruby runtime initialization as much as possible to
 * improve startup time. When values are put into the ScriptingContainer, or runScriptlet
 * method gets run Ruby runtime is created internally. However, the default, singleton
 * local context scope behave slightly different. If Ruby runtime has been already instantiated
 * by another ScriptingContainer, application, etc, the same runtime will be used.
 *
 * Below are examples.
 *
 * The first Example is a very simple Hello World. After initializing a ScriptingContainer,
 * a Ruby script, puts "Hello World!", runs and produces "Hello World!."
 * <pre>Example 1:
 *
 *         ScriptingContainer container = new ScriptingContainer();
 *         container.runScriptlet("puts \"Hello World!\"");
 *
 * Produces:
 * Hello World!</pre>
 *
 * The second example shows how to share variables between Java and Ruby.
 * In this example, a local variable "x" is shared. To make this happen, a local variable
 * behavior should be transient or persistent. As for JSR223 JRuby engine, set these
 * types using System property, org.jruby.embed.localvariable.behavior. If the local
 * variable behavior is one of transient or persistent,
 * Ruby's local, instance, global variables and constants are available to share
 * between Java and Ruby. (A class variable sharing does not work on current version)
 * Thus, "x" in Java is also "x" in Ruby.
 *
 * <pre>Example 2:
 *
 *         ScriptingContainer container = new ScriptingContainer();
 *         container.put("x", 12345);
 *         container.runScriptlet("puts x.to_s(2)");
 *
 * Produces:
 * 11000000111001</pre>
 *
 * The third examples shows how to keep local variables across multiple evaluations.
 * This feature simulates BSF engine for JRuby. In terms of Ruby semantics,
 * local variables should not survive after the evaluation has completed. Thus,
 * this behavior is optional, and users need to specify LocalVariableBehavior.PERSISTENT
 * when the container is instantiated.
 *
 * <pre>Example 3:
 *
 *         ScriptingContainer container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
 *         container.runScriptlet("p=9.0");
 *         container.runScriptlet("q = Math.sqrt p");
 *         container.runScriptlet("puts \"square root of #{p} is #{q}\"");
 *         System.out.println("Ruby used values: p = " + container.get("p") +
 *               ", q = " + container.get("q"));
 *
 * Produces:
 * square root of 9.0 is 3.0
 * Ruby used values: p = 9.0, q = 3.0</pre>
 *
 * Also, ScriptingContainer provides better i18n support. For example,
 * Unicode Escape Sequence can be included in Ruby scripts.
 *
 * <p>In addition, ScriptingContainer supports a parse-once-eval-many-times feature,
 * invoking methods defined by Ruby, and getting an instance of a specified interface
 * that has been implemented by Ruby.
 *
 * <pre>Example 4:
 *         ScriptingContainer container = new ScriptingContainer();
 *         script =
 *          "def message\n" +
 *              "\"message: #{@message}\"\n" +
 *          "end\n" +
 *          "message";
 *         container.put("@message", "What's up?");
 *         EvalUnit unit = container.parse(script);
 *         IRubyObject ret = unit.run();
 *         System.out.println(JavaEmbedUtils.rubyToJava(ret));
 *         container.put("@message", "Fabulous!");
 *         ret = unit.run();
 *         System.out.println(JavaEmbedUtils.rubyToJava(ret));
 *         container.put("@message", "That's the way you are.");
 *         ret = unit.run();
 *         System.out.println(JavaEmbedUtils.rubyToJava(ret));
 *
 * Produces:
 *     message: What's up?
 *     message: Fabulous!
 *     message: That's the way you are.</pre>
 *
 * See more details at project's
 * {@see <a href="https://github.com/jruby/jruby/wiki/RedBridge">Wiki</a>}
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class ScriptingContainer implements EmbedRubyInstanceConfigAdapter {
    private final Map<String, String[]> basicProperties;
    private final LocalContextScope scope;
    private final LocalContextProvider provider;
    private final EmbedRubyRuntimeAdapter runtimeAdapter = new EmbedRubyRuntimeAdapterImpl(this);
    private final EmbedRubyObjectAdapter objectAdapter = new EmbedRubyObjectAdapterImpl(this);
    private final EmbedRubyInterfaceAdapter interfaceAdapter = new EmbedRubyInterfaceAdapterImpl(this);

    /**
     * Constructs a ScriptingContainer with a default values.
     */
    public ScriptingContainer() {
        this(LocalContextScope.SINGLETON, LocalVariableBehavior.TRANSIENT, true);
    }

    /**
     * Constructs a ScriptingContainer with a specified local context type.
     *
     * @param scope a local context type.
     */
    public ScriptingContainer(LocalContextScope scope) {
        this(scope, LocalVariableBehavior.TRANSIENT, true);
    }

    /**
     * Constructs a ScriptingContainer with a specified local variable behavior.
     *
     * @param behavior a local variable behavior
     */
    public ScriptingContainer(LocalVariableBehavior behavior) {
        this(LocalContextScope.SINGLETON, behavior, true);
    }

    /**
     * Constructs a ScriptingContainer with a specified local context type and
     * variable behavior.
     *
     * @param scope a local context type
     * @param behavior a local variable behavior
     */
    public ScriptingContainer(LocalContextScope scope, LocalVariableBehavior behavior) {
        this(scope, behavior, true);
    }

    /**
     * Constructs a ScriptingContainer with a specified local context scope,
     * local variable behavior and laziness.
     *
     * @param scope is one of a local context scope defined by {@link LocalContextScope}
     * @param behavior is one of a local variable behavior defined by {@link LocalVariableBehavior}
     * @param lazy is a switch to do lazy retrieval of variables/constants from
     *        Ruby runtime. Default is true. When this value is true, ScriptingContainer tries to
     *        get as many variables/constants as possible from Ruby runtime.
     */
    public ScriptingContainer(LocalContextScope scope, LocalVariableBehavior behavior, boolean lazy) {
        this.provider = getProviderInstance(scope, behavior, lazy);
        this.scope = scope;
        try {
            initRubyInstanceConfig();
        }
        catch (RaiseException ex) {
            // TODO this seems useless - except that we get a Java stack trace
            throw new RuntimeException(ex);
        }
        basicProperties = getBasicProperties();
    }

    static LocalContextProvider getProviderInstance(LocalContextScope scope, LocalVariableBehavior behavior, boolean lazy) {
        switch(scope) {
            case THREADSAFE :
                return new ThreadSafeLocalContextProvider(behavior, lazy);
            case CONCURRENT :
                return new ConcurrentLocalContextProvider(behavior, lazy);
            case SINGLETHREAD :
                return new SingleThreadLocalContextProvider(behavior, lazy);
            case SINGLETON :
            default :
                return SingletonLocalContextProvider.getProvider(behavior, lazy);
        }
    }
    
    private void initRubyInstanceConfig() throws RaiseException {
        String home = SystemPropertyCatcher.findJRubyHome(this);
        if (home != null) {
        	provider.getRubyInstanceConfig().setJRubyHome(home);
        }
        provider.getRubyInstanceConfig().setScriptFileName("<script>");
    }

    // maybe these properties are not used at all?
    private static Map<String, String[]> getBasicProperties() {
        Map<String, String[]> properties = new HashMap<String, String[]>();
        properties.put("container.ids", new String[]{"ruby", "jruby"});
        properties.put("language.extension", new String[]{"rb"});
        properties.put("language.name", new String[]{"ruby"});
        properties.put("language.mimetypes", new String[]{"application/x-ruby"});
        return properties;
    }

    /**
     * Returns a list of load paths for Ruby scripts/libraries. If no paths is
     * given, the list is created from java.class.path System property.
     *
     * @since JRuby 1.5.0.
     *
     * @return a list of load paths.
     */
    public List<String> getLoadPaths() {
        return provider.getRubyInstanceConfig().getLoadPaths();
    }

    /**
     * Changes a list of load paths Ruby scripts/libraries. The default value
     * is an empty array. If no paths is given, the list is created from
     * java.class.path System property. This value can be set by
     * org.jruby.embed.class.path System property, also.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given paths will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param paths a new list of load paths.
     */
    public void setLoadPaths(List<String> paths) {
        provider.getRubyInstanceConfig().setLoadPaths(paths);
    }

    /**
     * Returns an input stream assigned to STDIN and $stdin.
     *
     * @since JRuby 1.5.0.
     *
     * @return input stream of STDIN and $stdin
     */
    public InputStream getInput() {
        return provider.getRubyInstanceConfig().getInput();
    }

    /**
     * Changes STDIN and $stdin to a given input stream. The default standard input
     * is java.lang.System.in.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given input stream will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param istream an input stream to be set
     */
    public void setInput(InputStream istream) {
        provider.getRubyInstanceConfig().setInput(istream);
    }

    /**
     * Changes STDIN and $stdin to a given reader. No reader is set by default.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given reader will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param reader a reader to be set
     */
    public void setInput(Reader reader) {
        if (reader == null) {
            provider.getRubyInstanceConfig().setInput(null);
        } else {
            ReaderInputStream istream = new ReaderInputStream(reader);
            provider.getRubyInstanceConfig().setInput(istream);
        }
    }

    /**
     * Returns an output stream assigned to STDOUT and $stdout.
     *
     * @since JRuby 1.5.0.
     *
     * @return an output stream of STDOUT and $stdout
     */
    public PrintStream getOutput() {
        return provider.getRubyInstanceConfig().getOutput();
    }

    /**
     * Changes STDOUT and $stdout to a given output stream. The default standard
     * output is java.lang.System.out.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given output stream will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param pstream an output stream to be set
     */
    public void setOutput(PrintStream pstream) {
        provider.getRubyInstanceConfig().setOutput(pstream);
    }

    /**
     * Changes STDOUT and $stdout to a given writer. No writer is set by default.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given writer will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param writer a writer to be set
     */
    public void setOutput(Writer writer) {
        if (writer == null) {
            provider.getRubyInstanceConfig().setOutput(null);
        } else {
            WriterOutputStream ostream = new WriterOutputStream(writer);
            PrintStream pstream = new PrintStream(ostream);
            provider.getRubyInstanceConfig().setOutput(pstream);
        }
    }

    /**
     * Returns an error stream assigned to STDERR and $stderr.
     *
     * @since JRuby 1.5.0.
     *
     * @return output stream for error stream
     */
    public PrintStream getError() {
        return provider.getRubyInstanceConfig().getError();
    }

    /**
     * Changes STDERR and $stderr to a given print stream. The default standard error
     * is java.lang.System.err.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given print stream will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param pstream a print stream to be set
     */
    public void setError(PrintStream pstream) {
        provider.getRubyInstanceConfig().setError(pstream);
    }

    /**
     * Changes STDERR and $stderr to a given writer. No writer is set by default.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given writer will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param writer a writer to be set
     */
    public void setError(Writer writer) {
        if (writer == null) {
            provider.getRubyInstanceConfig().setError(null);
        } else {
            WriterOutputStream ostream = new WriterOutputStream(writer);
            PrintStream pstream = new PrintStream(ostream);
            provider.getRubyInstanceConfig().setError(pstream);
        }
    }

    /**
     * Returns a compile mode currently chosen, which is one of CompileMode.JIT,
     * CompileMode.FORCE, CompileMode.OFF. The default mode is CompileMode.OFF
     * if CompatVersion.RUBY1_9 is chosen, otherwise, CompileMode.JIT. Also,
     * CompileMode.OFF is chosen when a security restriction is set.
     *
     * @since JRuby 1.5.0.
     *
     * @return a compile mode.
     */
    public CompileMode getCompileMode() {
        return provider.getRubyInstanceConfig().getCompileMode();
    }

    /**
     * Changes a compile mode to a given mode, which should be one of CompileMode.JIT,
     * CompileMode.FORCE, CompileMode.OFF.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given mode will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param mode compile mode
     */
    public void setCompileMode(CompileMode mode) {
        provider.getRubyInstanceConfig().setCompileMode(mode);
    }

    /**
     * Tests whether Ruby runs in a process or not.
     *
     * @since JRuby 1.5.0.
     *
     * @return true if Ruby is configured to run in a process, otherwise, false.
     */
    public boolean isRunRubyInProcess() {
        return provider.getRubyInstanceConfig().isRunRubyInProcess();
    }

    /**
     * Changes the value to determine whether Ruby runs in a process or not. The
     * default value is true.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given condition will be set.
     *
     * @since JRuby 1.5.0.
     *
     * @param inprocess true when Ruby is set to run in the process, or false not to
     * run in the process.
     */
    public void setRunRubyInProcess(boolean inprocess) {
        provider.getRubyInstanceConfig().setRunRubyInProcess(inprocess);
    }

    @Deprecated
    public CompatVersion getCompatVersion() {
        return provider.getRubyInstanceConfig().getCompatVersion();
    }

    @Deprecated
    public void setCompatVersion(CompatVersion version) {
    }

    /**
     * Tests whether the Object Space is enabled or not.
     *
     * @since JRuby 1.5.0.
     *
     * @return true if the Object Space is able to use, otherwise, false.
     */
    public boolean isObjectSpaceEnabled() {
        return provider.getRubyInstanceConfig().isObjectSpaceEnabled();
    }

    /**
     * Changes the value to determine whether the Object Space is enabled or not. The
     * default value is false.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given condition will be used.
     *
     * @since JRuby 1.5.0.
     *
     * This value can be set by jruby.objectspace.enabled system property.
     *
     * @param enable true to enable the Object Space, or false to disable.
     */
    public void setObjectSpaceEnabled(boolean enable) {
        provider.getRubyInstanceConfig().setObjectSpaceEnabled(enable);
    }

    /**
     * Returns a map of environment variables.
     *
     * @since JRuby 1.5.0.
     *
     * @return a map that has environment variables' key-value pairs.
     */
    public Map getEnvironment() {
        return provider.getRubyInstanceConfig().getEnvironment();
    }

    /**
     * Changes an environment variables' map.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param environment a new map of environment variables.
     */
    public void setEnvironment(Map environment) {
        provider.getRubyInstanceConfig().setEnvironment(environment);
    }

    /**
     * Returns a current directory.
     *
     * The default current directory is identical to a value of "user.dir" system
     * property if no security restriction is set. If the "user.dir" directory is
     * protected by the security restriction, the default value is "/".
     *
     * @since JRuby 1.5.0.
     *
     * @return a current directory.
     */
    public String getCurrentDirectory() {
        if (provider.isRuntimeInitialized()) {
            return provider.getRuntime().getCurrentDirectory();
        }
        return provider.getRubyInstanceConfig().getCurrentDirectory();
    }

    /**
     * Changes a current directory to a given directory.
     * The current directory can be changed anytime.
     *
     * @since JRuby 1.5.0.
     *
     * @param directory a new directory to be set.
     */
    public void setCurrentDirectory(String directory) {
        if (provider.isRuntimeInitialized()) {
            provider.getRuntime().setCurrentDirectory(directory);
        } else {
            provider.getRubyInstanceConfig().setCurrentDirectory(directory);
        }
    }

    /**
     * Returns a JRuby home directory.
     *
     * The default JRuby home is the value of JRUBY_HOME environment variable,
     * or "jruby.home" system property when no security restriction is set to
     * those directories. If none of JRUBY_HOME or jruby.home is set and jruby-complete.jar
     * is used, the default JRuby home is "/META-INF/jruby.home" in the jar archive.
     * Otherwise, "java.io.tmpdir" system property is the default value.
     *
     * @since JRuby 1.5.0.
     *
     * @return a JRuby home directory.
     */
    public String getHomeDirectory() {
        return provider.getRubyInstanceConfig().getJRubyHome();
    }

    /**
     * Changes a JRuby home directory to a directory of a given name.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given directory will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param home a name of new JRuby home directory.
     */
    public void setHomeDirectory(String home) {
        provider.getRubyInstanceConfig().setJRubyHome(home);
    }

    /**
     * Returns a class loader object that is currently used. This loader loads
     * Ruby files and libraries.
     *
     * @since JRuby 1.5.0.
     *
     * @return a class loader object that is currently used.
     */
    public ClassLoader getClassLoader() {
        return provider.getRubyInstanceConfig().getLoader();
    }

    /**
     * Changes a class loader to a given loader.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given class loader will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param loader a new class loader to be set.
     */
    public void setClassLoader(ClassLoader loader) {
        provider.getRubyInstanceConfig().setLoader(loader);
    }

    /**
     * Returns a Profile currently used. The default value is Profile.DEFAULT,
     * which has the same behavior to Profile.ALL.
     * Profile allows you to define a restricted subset of code to be loaded during
     * the runtime initialization. When you use JRuby in restricted environment
     * such as Google App Engine, Profile is a helpful option.
     *
     * @since JRuby 1.5.0.
     *
     * @return a current profiler.
     */
    public Profile getProfile() {
        return provider.getRubyInstanceConfig().getProfile();
    }

    /**
     * Changes a Profile to a given one. The default value is Profile.DEFAULT,
     * which has the same behavior to Profile.ALL.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * Profile allows you to define a restricted subset of code to be loaded during
     * the runtime initialization. When you use JRuby in restricted environment
     * such as Google App Engine, Profile is a helpful option. For example,
     * Profile.NO_FILE_CLASS doesn't load File class.
     *
     * @since JRuby 1.5.0.
     *
     * @param profile a new profiler to be set.
     */
    public void setProfile(Profile profile) {
        provider.getRubyInstanceConfig().setProfile(profile);
    }

    /**
     * Returns currently configured ProfileOutput object, which determines where
     * the output of profiling operations will be sent.  (e.g., a file specified
     * by --profile.out).
     *
     * @since JRuby 1.7.15
     *
     * @return current profiling output location.
     */
    public ProfileOutput getProfileOutput() {
        return provider.getRubyInstanceConfig().getProfileOutput();
    }

    /**
     * Changes ProfileOutput to given one. The default value is a ProfileOutput
     * instance that writes to stderr.  Similar to passing `--profile.out` from
     * the command line
     *
     * @since JRuby 1.7.15
     *
     * @param out a new ProfileOutput object, to which profiling data should be written
     */
    public void setProfileOutput(ProfileOutput out) {
        provider.getRubyInstanceConfig().setProfileOutput(out);
    }

    /**
     * Returns a ProfilingMode currently used. The default value is ProfilingMode.OFF.
     *
     * @since JRuby 1.6.6.
     *
     * @return a current profiling mode.
     */
    public ProfilingMode getProfilingMode() {
        return provider.getRubyInstanceConfig().getProfilingMode();
    }

    /**
     * Changes a ProfilingMode to a given one. The default value is Profiling.OFF.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * ProfilingMode allows you to change profiling style.
     *
     * Profiling.OFF - default. profiling off.
     * Profiling.API - activates Ruby profiler API. equivalent to --profile.api command line option
     * Profiling.FLAT - synonym for --profile command line option equivalent to --profile.flat command line option
     * Profiling.GRAPH - runs with instrumented (timed) profiling, graph format. equivalent to --profile.graph command line option.
     *
     * @since JRuby 1.6.6.
     *
     * @deprecated Use setProfilingMode instead
     *
     * @param mode a new profiling mode to be set.
     */
    @Deprecated
    public void setProfile(ProfilingMode mode) {
        provider.getRubyInstanceConfig().setProfilingMode(mode);
    }

    /**
     * Changes a ProfilingMode to a given one. The default value is Profiling.OFF.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * ProfilingMode allows you to change profiling style.
     *
     * Profiling.OFF - default. profiling off.
     * Profiling.API - activates Ruby profiler API. equivalent to --profile.api command line option
     * Profiling.FLAT - synonym for --profile command line option equivalent to --profile.flat command line option
     * Profiling.GRAPH - runs with instrumented (timed) profiling, graph format. equivalent to --profile.graph command line option.
     *
     * @since JRuby 1.7.15
     *
     * @param mode a new profiling mode to be set.
     */
    public void setProfilingMode(ProfilingMode mode) {
        provider.getRubyInstanceConfig().setProfilingMode(mode);
    }

    /**
     * Returns a LoadServiceCreator currently used.
     *
     * @since JRuby 1.5.0.
     *
     * @return a current LoadServiceCreator.
     */
    public LoadServiceCreator getLoadServiceCreator() {
        return provider.getRubyInstanceConfig().getLoadServiceCreator();
    }

    /**
     * Changes a LoadServiceCreator to a given one.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param creator a new LoadServiceCreator
     */
    public void setLoadServiceCreator(LoadServiceCreator creator) {
        provider.getRubyInstanceConfig().setLoadServiceCreator(creator);
    }

    /**
     * Returns a list of argument.
     *
     * @since JRuby 1.5.0.
     *
     * @return an arguments' list.
     */
    public String[] getArgv() {
        return provider.getRubyInstanceConfig().getArgv();
    }

    /**
     * Changes values of the arguments' list.
     *
     * @since JRuby 1.5.0.
     *
     * @param argv a new arguments' list.
     */
    public void setArgv(String[] argv) {
        provider.getRubyInstanceConfig().setArgv(argv);
    }

    /**
     * Returns a script filename to run. The default value is "&lt;script&gt;".
     *
     * @since JRuby 1.5.0.
     *
     * @return a script filename.
     */
    public String getScriptFilename() {
        return provider.getRubyInstanceConfig().getScriptFileName();
    }

    /**
     * Changes a script filename to run. The default value is "&lt;script&gt;".
     * Call this before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param filename a new script filename.
     */
    public void setScriptFilename(String filename) {
        provider.getRubyInstanceConfig().setScriptFileName(filename);
    }

    /**
     * Returns a record separator. The default value is "\n".
     *
     * @since JRuby 1.5.0.
     *
     * @return a record separator.
     */
    public String getRecordSeparator() {
        return provider.getRubyInstanceConfig().getRecordSeparator();
    }

    /**
     * Changes a record separator to a given value. If "0" is given, the record
     * separator goes to "\n\n", "777" goes to "\uFFFF", otherwise, an octal value
     * of the given number.
     * Call this before you use put/get, runScriptlet, and parse methods so that
     * initial configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param separator a new record separator value, "0" or "777"
     */
    public void setRecordSeparator(String separator) {
        provider.getRubyInstanceConfig().setRecordSeparator(separator);
    }

    /**
     * Returns a value of KCode currently used. The default value is KCode.NONE.
     *
     * @since JRuby 1.5.0.
     *
     * @return a KCode value.
     */
    public KCode getKCode() {
        return provider.getRubyInstanceConfig().getKCode();
    }

    /**
     * Changes a value of KCode to a given value. The value should be one of
     * KCode.NONE, KCode.UTF8, KCode.SJIS, or KCode.EUC. The default value is KCode.NONE.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the given value will be used.
     *
     * @since JRuby 1.5.0.
     *
     * @param kcode a new KCode value.
     */
    public void setKCode(KCode kcode) {
        provider.getRubyInstanceConfig().setKCode(kcode);
    }

    /**
     * Set whether native code is enabled for this config. Disabling it also
     * disables C extensions (@see RubyInstanceConfig#setCextEnabled).
     *
     * @see Options#NATIVE_ENABLED
     *
     * @param b new value indicating whether native code is enabled
     */
    public void setNativeEnabled(boolean b) {
        provider.getRubyInstanceConfig().setNativeEnabled(b);
    }

    /**
     * Get whether native code is enabled for this config.
     *
     * @see Options#NATIVE_ENABLED
     *
     * @return true if native code is enabled; false otherwise.
     */
    public boolean isNativeEnabled() {
        return provider.getRubyInstanceConfig().isNativeEnabled();
    }



    /**
     * Returns the value of n, which means that jitted methods are logged in
     * every n methods. The default value is 0.
     *
     * @since JRuby 1.5.0.
     *
     * @return a value that determines how often jitted methods are logged.
     */
    public int getJitLogEvery() {
        return provider.getRubyInstanceConfig().getJitLogEvery();
    }

    /**
     * Changes a value of n, so that jitted methods are logged in every n methods.
     * The default value is 0. This value can be set by the jruby.jit.logEvery System
     * property.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param logEvery a new number of methods.
     */
    public void setJitLogEvery(int logEvery) {
        provider.getRubyInstanceConfig().setJitLogEvery(logEvery);
    }

    /**
     * Returns a value of the threshold that determines whether jitted methods'
     * call reached to the limit or not. The default value is -1 when security
     * restriction is applied, or 50 when no security restriction exists.
     *
     * @since JRuby 1.5.0.
     *
     * @return a value of the threshold.
     */
    public int getJitThreshold() {
        return provider.getRubyInstanceConfig().getJitThreshold();
    }

    /**
     * Changes a value of the threshold that determines whether jitted methods'
     * call reached to the limit or not. The default value is -1 when security
     * restriction is applied, or 50 when no security restriction exists. This
     * value can be set by jruby.jit.threshold System property.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param threshold a new value of the threshold.
     */
    public void setJitThreshold(int threshold) {
        provider.getRubyInstanceConfig().setJitThreshold(threshold);
    }

    /**
     * Returns a value of a max class cache size. The default value is 0 when
     * security restriction is applied, or 4096 when no security restriction exists.
     *
     * @since JRuby 1.5.0.
     *
     * @return a value of a max class cache size.
     */
    public int getJitMax() {
        return provider.getRubyInstanceConfig().getJitMax();
    }

    /**
     * Changes a value of a max class cache size. The default value is 0 when
     * security restriction is applied, or 4096 when no security restriction exists.
     * This value can be set by jruby.jit.max System property.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param max a new value of a max class cache size.
     */
    public void setJitMax(int max) {
        provider.getRubyInstanceConfig().setJitMax(max);
    }

    /**
     * Returns a value of a max size of the bytecode generated by compiler. The
     * default value is -1 when security restriction is applied, or 10000 when
     * no security restriction exists.
     *
     * @since JRuby 1.5.0.
     *
     * @return a value of a max size of the bytecode.
     */
    public int getJitMaxSize() {
        return provider.getRubyInstanceConfig().getJitMaxSize();
    }

    /**
     * Changes a value of a max size of the bytecode generated by compiler. The
     * default value is -1 when security restriction is applied, or 10000 when
     * no security restriction exists. This value can be set by jruby.jit.maxsize
     * System property.
     * Call this method before you use put/get, runScriptlet, and parse methods so that
     * the configurations will work.
     *
     * @since JRuby 1.5.0.
     *
     * @param maxSize a new value of a max size of the bytecode.
     */
    public void setJitMaxSize(int maxSize) {
        provider.getRubyInstanceConfig().setJitMaxSize(maxSize);
    }

    /**
     * Returns version information about JRuby and Ruby supported by this platform.
     *
     * @return version information.
     */
    public String getSupportedRubyVersion() {
        return OutputStrings.getVersionString().trim();
    }

    /**
     * Returns an array of values associated to a key.
     *
     * @param key is a key in a property file
     * @return values associated to the key
     */
    public String[] getProperty(String key) {
        if (basicProperties.containsKey(key)) {
            return basicProperties.get(key);
        }
        return null;
    }

    /**
     * Returns a provider instance of {@link LocalContextProvider}. When users
     * want to configure Ruby runtime, they can do by setting class loading paths,
     * {@link org.jruby.RubyInstanceConfig} to the provider before they get Ruby
     * runtime.
     *
     * @return a provider of {@link LocalContextProvider}
     */
    public LocalContextProvider getProvider() {
        return provider;
    }

    /**
     * Returns a Ruby runtime in one of {@link LocalContextScope}.
     *
     * @deprecated As of JRuby 1.5.0. Use getProvider().getRuntime() method instead.
     *
     * @return Ruby runtime of a specified local context
     */
    @Deprecated
    public Ruby getRuntime() {
        return provider.getRuntime();
    }

    /**
     * Returns a variable map in one of {@link LocalContextScope}. Variables
     * in this map is used to share between Java and Ruby. Map keys are Ruby's
     * variable names, thus they must be valid Ruby names.
     *
     * @return a variable map specific to the current thread
     */
    public BiVariableMap getVarMap() {
        return provider.getVarMap();
    }

    /**
     * Returns a attribute map in one of {@link LocalContextScope}. Attributes
     * in this map accept any key value pair, types of which are java.lang.Object.
     * Ruby scripts do not look up this map.
     *
     * @return an attribute map specific to the current thread
     */
    public Map getAttributeMap() {
        return provider.getAttributeMap();
    }

    /**
     * Returns an attribute value associated with the specified key in
     * a attribute map. This is a short cut method of
     * ScriptingContainer#getAttributeMap().get(key).
     *
     * @param key is the attribute key
     * @return value is a value associated to the specified key
     */
    public Object getAttribute(Object key) {
        return provider.getAttributeMap().get(key);
    }

    /**
     * Associates the specified value with the specified key in a
     * attribute map. If the map previously contained a mapping for the key,
     * the old value is replaced. This is a short cut method of
     * ScriptingContainer#getAttributeMap().put(key, value).
     *
     * @param key is a key that the specified value is to be associated with
     * @param value is a value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    public Object setAttribute(Object key, Object value) {
        return provider.getAttributeMap().put(key, value);
    }

    /**
     * Removes the specified value with the specified key in a
     * attribute map. If the map previously contained a mapping for the key,
     * the old value is returned. This is a short cut method of
     * ScriptingContainer#getAttributeMap().remove(key).
     *
     * @param key is a key that the specified value is to be removed from
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    public Object removeAttribute(Object key) {
        return provider.getAttributeMap().remove(key);
    }

    /**
     * Returns a value of the specified key in a top level of runtime or null
     * if this map doesn't have a mapping for the key. The key
     * must be a valid Ruby variable or constant name.
     *
     * @param key is a key whose associated value is to be returned
     * @return a value to which the specified key is mapped, or null if this
     *         map contains no mapping for the key
     */
    public Object get(String key) {
        return provider.getVarMap().get(key);
    }

    /**
     * Returns a value of a specified key in a specified receiver or null if
     * a variable map doesn't have a mapping for the key in a given
     * receiver. The key must be a valid Ruby variable or constant name. A global
     * variable doesn't depend on the receiver.
     *
     * @param receiver a receiver to get the value from
     * @param key is a key whose associated value is to be returned
     * @return a value to which the specified key is mapped, or null if this
     *         map contains no mapping for the key
     */
    public Object get(Object receiver, String key) {
        return provider.getVarMap().get(receiver, key);
    }

    /**
     * Associates the specified value with the specified key in a
     * variable map. This key-value pair is injected to a top level of runtime
     * during evaluation. If the map previously contained a mapping for the key,
     * the old value is replaced. The key must be a valid Ruby variable or
     * constant name. It will be a top level variable or constant.
     *
     * @param key is a key that the specified value is to be associated with
     * @param value is a value to be associated with the specified key
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object put(String key, Object value) {
        return provider.getVarMap().put(key, value);
    }

    /**
     * Associates the specified value with the specified key in a variable map.
     * This key-value pair is injected to a given receiver during evaluation.
     * If the map previously contained a mapping for the key,
     * the old value is replaced. The key must be a valid Ruby variable or
     * constant name. A given receiver limits the scope of a variable or constant.
     * However, a global variable is accessible globally always.
     *
     * @param receiver a receiver to put the value in
     * @param key is a key that the specified value is to be associated with
     * @param value is a value to be associated with the specified key
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object put(Object receiver, String key, Object value) {
        return provider.getVarMap().put(receiver, key, value);
    }

    /**
     * Removes the specified Ruby variable with the specified variable name from a
     * variable map and runtime top level. If the map previously contained a
     * mapping for the key, the old value is returned. The key must be a valid
     * Ruby variable name.
     *
     * @param key is a key that the specified value is to be associated with
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object remove(String key) {
        return provider.getVarMap().remove(key);
    }

    /**
     * Removes the specified Ruby variable with the specified variable name in a
     * variable map and given receiver. If the map previously contained a mapping for the key,
     * the old value is returned. The key must be a valid Ruby variable name.
     * This is a short cut method of ScriptingContainer#getVarMap().remove(key).
     *
     * @param receiver a receiver to remove the value from
     * @param key is a key that the specified value is to be associated with
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object remove(Object receiver, String key) {
        return provider.getVarMap().removeFrom(receiver, key);
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns. Ruby variables are also
     * removed from Ruby instance. However, Ruby instance keep having global variable
     * names with null value.
     * This is a short cut method of ScriptingContainer#getVarMap().clear().
     */
    public void clear() {
        provider.getVarMap().clear();
    }

    /**
     * Parses a script and return an object which can be run(). This allows
     * the script to be parsed once and evaluated many times.
     *
     * @param script is a Ruby script to be parsed
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    public EmbedEvalUnit parse(String script, int... lines) {
        return runtimeAdapter.parse(script, lines);
    }

    /**
     * Parses a script given by a reader and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param reader is used to read a script from
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    public EmbedEvalUnit parse(Reader reader, String filename, int... lines) {
        return runtimeAdapter.parse(reader, filename, lines);
    }

    /**
     * Parses a script read from a specified path and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param type is one of the types {@link PathType} defines
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    public EmbedEvalUnit parse(PathType type, String filename, int... lines) {
        return runtimeAdapter.parse(type, filename, lines);
    }

    /**
     * Parses a script given by a input stream and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     *
     * @param istream is an input stream to get a script from
     * @param filename filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    public EmbedEvalUnit parse(InputStream istream, String filename, int... lines) {
        return runtimeAdapter.parse(istream, filename, lines);
    }

    /**
     * Evaluates a script under the current scope (perhaps the top-level
     * scope) and returns a result only if a script returns a value.
     * Right after the parsing, the script is evaluated once.
     *
     * @param script is a Ruby script to get run
     * @return an evaluated result converted to a Java object
     */
    public Object runScriptlet(String script) {
        EmbedEvalUnit unit = parse(script);
        return runUnit(unit);
    }

    private Object runUnit(EmbedEvalUnit unit) {
        if (unit == null) {
            return null;
        }
        IRubyObject ret = unit.run();
        return JavaEmbedUtils.rubyToJava(ret);
    }

    /**
     * Evaluates a script read from a reader under the current scope
     * (perhaps the top-level scope) and returns a result only if a script
     * returns a value. Right after the parsing, the script is evaluated once.
     *
     * @param reader is used to read a script from
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @return an evaluated result converted to a Java object
     */
    public Object runScriptlet(Reader reader, String filename) {
        EmbedEvalUnit unit = parse(reader, filename);
        return runUnit(unit);
    }

    /**
     * Evaluates a script read from a input stream under the current scope
     * (perhaps the top-level scope) and returns a result only if a script
     * returns a value. Right after the parsing, the script is evaluated once.
     *
     * @param istream is used to input a script from
     * @param filename is used as in information, for example, appears in a stack trace
     *        of an exception
     * @return an evaluated result converted to a Java object
     */
    public Object runScriptlet(InputStream istream, String filename) {
        EmbedEvalUnit unit = parse(istream, filename);
        return runUnit(unit);
    }

    /**
     * Reads a script file from specified path and evaluates it under the current
     * scope (perhaps the top-level scope) and returns a result only if a script
     * returns a value. Right after the parsing, the script is evaluated once.
     *
     * @param type is one of the types {@link PathType} defines
     * @param filename is used to read the script from and an information
     * @return an evaluated result converted to a Java object
     */
    public Object runScriptlet(PathType type, String filename) {
        EmbedEvalUnit unit = parse(type, filename);
        return runUnit(unit);
    }

    /**
     * Returns an instance of {@link EmbedRubyRuntimeAdapter} for embedders to parse
     * scripts.
     *
     * @return an instance of {@link EmbedRubyRuntimeAdapter}.
     */
    public EmbedRubyRuntimeAdapter newRuntimeAdapter() {
        return runtimeAdapter;
    }

    /**
     * Returns an instance of {@link EmbedRubyObjectAdapter} for embedders to invoke
     * methods defined by Ruby. The script must be evaluated prior to a method call.
     * In most cases, users don't need to use this method. ScriptingContainer's
     * callMethods are the shortcut and work in the same way.
     *
     * <pre>Example
     *         # calendar.rb
     *         require 'date'
     *         class Calendar
     *           def initialize;@today = DateTime.now;end
     *           def next_year;@today.year + 1;end
     *         end
     *         Calendar.new
     *
     *
     *         ScriptingContainer container = new ScriptingContainer();
     *         String filename =  "ruby/calendar.rb";
     *         Object receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
     *         EmbedRubyObjectAdapter adapter = instance.newObjectAdapter();
     *         Integer result =
     *             (Integer) adapter.callMethod(receiver, "next_year", Integer.class);
     *         System.out.println("next year: " + result);
     *         System.out.println(instance.get("@today"));
     *
     * Outputs:
     *     next year: 2010
     *     2009-05-19T17:46:44-04:00</pre>
     *
     * @return an instance of {@link EmbedRubyObjectAdapter}
     */
    public EmbedRubyObjectAdapter newObjectAdapter() {
        return objectAdapter;
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments
     * @return an instance of requested Java type
     */
    public Object callMethod(Object receiver, String methodName, Object... args) {
        return objectAdapter.callMethod(receiver, methodName, args);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     *
     * @param receiver is an instance that will receive this method call
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param block is a block to be executed in this method
     * @param args is an array of method arguments
     * @return an instance of requested Java type
     */
    public Object callMethod(Object receiver, String methodName, Block block, Object... args) {
        return objectAdapter.callMethod(receiver, methodName, block, args);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType) {
        return objectAdapter.callMethod(receiver, methodName, returnType);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have only one argument.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param singleArg is an method argument
     * @param returnType returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Object singleArg, Class<T> returnType) {
        return objectAdapter.callMethod(receiver, methodName, singleArg, returnType);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType) {
        return objectAdapter.callMethod(receiver, methodName, args, returnType);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, one of which is a block.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType) {
        return objectAdapter.callMethod(receiver, methodName, args, block, returnType);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument, and users want to inject Ruby's local
     * variables' values from Java.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType, EmbedEvalUnit unit) {
        return objectAdapter.callMethod(receiver, methodName, returnType, unit);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, and users want to inject Ruby's local
     * variables' values from Java.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return an instance of requested Java type
     */
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType, EmbedEvalUnit unit) {
        return objectAdapter.callMethod(receiver, methodName, args, returnType, unit);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, one of which is a block, and users want to
     * inject Ruby's local variables' values from Java.
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return is the type we want it to convert to
     */
    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType, EmbedEvalUnit unit) {
        return objectAdapter.callMethod(receiver, methodName, args, block, returnType, unit);
    }

    /**
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    public <T> T callSuper(Object receiver, Object[] args, Class<T> returnType) {
        return objectAdapter.callSuper(receiver, args, returnType);
    }

    /**
     *
     * @param receiver is an instance that will receive this method call.
     *                 Ruby's self object will be used if no appropriate receiver
     *                 is given.
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    public <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType) {
        return objectAdapter.callSuper(receiver, args, block, returnType);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     *
     * @param returnType is the type we want it to convert to
     * @param receiver is an instance that will receive this method call. The receiver
     *                 can be null or other Java objects as well as RubyObject.
     *                 The null will be converted to RubyNil. Java objects will be
     *                 wrapped in RubyObject.
     * @param methodName is a method name to be called
     * @param args is an array of method arguments
     * @return an instance of requested Java type
     */
    public <T> T runRubyMethod(Class<T> returnType, Object receiver, String methodName, Object... args) {
        return objectAdapter.runRubyMethod(returnType, receiver, methodName, null, args);
    }

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     *
     * @param returnType is the type we want it to convert to
     * @param receiver is an instance that will receive this method call. The receiver
     *                 can be null or other Java objects as well as RubyObject.
     *                 The null will be converted to RubyNil. Java objects will be
     *                 wrapped in RubyObject.
     * @param methodName is a method name to be called
     * @param block is an optional Block object.  Send null for no block.
     * @param args is an array of method arguments
     * @return an instance of requested Java type
     */
    public <T> T runRubyMethod(Class<T> returnType, Object receiver, String methodName, Block block, Object... args) {
        return objectAdapter.runRubyMethod(returnType, receiver, methodName, block, args);
    }

    /**
     * Returns an instance of a requested interface type. An implementation of
     * the requested interface is done by a Ruby script, which has been evaluated
     * before getting the instance.
     * In most cases, users don't need to use this method. ScriptingContainer's
     * runScriptlet method returns an instance of the interface type that is
     * implemented by Ruby.
     *
     * <pre>Example
     * Interface
     *     //QuadraticFormula.java
     *     package org.jruby.embed;
     *     import java.util.List;
     *     public interface QuadraticFormula {
     *         List solve(int a, int b, int c) throws Exception;
     *     }
     *
     * Implementation
     *     #quadratic_formula.rb
     *     def solve(a, b, c)
     *       v = b ** 2 - 4 * a * c
     *       if v < 0: raise RangeError end
     *       s0 = ((-1)*b - Math.sqrt(v))/(2*a)
     *       s1 = ((-1)*b + Math.sqrt(v))/(2*a)
     *       return s0, s1
     *     end
     *
     * Usage
     *     ScriptingContainer container = new ScriptingContainer();
     *     String filename = "ruby/quadratic_formula_class.rb";
     *     Object receiver = container.runScriptlet(PathType.CLASSPATH, filename);
     *     QuadraticFormula qf = container.getInstance(receiver, QuadraticFormula.class);
     *     try {
     *          List<Double> solutions = qf.solve(1, -2, -13);
     *          printSolutions(solutions);
     *          solutions = qf.solve(1, -2, 13);
     *          for (double s : solutions) {
     *              System.out.print(s + ", ");
     *          }
     *     } catch (Exception e) {
     *          e.printStackTrace();
     *     }
     *
     * Output
     *     -2.7416573867739413, 4.741657386773941,
     * </pre>
     *
     *
     * @param receiver is an instance that implements the interface
     * @param clazz is a requested interface
     * @return an instance of a requested interface type
     */
    public <T> T getInstance(Object receiver, Class<T> clazz) {
        return interfaceAdapter.getInstance(receiver, clazz);
    }

    /**
     * Replaces a standard input by a specified reader
     *
     * @param reader is a reader to be set
     */
    public void setReader(Reader reader) {
        if (reader == null) {
            return;
        }
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.READER)) {
            Reader old = (Reader) map.get(AttributeName.READER);
            if (old == reader) {
                return;
            }
        }
        map.put(AttributeName.READER, reader);
        InputStream istream = new ReaderInputStream(reader);
        Ruby runtime = provider.getRuntime();
        RubyIO io = new RubyIO(runtime, istream);
        io.getOpenFile().setSync(true);
        runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", io), GlobalVariable.Scope.GLOBAL);
        runtime.getObject().storeConstant("STDIN", io);
    }

    /**
     * Returns a reader set in an attribute map.
     *
     * @return a reader in an attribute map
     */
    public Reader getReader() {
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.READER)) {
            return (Reader) getAttributeMap().get(AttributeName.READER);
        }
        return null;
    }

    /**
     * Returns an input stream that Ruby runtime has. The stream is set when
     * Ruby runtime is initialized.
     *
     * @deprecated As of JRuby 1.5.0, replaced by getInput().
     *
     * @return an input stream that Ruby runtime has.
     */
    @Deprecated
    public InputStream getIn() {
        return getInput();
    }

    /**
     * Replaces a standard output by a specified writer.
     *
     * @param writer is a writer to be set
     */
    public void setWriter(Writer writer) {
        if (writer == null) {
            return;
        }
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.WRITER)) {
            Writer old = (Writer) map.get(AttributeName.WRITER);
            if (old == writer) {
                return;
            }
        }
        map.put(AttributeName.WRITER, writer);
        PrintStream pstream = new PrintStream(new WriterOutputStream(writer));
        setOutputStream(pstream);
    }

    private void setOutputStream(PrintStream pstream) {
        if (pstream == null) {
            return;
        }
        Ruby runtime = provider.getRuntime();
        RubyIO io = new RubyIO(runtime, pstream);
        io.getOpenFile().setSync(true);
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", io), GlobalVariable.Scope.GLOBAL);
        runtime.getObject().storeConstant("STDOUT", io);
        runtime.getGlobalVariables().alias("$>", "$stdout");
        runtime.getGlobalVariables().alias("$defout", "$stdout");
    }

    public void resetWriter() {
        PrintStream pstream = provider.getRubyInstanceConfig().getOutput();
        setOutputStream(pstream);
    }

    /**
     * Returns a writer set in an attribute map.
     *
     * @return a writer in a attribute map
     */
    public Writer getWriter() {
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.WRITER)) {
            return (Writer) getAttributeMap().get(AttributeName.WRITER);
        }
        return null;
    }

    /**
     * Returns an output stream that Ruby runtime has. The stream is set when
     * Ruby runtime is initialized.
     *
     * @deprecated As of JRuby 1.5.0, replaced by getOutput().
     *
     * @return an output stream that Ruby runtime has
     */
    @Deprecated
    public PrintStream getOut() {
        return getOutput();
    }

    /**
     * Replaces a standard error by a specified writer.
     *
     * @param errorWriter is a writer to be set
     */
    public void setErrorWriter(Writer errorWriter) {
        if (errorWriter == null) {
            return;
        }
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.ERROR_WRITER)) {
            Writer old = (Writer) map.get(AttributeName.ERROR_WRITER);
            if (old == errorWriter) {
                return;
            }
        }
        map.put(AttributeName.ERROR_WRITER, errorWriter);
        PrintStream pstream = new PrintStream(new WriterOutputStream(errorWriter));
        setErrorStream(pstream);
    }

    private void setErrorStream(PrintStream error) {
        if (error == null) {
            return;
        }
        Ruby runtime = provider.getRuntime();
        RubyIO io = new RubyIO(runtime, error);
        io.getOpenFile().setSync(true);
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", io), GlobalVariable.Scope.GLOBAL);
        runtime.getObject().storeConstant("STDERR", io);
        runtime.getGlobalVariables().alias("$deferr", "$stderr");
    }

    public void resetErrorWriter() {
        PrintStream error = provider.getRubyInstanceConfig().getError();
        setErrorStream(error);
    }

    /**
     * Returns an error writer set in an attribute map.
     *
     * @return an error writer in a attribute map
     */
    public Writer getErrorWriter() {
        Map map = getAttributeMap();
        if (map.containsKey(AttributeName.ERROR_WRITER)) {
            return (Writer) getAttributeMap().get(AttributeName.ERROR_WRITER);
        }
        return null;
    }

    /**
     * Returns an error output stream that Ruby runtime has. The stream is set when
     * Ruby runtime is initialized.
     *
     * @deprecated As of JRuby 1.5.0, Replaced by getError()
     *
     * @return an error output stream that Ruby runtime has
     */
    @Deprecated
    public PrintStream getErr() {
        return getError();
    }

    /**
     * Cleanly shut down this ScriptingContainer and any JRuby resources it holds.
     * All ScriptingContainer instances should be terminated when you are done with
     * them, rather then leaving them for GC to finalize.
     *
     * @since JRuby 1.5.0
     */
    public void terminate() {
        if (getProvider().isRuntimeInitialized()) getProvider().getRuntime().tearDown(false);
        getProvider().terminate();
    }

    /**
     * Ensure this ScriptingContainer instance is terminated when nobody holds any
     * references to it (and GC wants to reclaim it).
     *
     * Note that {@link org.jruby.embed.LocalContextScope::SINGLETON} containers will not terminate on GC.
     *
     * @throws Throwable
     *
     * @since JRuby 1.6.0
     */
    public void finalize() throws Throwable {
        super.finalize();
        // singleton containers share global runtime, and should not tear it down
        if (scope != LocalContextScope.SINGLETON) terminate();
    }

    /**
     * Force dynamically-loaded Java classes to load first from the classloader provided by
     * JRuby before searching parent classloaders. This can be used to isolated dependency
     * in different runtimes from each other and from parent classloaders. The default
     * behavior is to defer to parent classloaders if they can provide the requested
     * classes.
     *
     * Note that if different versions of a  library are loaded by both a parent
     * classloader and the JRuby classloader, those classess would be incompatible
     * with each other and with other library objects from the opposing classloader.
     *
     * @param classloaderDelegate set whether prefer the JRuby classloader to delegate first
     *                            to the parent classloader when dynamically loading classes
     * @since JRuby 9.0.0.0
     */
    public void setClassloaderDelegate(boolean classloaderDelegate) {
        getProvider().getRubyInstanceConfig().setClassloaderDelegate(classloaderDelegate);
    }

    /**
     * Retrieve the self-first classloader setting.
     *
     * @see ScriptingContainer#setClassloaderDelegate(boolean)
     */
    public boolean getClassloaderDelegate() {
        return getProvider().getRubyInstanceConfig().isClassloaderDelegate();
    }
}
