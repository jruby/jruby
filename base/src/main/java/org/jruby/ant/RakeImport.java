package org.jruby.ant;

import org.apache.tools.ant.BuildException;

public class RakeImport extends RakeTaskBase {
    @Override
    public void execute() throws BuildException {
        super.execute();
        
        rakeMethod("import", handleFilenameArgument());
    }
}
