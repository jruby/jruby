package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface ILoadService {
    /**
     * Init the LOAD_PATH array.
     * 
     * An array of strings, where each string specifies a directory 
     * to be searched for Ruby scripts and binary extensions used by
     * the load and require methods.
     * 
     * The initial value is the value of the arguments passed via the -I
     * command-line option, followed by an installation-defined standard
     * library location, followed by the current directory (``.''). This
     * variable may be set from within a program to alter the default
     * search path; typically, programs use $: &lt;&lt; dir to append
     * dir to the path.
     */
    void init(Ruby runtime, List additionalDirectories);

    boolean load(String file);
    boolean require(String file);

    List getLoadPath();
    List getLoadedFeatures();

    boolean isAutoloadDefined(String name);
    IRubyObject autoload(String name);
    void addAutoload(String name, IAutoloadMethod loadMethod);

    void registerBuiltin(String name, Library library);
}