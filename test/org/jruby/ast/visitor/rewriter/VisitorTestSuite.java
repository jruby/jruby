package org.jruby.ast.visitor.rewriter;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jruby.ast.visitor.rewriter.utils.TestBooleanStateStack;
import org.jruby.ast.visitor.rewriter.utils.TestOperators;

public class VisitorTestSuite extends TestSuite {
	
	public static Test suite() throws Throwable {
		TestSuite suite = new TestSuite();
		suite.addTest(SourceRewriteTester.suite());
		suite.addTest(new TestSuite(TestOperators.class));
		suite.addTest(new TestSuite(TestBooleanStateStack.class));
		suite.addTest(new TestSuite(TestReWriteVisitor.class));
		return suite;
	}
}
