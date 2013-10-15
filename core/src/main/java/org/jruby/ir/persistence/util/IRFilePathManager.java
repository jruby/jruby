package org.jruby.ir.persistence.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum IRFilePathManager {
    INSTANCE;

    private static final String IR_FILE_EXTENSION = ".ir";
    private static final String IR_FOLDER = "ir";
    private static final String RB_FILE_PATTERN = ".*" + File.separator + "(.*).rb";
    private static Pattern rbFilePattern;

    public File getIRFile(File parentDir, String rbFileName) {
        initIfRequired();
        
        File irFolder = createOrFindIrFolder(parentDir);
               
        String irFileName = getIrFileName(rbFileName);
        
        return new File(irFolder, irFileName);
    }
    
    private void initIfRequired() {
        if (rbFilePattern == null) {
            rbFilePattern = Pattern.compile(RB_FILE_PATTERN);
        }
    }

    private File createOrFindIrFolder(File parentDir) {
        File irFolder = new File(parentDir, IR_FOLDER);
        if(!irFolder.exists()) {
            irFolder.mkdir();
        }
        return irFolder;
    }
    
    private String getIrFileName(String rbFileName) {
        Matcher rbFileMatcher = rbFilePattern.matcher(rbFileName);
        String fileNameWithoutExtension; 
        if(rbFileMatcher.matches()) {
            fileNameWithoutExtension = rbFileMatcher.group(1);
        } else {
            fileNameWithoutExtension = rbFileName;
        }
        
        String irFileName = fileNameWithoutExtension + IR_FILE_EXTENSION;
        return irFileName;
    }

}
