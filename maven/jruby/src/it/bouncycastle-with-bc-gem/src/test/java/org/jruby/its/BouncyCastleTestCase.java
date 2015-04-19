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

    @Test
    public void java() {
        assertEquals("BouncyCastle Security Provider v1.50", new BouncyCastleProvider().getInfo());
    }

    @Test
    public void ruby(){
        ScriptingContainer container = new ScriptingContainer();
        container.setClassloaderDelegate(false);
        Object result = container.parse( "gem 'bouncy-castle-java'; require 'bouncy-castle-java'; Java::OrgBouncycastleJceProvider::BouncyCastleProvider.new.info").run();
        assertEquals( "BouncyCastle Security Provider v1.47", result.toString() );

        result = container.parse("JRuby.runtime.jruby_class_loader").run();
        assertEquals("org.jruby.util.SelfFirstJRubyClassLoader", result.toString().replaceFirst("@.*$", ""));
    }
}
