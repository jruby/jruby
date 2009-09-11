package java_integration.fixtures;

import junit.framework.Test;
import junit.framework.TestResult;

public class ThrowExceptionOnCreate implements Test {
    public int countTestCases() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void run(TestResult arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
