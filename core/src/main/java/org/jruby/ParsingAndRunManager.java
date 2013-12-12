package org.jruby;

import java.io.IOException;
import java.io.InputStream;

import org.jruby.ast.Node;
import org.jruby.runtime.ThreadContext;

public class ParsingAndRunManager {
    
    public void dealWithRunFromMain(Ruby runtime, InputStream inputStream, String filename) {
        Node scriptNode = runtime.parseFromMain(inputStream, filename);

        // done with the stream, shut it down
        try {inputStream.close();} catch (IOException ioe) {}

        ThreadContext context = runtime.getCurrentContext();

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            if(scriptNode != null) context.setFileAndLine(scriptNode.getPosition());
            
            RubyInstanceConfig config = runtime.getInstanceConfig();
            if (config.isAssumePrinting() || config.isAssumeLoop()) {
                runtime.runWithGetsLoop(scriptNode, config.isAssumePrinting(), config.isProcessLineEnds(),
                        config.isSplit());
            } else {
                runtime.runNormally(scriptNode);
            }
        } finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }

}