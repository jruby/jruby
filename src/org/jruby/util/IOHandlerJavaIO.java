/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;

import org.jruby.Ruby;

/**
 */
public abstract class IOHandlerJavaIO extends IOHandler {
    // Last char to be 'ungot'.  <0 indicates nothing waiting to be re-got
    protected int ungotc = -1;

    protected IOHandlerJavaIO(Ruby runtime) {
    super(runtime);
    }

    public ByteList gets(ByteList separatorString) throws IOException, BadDescriptorException {
        checkReadable();

        if (separatorString == null) {
            return getsEntireStream();
        }

        final ByteList separator = (separatorString == PARAGRAPH_DELIMETER) ?
            ByteList.create("\n\n") : separatorString;

        byte c = (byte)read();
        if (c == -1) {
            return null;
        }

        ByteList buffer = new ByteList();

        LineLoop : while (true) {
            while (c != separator.bytes[separator.begin] && c != -1) {
                buffer.append(c);
                c = (byte)read();
            }
            for (int i = 0; i < separator.realSize; i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != separator.bytes[separator.begin + i]) {
                    continue LineLoop;
                }
                buffer.append(c);
                if (i < separator.realSize - 1) {
                    c = (byte)read();
                }
            }
            break;
        }

        if (separatorString == PARAGRAPH_DELIMETER) {
            while (c == separator.bytes[separator.begin]) {
                c = (byte)read();
            }
            ungetc(c);
        }

        return buffer;
    }

    public ByteList getsEntireStream() throws IOException {
        ByteList result = new ByteList();
        int c;
        while ((c = (byte)read()) != -1) {
            result.append(c);
        }

        // We are already at EOF
        if (result.realSize == 0) {
            return null;
        }

        return result;
    }

    public int read() throws IOException {
        try {
            if (ungotc >= 0) {
                int c = ungotc;
                ungotc = -1;
                return c;
            }

            return sysread();
        } catch (EOFException e) {
            return -1;
        }
    }

    public int getc() throws IOException, BadDescriptorException {
        checkReadable();

        int c = read();

        if (c == -1) {
            return c;
        }
        return c & 0xff;
    }

    public ByteList read(int number) throws IOException, BadDescriptorException {
        try {

            if (ungotc >= 0) {
                ByteList buf2 = sysread(number - 1);
                buf2.prepend((byte)ungotc);
                ungotc = -1;
                return buf2;
            }

            return sysread(number);
        } catch (EOFException e) {
            return null;
        }
    }

    public void ungetc(int c) {
        // Ruby silently ignores negative ints for some reason?
        if (c >= 0) {
            ungotc = c;
        }
    }

    public void putc(int c) throws IOException, BadDescriptorException {
        try {
            syswrite(c);
            flush();
        } catch (IOException e) {
        }
    }

    public int write(ByteList string) throws IOException, BadDescriptorException {
        return syswrite(string);
    }

    protected int sysread(ByteList buf, int length) throws IOException {
        if (buf == null) {
            throw new IOException("sysread2: Buf is null");
        }

        int i = 0;
        for (;i < length; i++) {
            int c = sysread();

            if (c == -1) {
                if (i <= 0) {
                    return -1;
                }
                break;
            }

            buf.append(c);
        }

        return i;
    }

    // Question: We should read bytes or chars?
    public ByteList sysread(int number) throws IOException, BadDescriptorException {
        if (!isOpen()) {
            throw new IOException("File not open");
        }
        checkReadable();

        ByteList buf = new ByteList(number);
        int position = 0;

        while (position < number) {
            int s = sysread(buf, number - position);

            if (s == -1) {
                if (position <= 0) {
                    throw new EOFException();
                }
                break;
            }

            position += s;
        }

        return buf;
    }

    public abstract int sysread() throws IOException;

    public abstract InputStream getInputStream();
    public abstract OutputStream getOutputStream();
    
    public int ready() throws IOException {
        return getInputStream().available();
    }
}
