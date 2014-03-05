/*
Copyright (c) 2000-2014 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
 */
package org.jruby.ext.openssl.impl.pem;

// NOTE: simply delete this whole thing once using a BC release that has PEMParser

import java.io.IOException;

/**
 * Part of <code>PEMReader</code> (replacement) support.
 *
 * @note org.bouncycastle.openssl.PEMParser (not available in BC 1.47)
 */
public class PEMException extends IOException {
    Exception    underlying;

    public PEMException(
        String    message)
    {
        super(message);
    }

    public PEMException(
        String        message,
        Exception    underlying)
    {
        super(message);
        this.underlying = underlying;
    }

    public Exception getUnderlyingException()
    {
        return underlying;
    }


    public Throwable getCause()
    {
        return underlying;
    }
}