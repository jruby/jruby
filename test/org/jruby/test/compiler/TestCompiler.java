
package org.jruby.test.compiler;

import junit.framework.TestCase;
import org.ablaf.ast.INode;
import org.jruby.Ruby;
import org.jruby.compiler.bytecodes.PushFixnum;
import org.jruby.compiler.bytecodes.AssignLocal;
import org.jruby.compiler.ByteCodeSequence;
import org.jruby.compiler.Compiler;

public class TestCompiler extends TestCase {

    private INode parse(String code) {
        return Ruby.getDefaultInstance().parse(code, "*test-script*");
    }

    public void testAssignment() {
        Compiler c = new Compiler();
        INode syntaxTree = parse("x = 10");
        ByteCodeSequence bc = c.compile(syntaxTree);
        assertEquals(2, bc.instructionCount());
        assertEquals(new PushFixnum(10), bc.at(0));
        assertEquals(AssignLocal.class, bc.at(1).getClass());
    }
}
