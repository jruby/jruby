package org.jruby.ir.persistence.read;

public enum IRReadingContext {
    INSTANCE;
    
    private ThreadLocal<String> fileNameLocal = new ThreadLocal<String>();

    public void setFileName(String fileName) {
        fileNameLocal.set(fileName);
    }
    
    public String getFileName() {
        return fileNameLocal.get();
    }

}
