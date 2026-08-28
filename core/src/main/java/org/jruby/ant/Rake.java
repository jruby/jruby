package org.jruby.ant;

import java.util.List;
import org.apache.tools.ant.BuildException;

public class Rake extends RakeTaskBase {
    private String taskname; // A list?

    @Override
    public void execute() throws BuildException {
        super.execute();
        List args = handleFilenameArgument();

        if (taskname != null) args.add(taskname);

        rakeMethod("execute", args.toArray(new Object[args.size()]));
    }

    // FIXME?: Allow list of tasks to be executed
    public void setTask(String taskname) {
        this.taskname = taskname;
    }
    // FIXME: Add flag to allow registering all defined ant tasks in Rake dependency tree?
}
