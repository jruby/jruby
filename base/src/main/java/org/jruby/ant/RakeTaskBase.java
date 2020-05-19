package org.jruby.ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jruby.embed.ScriptingContainer;

public class RakeTaskBase extends Task {
    private Object rakeWrapper;
    private ScriptingContainer container;

    protected String filename;

    public RakeTaskBase() {
        acquireRakeReference();
    }

    public void setFile(String filename) {
        this.filename = filename;
    }

    @Override
    public void execute() throws BuildException {
        container.put("$project", getProject());             // set project so jruby ant lib gets it
    }

    protected void acquireRakeReference() {
        ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            System.setProperty("jruby.native.enabled", "false"); // Problem with cl w/ jnr + jffi
            container = new ScriptingContainer();

            // FIXME: This needs to be replaced by something which does not assume CWD
            container.setLoadPaths(Arrays.asList("lib"));
            container.runScriptlet("require 'ant/tasks/raketasks'");

            rakeWrapper = container.runScriptlet("RakeWrapper.new");
        } finally {
            Thread.currentThread().setContextClassLoader(prevClassLoader);
        }
    }

    protected List handleFilenameArgument() {
        List args = new ArrayList();

        if (filename != null) {
            args.add("-f");
            args.add(filename);
        }

        return args;
    }

    public void rakeMethod(String method, Object... args) throws BuildException {
        try {
            container.callMethod(rakeWrapper, method, args);
        } catch(Exception e) {
            throw new BuildException("Build failed: " + e.getMessage(), e);
        }
    }
}
