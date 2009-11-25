/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import org.jruby.Ruby;
import org.jruby.RubyGlobal.InputGlobalVariable;
import org.jruby.RubyGlobal.OutputGlobalVariable;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.internal.EmbedRubyInterfaceAdapterImpl;
import org.jruby.embed.internal.EmbedRubyObjectAdapterImpl;
import org.jruby.embed.internal.EmbedRubyRuntimeAdapterImpl;
import org.jruby.embed.internal.SingleThreadLocalContextProvider;
import org.jruby.embed.internal.SingletonLocalContextProvider;
import org.jruby.embed.internal.ThreadSafeLocalContextProvider;
import org.jruby.embed.io.ReaderInputStream;
import org.jruby.embed.io.WriterOutputStream;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * ScipritngContainer provides various methods and resources that are useful
 * for embedding Ruby in Java. Using this class, users can run Ruby scripts from
 * Java programs easily. Also, users can use methods defined or implemented by Ruby.
 *
 * ScriptingContainer has a couple of configuration parameters per container base.
 * Those are a local context scope, local variable behavior, and property file.
 * These parameters should be given when the container is instantiated; otherwise,
 * default values are applied to.
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
 * In this example, a local variable "x" is shared. Unlike JSR223 JRuby engine,
 * Ruby's local, instance, global variables and constants are available to share on
 * ScrriptingContainer. (A class variable sharing does not work on current version)
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
 * The third examples shows how to keep local variables across mutilple evalucations.
 * This feature simulates BSF engine for JRuby. In terms of Ruby semantics,
 * local variables should not survive after the evaluation has completed. Thus,
 * this behavior is optional, and users need to specify LocalVariableBehvior.PERSISTENT
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
 * invoking methods defined by Ruby, and getting an instance of a specifed interface
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
 * {@see <a href="http://kenai.com/projects/jruby-embed/pages/Home">Wiki</a>}
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class ScriptingContainer {
    private static final String defaultProps = "org/jruby/embed/jruby-embed.properties";
    private final Map<String, String[]> properties;
    private LocalContextProvider provider = null;
    private EmbedRubyRuntimeAdapter runtimeAdapter = new EmbedRubyRuntimeAdapterImpl(this);
    private EmbedRubyObjectAdapter objectAdapter = new EmbedRubyObjectAdapterImpl(this);
    private EmbedRubyInterfaceAdapter interfaceAdapter = new EmbedRubyInterfaceAdapterImpl(this);

    /**
     * Constructs a ScriptingContainer with a default values.
     */
    public ScriptingContainer() {
        this(LocalContextScope.SINGLETON, LocalVariableBehavior.TRANSIENT, defaultProps);
    }

    public ScriptingContainer(LocalContextScope scope) {
        this(scope, LocalVariableBehavior.TRANSIENT, defaultProps);
    }

    public ScriptingContainer(LocalVariableBehavior behavior) {
        this(LocalContextScope.SINGLETON, behavior, defaultProps);
    }

    public ScriptingContainer(LocalContextScope scope, LocalVariableBehavior behavior) {
        this(scope, behavior, defaultProps);
    }

    /**
     * Constructs a ScriptingContainer with a specified local context scope,
     * local variabe behavior and property file.
     *
     * <p>A property file can have key-values pairs. If multiple values are
     * associated to a key, each value is separated by comma.
     * <pre>Example
     * container.ids=ruby, jruby
     * language.extension=rb</pre>
     *
     * @param scope is one of a local context scope defined by {@link LocalContextScope}
     * @param behavior is one of a local variable behavior defined by {@link LocalVariableBehavior}
     * @param propertyname is a propert file name
     */
    public ScriptingContainer(LocalContextScope scope, LocalVariableBehavior behavior, String propertyname) {
        provider = getProviderInstance(scope, behavior);
        Map map = new HashMap<String, String[]>();
        properties = Collections.unmodifiableMap(map);
        if (propertyname == null || propertyname.length() == 0) {
            return;
        }
        prepareProperties(propertyname, map);
        try {
            SystemPropertyCatcher.setJRubyHome(this);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private LocalContextProvider getProviderInstance(LocalContextScope scope, LocalVariableBehavior behavior) {
        switch(scope) {
            case THREADSAFE :
                return new ThreadSafeLocalContextProvider(behavior);
            case SINGLETHREAD :
                return new SingleThreadLocalContextProvider(behavior);
            case SINGLETON :
            default :
                return new SingletonLocalContextProvider(behavior);
        }
    }

    private void prepareProperties(String propertyname, Map<String, String[]> map) {
        try {
            PropertyResourceBundle resource =
                    new PropertyResourceBundle(getClass().getClassLoader().getResourceAsStream(propertyname));
            Enumeration<String> keys = resource.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String[] values = resource.getString(key).split(",\\s*");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }
                map.put(key, values);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Returns an array of values associated to a key
     *
     * @param key is a key in a property file
     * @return values associated to the key
     */
    public String[] getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Returns a version of JRuby that gets ScriptingContainer started
     *
     * @return a JRuby version
     */
    public String getSupportedRubyVersion() {
        Ruby ruby = getRuntime();
        RubyInstanceConfig config = ruby.getInstanceConfig();
        return config.getVersionString().trim();
    }

    /**
     * Returns a provider instance of {@link LocalContextProvider}. When users 
     * want to configure Ruby runtime, they can do by setting class loading paths,
     * {@link org.jruby.RubyInstanceConfig} or {@link org.jruby.util.ClassCache}
     * to the provider before they get Ruby runtime.
     * 
     * @return a provider of {@link LocalContextProvider}
     */
    public LocalContextProvider getProvider() {
        return provider;
    }

    /**
     * Returns a Ruby runtime in one of {@link LocalContextScope}
     *
     * @return Ruby runtime of a spcifed local context
     */
    public Ruby getRuntime() {
        return provider.getRuntime();
    }

    /**
     * Returns a variable map in one of {@link LocalContextScope}. Variables
     * in this map is used to share between Java and Ruby. Map keys are Ruby's
     * variable names, thus they must be vaild Ruby names.
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
     * Returns a value to which the specified key is mapped in a
     * variable map, or null if this map contains no mapping for the key. The key
     * must be a valid Ruby variable name. This is a short cut method of
     * ScriptingContainer#getVarMap().get(key).
     * 
     * @param key is a key whose associated value is to be returned
     * @return a value to which the specified key is mapped, or null if this
     *         map contains no mapping for the key
     */
    public Object get(String key) {
        return provider.getVarMap().get(key);
    }

    /**
     * Associates the specified value with the specified key in a
     * variable map. If the map previously contained a mapping for the key,
     * the old value is replaced. The key must be a valid Ruby variable name.
     * This is a short cut method of ScriptingContainer#getVarMap().put(key, value).
     * 
     * @param key is a key that the specified value is to be associated with
     * @param value is a value to be associated with the specified key
     * @param lines are line numbers to be parsed from. Only the first argument is used for parsing.
     *        This field is optional. When no line number is specified, 0 is applied to.
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object put(String key, Object value) {
        return provider.getVarMap().put(key, value);
    }

    /**
     * Removes the specified Ruby variable with the specified variable name in a
     * variable map. If the map previously contained a mapping for the key,
     * the old value is returned. The key must be a valid Ruby variable name.
     * This is a short cut method of ScriptingContainer#getVarMap().remove(key).
     *
     * @param key is a key that the specified value is to be associated with
     * @return a previous value associated with a key, or null if there was
     *         no mapping for this key.
     */
    public Object remove(String key) {
        return provider.getVarMap().remove(key);
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
     * @param receiver is an instance that will receive this method call
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
     * @param receiver is an instance that will receive this method call
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
     * @param receiver is an instance that will receive this method call
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
     * @param receiver is an instance that will receive this method call
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
     * @param receiver is an instance that will receive this method call
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
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be calleid
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
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be calleid
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
     * @param receiver is an instance that will receive this method call
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    public <T> T callSuper(Object receiver, Object[] args, Class<T> returnType) {
        return objectAdapter.callSuper(receiver, args, returnType);
    }

    /**
     *
     * @param receiver is an instance that will receive this method call
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    public <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType) {
        return objectAdapter.callSuper(receiver, args, block, returnType);
    }

    /**
     * Returns an instance of a requested interface type. An implementation of
     * the requested interface is done by a Ruby script, which has been evaluated
     * before getting the instance.
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
     *     ScriptingcContainer container = new ScriptingContaier();
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
     * Replaces a standard input by a specifed reader
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
        Ruby runtime = getRuntime();
        RubyIO io = new RubyIO(runtime, istream);
        io.getOpenFile().getMainStream().setSync(true);
        runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", io));
        runtime.getObject().getConstantMapForWrite().put("STDIN", io);
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
     * @return an input stream that Ruby runtime has.
     */
    public InputStream getIn() {
        return getRuntime().getIn();
    }

    /**
     * Replaces a standard output by a specifed writer.
     *
     * @param writer is a wrtier to be set
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
        Ruby runtime = getRuntime();
        RubyIO io = new RubyIO(runtime, pstream);
        io.getOpenFile().getMainStream().setSync(true);
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", io));
        runtime.getObject().getConstantMapForWrite().put("STDOUT", io);
        runtime.getGlobalVariables().alias("$>", "$stdout");
        runtime.getGlobalVariables().alias("$defout", "$stdout");
    }

    public void resetWriter() {
        PrintStream pstream = provider.getRubyInstanceConfig().getOutput();
        setOutputStream(pstream);
    }

    /**
     * Returns a wrtier set in an attribute map.
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
     * @return an output stream that Ruby runtime has
     */
    public PrintStream getOut() {
        return getRuntime().getOut();
    }

    /**
     * Replaces a standard error by a specifed writer.
     * 
     * @param errorWriter is a wrtier to be set
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
        Ruby runtime = getRuntime();
        RubyIO io = new RubyIO(runtime, error);
        io.getOpenFile().getMainStream().setSync(true);
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", io));
        runtime.getObject().getConstantMapForWrite().put("STDERR", io);
        runtime.getGlobalVariables().alias("$deferr", "$stderr");
    }

    public void resetErrorWriter() {
        PrintStream error = provider.getRubyInstanceConfig().getError();
        setErrorStream(error);
    }

    /**
     * Returns an error wrtier set in an attribute map.
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
     * @return an error output stream that Ruby runtime has
     */
    public PrintStream getErr() {
        return getRuntime().getErr();
    }
}