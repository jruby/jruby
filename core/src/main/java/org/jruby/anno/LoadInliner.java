package org.jruby.anno;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LoadInliner {
    public static void main(String[] args) {
        File sourceDir = new File(args[0]);
        File targetDir = new File(args[1]);

        for (int i = 2; i < args.length; i++) {
            File sourceFile = new File(sourceDir, args[i]);
            File targetFile = new File(targetDir, args[i]);

            System.out.println("inlining loads from " + sourceFile + " into " + targetFile);

            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(targetFile)) {

                PrintStream pos = new PrintStream(fos);

                byte[] bytes = new byte[fis.available()];
                fis.read(bytes);

                String[] lines = new String(bytes).split("\n");
                for (String line : lines) {
                    if (line.startsWith("load '")) {
                        String inlined = line.substring(6, line.length() - 1);
                        try (FileInputStream fis2 = new FileInputStream(new File(sourceDir, inlined))) {
                            byte[] bytes2 = new byte[fis2.available()];
                            fis2.read(bytes2);
                            String inlinedSource = "# " + inlined + "\n\n" + new String(bytes2) + "\n";
                            pos.println(inlinedSource);
                        }
                    } else {
                        pos.println(line);
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }
}
