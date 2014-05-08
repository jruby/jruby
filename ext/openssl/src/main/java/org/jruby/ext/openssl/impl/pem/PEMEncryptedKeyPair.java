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

import org.bouncycastle.operator.OperatorCreationException;

/**
 * Part of <code>PEMReader</code> (replacement) support.
 *
 * @note org.bouncycastle.openssl.PEMParser (not available in BC 1.47)
 */
public class PEMEncryptedKeyPair {

    private final String dekAlgName;
    private final byte[] iv;
    private final byte[] keyBytes;
    private final PEMKeyPairParser parser;

    PEMEncryptedKeyPair(String dekAlgName, byte[] iv, byte[] keyBytes, PEMKeyPairParser parser)
    {
        this.dekAlgName = dekAlgName;
        this.iv = iv;
        this.keyBytes = keyBytes;
        this.parser = parser;
    }

    public PEMKeyPair decryptKeyPair(PEMDecryptorProvider keyDecryptorProvider)
        throws IOException
    {
        try
        {
            PEMDecryptor keyDecryptor = keyDecryptorProvider.get(dekAlgName);

            return parser.parse(keyDecryptor.decrypt(keyBytes, iv));
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (OperatorCreationException e)
        {
            throw new PEMException("cannot create extraction operator: " + e.getMessage(), e);
        }
        catch (Exception e)
        {
            throw new PEMException("exception processing key pair: " + e.getMessage(), e);
        }
    }
}