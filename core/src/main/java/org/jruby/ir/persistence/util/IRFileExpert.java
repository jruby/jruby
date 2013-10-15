package org.jruby.ir.persistence.util;

import java.io.File;

import org.jruby.RubyInstanceConfig;

public enum IRFileExpert {
    INSTANCE;

    private static final String IR_FILE_EXTENSION = ".ir";
    private static final String IR_FOLDER = "ir";
    private static final String EXTENSION_SEPARATOR = ".";

    public File getIRFileInIntendedPlace(RubyInstanceConfig config) {
        // Place ir files inside ir folder under current directory
        File parentDir = new File(config.getCurrentDirectory());
        String rbFileName = config.displayedFileName();

        return getIrFile(parentDir, rbFileName);
    }

    public boolean isIrFileName(String name) {
        // FIXME: primitive check by extension
        return name.endsWith(IR_FILE_EXTENSION);
    }
    
    public boolean isIrFileForRbFileFound(RubyInstanceConfig config) {
        return getIRFileInIntendedPlace(config).exists();
    }

    private File getIrFile(File parentDir, String fileName) {
        if (isIrFileName(fileName)) { // it is already ir file 
            return new File(parentDir, fileName);
        } else {
            File irFolder = createOrFindIrFolder(parentDir);

            String irFileName = getIrFileName(fileName);

            return new File(irFolder, irFileName);
        }
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



