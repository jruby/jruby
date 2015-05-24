package org.jruby.compiler;

import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.ast.executable.ScriptAndCode;
import org.jruby.util.ClassDefiningClassLoader;

/**
 * Created by uwe on 20/5/15.
 */
public interface Compiler {
    ScriptAndCode tryCompile(Node node, ClassDefiningClassLoader classLoader, Ruby ruby);
}
