/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.io.IOException;
import org.jruby.ir.IRScope;

/**
 *
 * @author enebo
 */
public class IRReader {
    public static void load(IRReaderDecoder file, IRScope script) throws IOException {
        int headersOffset = file.decodeInt();
        int poolOffset = file.decodeInt();
        
        file.seek(headersOffset);
        
        
    }    
}
