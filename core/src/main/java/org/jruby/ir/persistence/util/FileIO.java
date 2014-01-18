package org.jruby.ir.persistence.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FileIO {
    public static final Charset CHARSET = Charset.forName("UTF-8");

    public static String readFile(File file) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            return CHARSET.decode(fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())).toString();
        } finally {
            if (fis != null) fis.close();
        }
    }

    public static void writeToFile(File file, String containment) throws IOException {
        writeToFileCommon(containment, new FileOutputStream(file, true));
    }

    public static void writeToFile(String fileName, String containment) throws IOException {
        writeToFileCommon(containment, new FileOutputStream(new File(fileName), true));
    }

    private static void writeToFileCommon(String containment, FileOutputStream fos) throws IOException {
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(fos);
            outputStreamWriter.write(containment);
        } finally {
            if (outputStreamWriter != null) outputStreamWriter.close();
        }
    }

}
