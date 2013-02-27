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
 * Copyright (C) 2010 Yoko Harada <yokolet@gmail.com>
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

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import org.jruby.CompatVersion;
import org.jruby.Profile;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.RubyInstanceConfig.LoadServiceCreator;
import org.jruby.util.ClassCache;
import org.jruby.util.KCode;

/**
 * This interface defines methods to configure Ruby runtime for embedding.
 *
 * @since JRuby 1.5.0
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public interface EmbedRubyInstanceConfigAdapter {
    /**
     * Returns a list of load paths for Ruby scripts/libraries. If no paths is
     * given, the list is created from java.class.path System property.
     *
     * @return a list of load paths.
     */
    public List<String> getLoadPaths();

    /**
     * Changes a list of load paths Ruby scripts/libraries. The default value
     * is an empty array. If no paths is given, the list is created from
     * java.class.path System property. This value can be set by
     * org.jruby.embed.class.path System property.
     *
     * @param paths a new list of load paths.
     */
    public void setLoadPaths(List<String> paths);

    /**
     * Returns an input stream assigned to STDIN and $stdin.
     * 
     * @return input stream of STDIN and $stdin
     */
    public InputStream getInput();

    /**
     * Changes STDIN and $stdin to a given input stream. The default standard input
     * is java.lang.System.in.
     * 
     * @param istream an input stream to be set
     */
    public void setInput(InputStream istream);

    /**
     * Changes STDIN and $stdin to a given reader. No reader is set by default.
     * 
     * @param reader a reader to be set
     */
    public void setInput(Reader reader);

    /**
     * Returns an output stream assigned to STDOUT and $stdout
     *
     * @return an output stream of STDOUT and $stdout
     */
    public PrintStream getOutput();

    /**
     * Changes STDOUT and $stdout to a given output stream. The default standard
     * output is java.lang.System.out.
     * 
     * @param pstream an output stream to be set
     */
    public void setOutput(PrintStream pstream);

    /**
     * Changes STDOUT and $stdout to a given writer. No writer is set by default.
     * 
     * @param writer a writer to be set
     */
    public void setOutput(Writer writer);

    /**
     * Returns an error stream assigned to STDERR and $stderr.
     * 
     * @return output stream for error stream
     */
    public PrintStream getError();

    /**
     * Changes STDERR and $stderr to a gievn print stream. The default standard error
     * is java.lang.System.err.
     * 
     * @param pstream a print stream to be set
     */
    public void setError(PrintStream pstream);

    /**
     * Changes STDERR and $stderr to a given writer. No writer is set by default.
     * 
     * @param writer a writer to be set
     */
    public void setError(Writer writer);

    /**
     * Returns a compile mode currently chosen, which is one of CompileMode.JIT,
     * CompileMode.FORCE, CompileMode.OFF. The default mode is CompileMode.OFF.
     *
     * @return a compile mode.
     */
    public CompileMode getCompileMode();

    /**
     * Changes a compile mode to a given mode, which should be one of CompileMode.JIT,
     * CompileMode.FORCE, CompileMode.OFF. The default value is CompileMode.OFF.
     * 
     * @param mode compile mode
     */
    public void setCompileMode(CompileMode mode);

    /**
     * Tests whether Ruby runs in a process or not.
     *
     * @return true if Ruby is configured to run in a process, otherwise, false.
     */
    public boolean isRunRubyInProcess();

    /**
     * Changes the value to determine whether Ruby runs in a process or not. The
     * default value is true.
     * 
     * @param inprocess true when Ruby is set to run in the process, or false not to
     * run in the process.
     */
    public void setRunRubyInProcess(boolean inprocess);

    /**
     * Returns a Ruby version currently chosen, which is one of CompatVersion.RUBY1_8,
     * CompatVersion.RUBY1_9, or CompatVersion.BOTH. The default version is
     * CompatVersion.RUBY1_8.
     * 
     * @return a Ruby version
     */
    public CompatVersion getCompatVersion();

    /**
     * Changes a Ruby version to be evaluated into one of CompatVersion.RUBY1_8,
     * CompatVersion.RUBY1_9, or CompatVersion.BOTH. The default version is
     * CompatVersion.RUBY1_8.
     * 
     * @param version a Ruby version
     */
    public void setCompatVersion(CompatVersion version);

    /**
     * Tests whether the Object Space is enabled or not.
     * 
     * @return true if the Object Space is able to use, otherwise, false.
     */
    public boolean isObjectSpaceEnabled();

    /**
     * Changes the value to determine whether the Object Space is enabled or not. The
     * default value is false.
     *
     * This value can be set by jruby.objectspace.enabled system property.
     * 
     * @param enable true to enable the Object Space, or false to disable.
     */
    public void setObjectSpaceEnabled(boolean enable);

    /**
     * Returns a map of environment variables.
     *
     * @return a map that has environment variables' key-value pairs.
     */
    public Map getEnvironment();

    /**
     * Changes an environment variables' map.
     *
     * @param environment a new map of environment variables.
     */
    public void setEnvironment(Map environment);

    /**
     * Returns a current directory.
     *
     * The default current directory is identical to a value of "user.dir" system 
     * property if no security restriction is set. If the "user.dir" directory is
     * protected by the security restriction, the default value is "/".
     *
     * @return a current directory.
     */
    public String getCurrentDirectory();

    /**
     * Changes a current directory to a given directory.
     *
     * @param directory a new directory to be set.
     */
    public void setCurrentDirectory(String directory);

    /**
     * Returns a JRuby home directory.
     *
     * The default JRuby home is the value of JRUBY_HOME environmet variable,
     * or "jruby.home" system property when no security restriction is set to
     * those directories. If none of JRUBY_HOME or jruby.home is set and jruby-complete.jar
     * is used, the default JRuby home is "/META-INF/jruby.home" in the jar archive.
     * Otherwise, "java.io.tmpdir" system property is the default value.
     *
     * @return a JRuby home directory.
     */
    public String getHomeDirectory();

    /**
     * Changes a JRuby home directroy to a directory of a given name.
     *
     * @param home a name of new JRuby home directory.
     */
    public void setHomeDirectory(String home);

    /**
     * Returns a ClassCache object that is tied to a class loader. The default ClassCache
     * object is tied to a current thread' context loader if it exists. Otherwise, it is
     * tied to the class loader that loaded RubyInstanceConfig.
     *
     * @return a ClassCache object.
     */
    public ClassCache getClassCache();

    /**
     * Changes a ClassCache object to a given one.
     * 
     * @param cache a new ClassCache object to be set.
     */
    public void setClassCache(ClassCache cache);

    /**
     * Returns a class loader object that is currently used. This loader loads
     * Ruby files and libraries.
     * 
     * @return a class loader object that is currently used.
     */
    public ClassLoader getClassLoader();

    /**
     * Changes a class loader to a given loader.
     * 
     * @param loader a new class loader to be set.
     */
    public void setClassLoader(ClassLoader loader);

    /**
     * Returns a profiler currently used. The default profiler is Profile.DEFAULT,
     * which has the same behavior to Profile.ALL.
     *
     * @return a current profiler.
     */
    public Profile getProfile();

    /**
     * Changes a profiler to a given one.
     * 
     * @param profile a new profiler to be set.
     */
    public void setProfile(Profile profile);

    /**
     * Returns a LoadServiceCreator currently used.
     *
     * @return a current LoadServiceCreator.
     */
    public LoadServiceCreator getLoadServiceCreator();

    /**
     * Changes a LoadServiceCreator to a given one.
     *
     * @param creator a new LoadServiceCreator
     */
    public void setLoadServiceCreator(LoadServiceCreator creator);

    /**
     * Returns an arguments' list.
     *
     * @return an arguments' list.
     */
    public String[] getArgv();

    /**
     * Changes values of the arguments' list.
     *
     * @param argv a new arguments' list.
     */
    public void setArgv(String[] argv);

    /**
     * Returns a script filename to run. The default value is null.
     *
     * @return a script filename.
     */
    public String getScriptFilename();

    /**
     * Changes a script filename to run. The default value is null.
     *
     * @param filename a new script filename.
     */
    public void setScriptFilename(String filename);

    /**
     * Tests whether JRuby/Ruby version will be shown or not. The default value is
     * false.
     *
     * @return true if the version will be shown, false otherwise.
     */
    //Embedding API has a method to show version. Users should call the method
    //when they want to show version.
    //public boolean isShowVersion();

    /**
     * Changes a value whether JRuby/Ruby version will be shown or not. The default
     * value is false.
     *
     * @param show a new value to determine whether JRuby/Ruby version will be shown or not.
     */
    //public void setShowVersion(boolean show);
    
    /**
     * Tests whether generated Java bytecodes will be dumped out to System.out or not.
     * The default value is false.
     * 
     * @return true if generated Java bytecodes are shown, false otherwise.
     */
    //public boolean isShowBytecode();

    /**
     * Changes a value to show generated Java bytecodes or not. The default value is false.
     *
     * @param show true if generated Java bytecodes will be shown, false otherwise.
     */
    //public void setShowBytecode(boolean show);

    /**
     * Returns a record separator. The default value is \u0000.
     *
     * @return a record separator.
     */
    public String getRecordSeparator();

    /**
     * Changes a record separator to a given value. If "0" is given, the record
     * separator goes to "\n\n", "777" goes to "\uFFFF", otherwise, an octal value
     * of the given number.
     *
     * @param separator a new record separator value, "0" or "777"
     */
    public void setRecordSeparator(String separator);

    /**
     * Returns a value of KCode currently used. The default value is KCode.NONE.
     *
     * @return a KCode value.
     */
    public KCode getKCode();

    /**
     * Changes a value of KCode to a given value. The default value is KCode.NONE.
     *
     * @param kcode a new KCode value.
     */
    public void setKCode(KCode kcode);

    /**
     * Returns the value of n, which means that jitted methods are logged in
     * every n methods. The default value is 0.
     *
     * @return a value that determines how often jitted methods are logged.
     */
    public int getJitLogEvery();

    /**
     * Changes a value of n, so that jitted methods are logged in every n methods.
     * The default value is 0. This value can be set by the jruby.jit.logEvery System
     * property.
     * 
     * @param logEvery a new number of methods.
     */
    public void setJitLogEvery(int logEvery);

    /**
     * Returns a value of the threshold that determines whether jitted methods'
     * call reached to the limit or not. The default value is -1 when security
     * restriction is applied, or 50 when no security restriction exists.
     *
     * @return a value of the threshold.
     */
    public int getJitThreshold();

    /**
     * Changes a value of the threshold that determines whether jitted methods'
     * call reached to the limit or not. The default value is -1 when security
     * restriction is applied, or 50 when no security restriction exists. This
     * value can be set by jruby.jit.threshold System property.
     * 
     * @param threshold a new value of the threshold.
     */
    public void setJitThreshold(int threshold);

    /**
     * Returns a value of a max class cache size. The default value is 0 when
     * security restriction is applied, or 4096 when no security restriction exists.
     * 
     * @return a value of a max class cache size.
     */
    public int getJitMax();

    /**
     * Changes a value of a max class cache size. The default value is 0 when
     * security restriction is applied, or 4096 when no security restriction exists.
     * This value can be set by jruby.jit.max System property.
     * 
     * @param max a new value of a max class cache size.
     */
    public void setJitMax(int max);

    /**
     * Returns a value of a max size of the bytecode generated by compiler. The
     * default value is -1 when security restriction is applied, or 10000 when
     * no security restriction exists.
     *
     * @return a value of a max size of the bytecode.
     */
    public int getJitMaxSize();

    /**
     * Changes a value of a max size of the bytecode generated by compiler. The
     * default value is -1 when security restriction is applied, or 10000 when
     * no security restriction exists. This value can be set by jruby.jit.maxsize
     * System property.
     *
     * @param maxSize a new value of a max size of the bytecode.
     */
    public void setJitMaxSize(int maxSize);

    /**
     * Returns version information about JRuby and Ruby supported by this platform.
     *
     * @return version information.
     */
    public String getSupportedRubyVersion();
}
