package org.jruby.ext.strscan;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.ext.strscan.RubyStringScanner;
import org.jruby.runtime.load.Library;

/**
 * @author kscott
 *
 */
public class StringScannerLibrary implements Library {

	/**
	 * @see org.jruby.runtime.load.Library#load(org.jruby.Ruby)
	 */
	public void load(Ruby runtime, boolean wrap) throws IOException {
		RubyStringScanner.createScannerClass(runtime);
	}

}
