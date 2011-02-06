package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.embed.ScriptingContainer;

public class TestScriptingContainer extends TestCase {

	/**
	 * Test setCurrentDirectory method after minimal setup of a ScriptingContainer.
	 */
	public void testSetCurrentDirectoryAfterMinimalSetup() {
		String directory = System.getProperty( "user.home" );
		ScriptingContainer instance = new ScriptingContainer();
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());
	}

}
