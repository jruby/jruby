package org.jruby.its;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import junit.framework.TestCase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BouncyCastleTestCase {

    @Before
    public void setupProvider() throws Exception{
        Security.addProvider( new BouncyCastleProvider() );
    }

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
