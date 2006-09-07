package org.jruby.libraries;

import java.io.IOException;

import org.jruby.IRuby;
import org.jruby.RubyStringScanner;
import org.jruby.runtime.load.Library;

/**
 * @author kscott
 *
 */
public class StringScannerLibrary implements Library {

	/**
	 * @see org.jruby.runtime.load.Library#load(org.jruby.IRuby)
	 */
	public void load(IRuby runtime) throws IOException {
		RubyStringScanner.createScannerClass(runtime);
	}

}
