/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.util.encoding;

import java.nio.charset.Charset;
import org.jcodings.Encoding;

/**
 *
 * @author headius
 */
public class RubyCoderResult {
    public final String stringResult;
    public final byte[] errorBytes;
    public final Encoding inEncoding;
    public final Encoding outEncoding;
    public final byte[] readagainBytes;
    private final boolean error;
    private final boolean incomplete;
    private final boolean undefined;

    public RubyCoderResult(String stringResult, Encoding inEncoding, Encoding outEncoding, byte[] errorBytes, byte[] readagainBytes) {
        this.errorBytes = errorBytes;
        this.inEncoding = inEncoding;
        this.outEncoding = outEncoding;
        this.readagainBytes = readagainBytes;
        this.stringResult = stringResult;
        this.incomplete = stringResult.equals("invalid_byte_sequence");
        this.undefined = stringResult.equals("undefined_conversion");
        this.error = incomplete || undefined;
    }

    public boolean isError() {
        return error;
    }

    public boolean isInvalid() {
        return incomplete;
    }

    public boolean isUndefined() {
        return undefined;
    }
    
}
