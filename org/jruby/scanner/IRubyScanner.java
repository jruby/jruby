package org.jruby.scanner;

import java.io.*;

public interface IRubyScanner {
    public IToken getNextToken() throws IOException;
    
    public void addScannerListener(IScannerListener listener);
    public void removeScannerListener(IScannerListener listener);
}