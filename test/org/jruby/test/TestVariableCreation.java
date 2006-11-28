package org.jruby.test;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;

public class TestVariableCreation  extends TestCase {
    private static IRuby r;
    private static RaiseException failed;
	public  void testLocalVars() {
        r = Ruby.getDefaultInstance();
		// define new method		
		r.evalScript("a = 1\n");
		r.evalScript("a.to_s");
		
		// will run on non main thread
		Runnable run = new Runnable(){
			public void run(){
				try {
					r.evalScript("a.to_s");
				} catch(RaiseException ex){
					failed = ex;
				}
			}
		};
		Thread n = new Thread(run);
		
		n.start();		
		try {
			n.join();
			assertNotNull(failed);
            assertEquals("NameError", failed.getException().getMetaClass().getName());
		} catch (InterruptedException e) {
			fail();
		}
		
		
		

	}

}
