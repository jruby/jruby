package org.jruby.ir.persistence.util;

import java.io.File;

public class IRFileExpert {
    private static final String IR_FILE_EXTENSION = ".ir";
    private static final String IR_FOLDER = "ir";
    private static final String EXTENSION_SEPARATOR = ".";

    private static final File IR_ROOT_FOLDER = new File(System.getProperty("user.home"), IR_FOLDER);

    public static File getIRFileInIntendedPlace(String fileName) {
        fileName = fileName.replaceAll("file:", "");
        File rbFile = new File(fileName);
        fileName = rbFile.getAbsolutePath();

        int startOfFileName = fileName.lastIndexOf(File.separator) + 1;
        File irFolder = IR_ROOT_FOLDER;
        if (startOfFileName > 0) {
            String fileFolderPath = fileName.substring(0, startOfFileName);
            irFolder = new File(irFolder, fileFolderPath);
        }
        irFolder.mkdirs();

        int endOfFileName = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        String fileNameWithoutExtension;
        if (endOfFileName > 0) {
            fileNameWithoutExtension = fileName.substring(startOfFileName, endOfFileName);
        } else {
            fileNameWithoutExtension = fileName;
        }

        return new File(irFolder, fileNameWithoutExtension + IR_FILE_EXTENSION);
    }
}
