/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ripper;

/**
 *
 * @author enebo
 */
public interface RipperParserState {
    public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop);    
}
