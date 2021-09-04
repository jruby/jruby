package org.jruby.runtime.io;

import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.EOFError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Random;
import java.util.UUID;

public class RubyIOTest {
    private static final String SCRIPT = "read_file.rb";

    private File file;

    @Before
    public void createFile() throws IOException {
        file = generateTempFile();
    }

    @After
    public void deleteFile() {
        file.delete();
    }

    @Test(expected = EOFError.class)
    public void sysread() throws Throwable {
        ScriptingContainer scriptingContainer = new ScriptingContainer();
        scriptingContainer.setArgv(new String[] { file.getAbsolutePath() });

        InputStream inputStream = getClass().getResourceAsStream(SCRIPT);
        try {
            scriptingContainer.runScriptlet(inputStream, SCRIPT);
        } catch (EvalFailedException efe) {
            throw efe.getCause();
        }
    }

    private File generateTempFile() throws IOException {
        File file = File.createTempFile(UUID.randomUUID().toString(), null);
        PrintWriter printWriter = new PrintWriter(new FileWriter(file));
        int max = new Random().nextInt(100);
        for (int i = 0; i < max; i++) {
            printWriter.println(UUID.randomUUID().toString());
        }
        printWriter.close();
        return file;
    }
}
