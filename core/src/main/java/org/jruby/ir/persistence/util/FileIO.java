package org.jruby.ir.persistence.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public enum FileIO {
    INSTANCE;

    public String readFile(String fileName) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            FileChannel fc = fis.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public void writeToFile(File file, String containment) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        writeToFileCommon(containment, fileOutputStream);
    }

    public void writeToFile(String fileName, String containment) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        writeToFileCommon(containment, fileOutputStream);
    }

    private void writeToFileCommon(String containment, FileOutputStream fos) throws IOException {
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(fos);
            outputStreamWriter.write(containment);
        } finally {
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
        }
    }

}
