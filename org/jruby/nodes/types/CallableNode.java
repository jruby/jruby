/*
 * CallableNode.java
 *
 * Created on 2. November 2001, 16:14
 */

package org.jruby.nodes.types;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public interface CallableNode {
    public RubyObject call(Ruby ruby, RubyObject recv, RubyId id, RubyObject[] args, boolean noSuper);
}