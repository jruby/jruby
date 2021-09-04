package org.jruby.its;

import junit.framework.TestCase;

import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.ScriptingContainer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TerminateContainerAndExtensionsTestCase {

    @Test
    public void terminteAndStartAgain(){
        ScriptingContainer container = new ScriptingContainer();
        String result = container.parse( "require 'thread_safe';ThreadSafe::Cache.superclass").run().toString();
        assertEquals(result, "ThreadSafe::JRubyCacheBackend");
        container.terminate();

        // no things should work as before
        container = new ScriptingContainer();
        result = container.parse( "require 'thread_safe';ThreadSafe::Cache.superclass").run().toString();
        assertEquals(result, "ThreadSafe::JRubyCacheBackend");
        container.terminate();
    }
}
