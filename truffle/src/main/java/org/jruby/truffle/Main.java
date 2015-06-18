package org.jruby.truffle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.RootNode;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Main {

	public static void main(String[] args) {
		final RubyInstanceConfig config = new RubyInstanceConfig();
		config.setHardExit(true);
		config.processArguments(args);

		InputStream in = config.getScriptSource();
		String filename = config.displayedFileName();

		final Ruby runtime = Ruby.newInstance(config);
		final AtomicBoolean didTeardown = new AtomicBoolean();

		config.setCompileMode(RubyInstanceConfig.CompileMode.TRUFFLE);

		if (config.isHardExit()) {
			// we're the command-line JRuby, and should set a shutdown hook for
			// teardown.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					if (didTeardown.compareAndSet(false, true)) {
						runtime.tearDown();
					}
				}
			});
		}

		if (in == null) {
			return;
		} else {

			// global variables
			IAccessor d = new ValueAccessor(runtime.newString(filename));
			runtime.getGlobalVariables().define("$PROGRAM_NAME", d,
					GlobalVariable.Scope.GLOBAL);
			runtime.getGlobalVariables().define("$0", d,
					GlobalVariable.Scope.GLOBAL);

			for (Iterator i = config.getOptionGlobals().entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				Object value = entry.getValue();
				IRubyObject varvalue;
				if (value != null) {
					varvalue = runtime.newString(value.toString());
				} else {
					varvalue = runtime.getTrue();
				}
				runtime.getGlobalVariables().set(
						"$" + entry.getKey().toString(), varvalue);
			}

			RootNode scriptNode = (RootNode) runtime
					.parseFromMain(filename, in);

			// if no DATA, we're done with the stream, shut it down
			if (runtime.fetchGlobalConstant("DATA") == null) {
				try {
					in.close();
				} catch (IOException ioe) {
				}
			}
			ThreadContext context = runtime.getCurrentContext();

			String oldFile = context.getFile();
			int oldLine = context.getLine();

			try {
				context.setFileAndLine(scriptNode.getPosition());
				runtime.getTruffleContext().execute(scriptNode);
			} finally {
				context.setFileAndLine(oldFile, oldLine);
				runtime.shutdownTruffleContextIfRunning();
			}

		}

	}

}
