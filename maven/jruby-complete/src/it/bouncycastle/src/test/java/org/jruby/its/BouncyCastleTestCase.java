package org.jruby.its;

import junit.framework.TestCase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jruby.embed.ScriptingContainer;
import org.junit.Test;
import static org.junit.Assert.*;

public class BouncyCastleTestCase {
    @Test
    public void java(){
        assertEquals( "BouncyCastle Security Provider v1.49", new BouncyCastleProvider().getInfo() );
    }

    @Test
    public void ruby(){
        ScriptingContainer container = new ScriptingContainer();
        Object result = container.parse( "require 'openssl'; Java::OrgBouncycastleJceProvider::BouncyCastleProvider.new.info").run();
        assertEquals( "BouncyCastle Security Provider v1.47", result.toString() );
    }
}
