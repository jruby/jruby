/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jruby.truffle.test.core.*;
import org.jruby.truffle.test.debug.*;
import org.jruby.truffle.test.language.*;

public class TruffleTestSuite extends TestSuite {

    public static Test suite() throws Throwable {
        TestSuite suite = new TestSuite();
        
        suite.addTestSuite(ArrayTests.class);
        suite.addTestSuite(BignumTests.class);
        suite.addTestSuite(BoolTests.class);
        suite.addTestSuite(ContinuationTests.class);
        suite.addTestSuite(FiberTests.class);
        suite.addTestSuite(FixnumTests.class);
        suite.addTestSuite(FloatTests.class);
        suite.addTestSuite(HashTests.class);
        suite.addTestSuite(IntegerTests.class);
        suite.addTestSuite(KernelTests.class);
        suite.addTestSuite(MathTests.class);
        suite.addTestSuite(org.jruby.truffle.test.core.ModuleTests.class);
        suite.addTestSuite(ProcTests.class);
        suite.addTestSuite(RangeTests.class);
        suite.addTestSuite(RegexpTests.class);
        suite.addTestSuite(StringTests.class);
        suite.addTestSuite(SymbolTests.class);
        suite.addTestSuite(ThreadTests.class);

        // Disabled until debug nodes work again
        //suite.addTestSuite(DebugTests.class);

        suite.addTestSuite(AndTests.class);
        suite.addTestSuite(BlockTests.class);
        suite.addTestSuite(CaseTests.class);
        suite.addTestSuite(ClassLocalTests.class);
        suite.addTestSuite(ClassTests.class);
        suite.addTestSuite(ClassLocalTests.class);
        suite.addTestSuite(ConstantTests.class);
        suite.addTestSuite(ForTests.class);
        suite.addTestSuite(GlobalVariableTests.class);
        suite.addTestSuite(IfTests.class);
        suite.addTestSuite(InterpolatedStringTests.class);
        suite.addTestSuite(LocalTests.class);
        suite.addTestSuite(MethodTests.class);
        suite.addTestSuite(org.jruby.truffle.test.language.ModuleTests.class);
        suite.addTestSuite(MultipleAssignmentTests.class);
        suite.addTestSuite(OrTests.class);
        suite.addTestSuite(PolymorphismTests.class);
        suite.addTestSuite(RaiseRescueTests.class);
        suite.addTestSuite(RedefinitionTests.class);
        suite.addTestSuite(ShortcutTests.class);
        suite.addTestSuite(SpecialVariableTests.class);
        suite.addTestSuite(UntilTests.class);
        suite.addTestSuite(WhileTests.class);

        return suite;
    }
}
