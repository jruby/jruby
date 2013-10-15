package org.jruby.ir.persistence.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum IRFilePathManager {
    INSTANCE;

    private static final String IR_FILE_EXTENSION = ".ir";
    private static final String IR_FOLDER = "ir";
    private static final String EXTENSION_SEPARATOR = ".";

    public File getIRFile(File parentDir, String rbFileName) {
        File irFolder = createOrFindIrFolder(parentDir);

        String irFileName = getIrFileName(rbFileName);

        return new File(irFolder, irFileName);
    }

    private File createOrFindIrFolder(File parentDir) {
        File irFolder = new File(parentDir, IR_FOLDER);
        if (!irFolder.exists()) {
            irFolder.mkdir();
        }
        return irFolder;
    }

    private String getIrFileName(String rbFileName) {
        int endOfFileName = rbFileName.lastIndexOf(EXTENSION_SEPARATOR);
        int startOfFileName = rbFileName.lastIndexOf(File.separator) + 1;
        String fileNameWithoutExtension;
        if (endOfFileName != 0) {
            fileNameWithoutExtension = rbFileName.substring(startOfFileName, endOfFileName);
        } else {
            fileNameWithoutExtension = rbFileName;
        }        

        String irFileName = fileNameWithoutExtension + IR_FILE_EXTENSION;
        return irFileName;
    }

}
