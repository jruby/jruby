package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.transcode.EConvFlags;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Newline decorator selection for text-mode IO, with the platform passed in so the
 * Windows (RUBY_CRLF_ENVIRONMENT) path is covered on every platform.
 */
public class OpenFileTextModeTest {
    private static final Encoding BINARY = ASCIIEncoding.INSTANCE;
    private static final Encoding UTF8 = UTF8Encoding.INSTANCE;

    // "r" on Windows: fmode TEXTMODE, ecflags carry the default CRLF marker only
    private static final int WINDOWS_TEXT_READ = OpenFile.READABLE | OpenFile.TEXTMODE;
    private static final int WINDOWS_TEXT_WRITE = OpenFile.WRITABLE | OpenFile.TEXTMODE;
    private static final int CRLF = EConvFlags.CRLF_NEWLINE_DECORATOR;
    private static final int UNIVERSAL = EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;

    @Test
    public void textModeReadOnWindowsUsesTheConverter() {
        assertTrue(OpenFile.needsReadConversion(true, null, WINDOWS_TEXT_READ, CRLF));
    }

    @Test
    public void textModeReadOnWindowsConvertsNewlines() {
        assertEquals(UNIVERSAL, OpenFile.readConversionFlags(true, WINDOWS_TEXT_READ, CRLF));
    }

    @Test
    public void textModeReadOnWindowsKeepsAnExplicitUniversalNewline() {
        assertEquals(UNIVERSAL, OpenFile.readConversionFlags(true, WINDOWS_TEXT_READ, UNIVERSAL));
    }

    @Test
    public void binmodeReadOnWindowsDoesNotConvertNewlines() {
        int mode = OpenFile.READABLE | OpenFile.BINMODE;
        assertFalse(OpenFile.needsReadConversion(true, null, mode, 0));
        assertEquals(0, OpenFile.readConversionFlags(true, mode, 0));
    }

    @Test
    public void textModeReadOffWindowsIsUnchanged() {
        assertFalse(OpenFile.needsReadConversion(false, null, OpenFile.READABLE, 0));
        assertEquals(0, OpenFile.readConversionFlags(false, OpenFile.READABLE | OpenFile.TEXTMODE, CRLF));
        assertEquals(UNIVERSAL, OpenFile.readConversionFlags(false, OpenFile.READABLE | OpenFile.TEXTMODE, UNIVERSAL));
    }

    @Test
    public void textModeWriteOnWindowsUsesTheConverter() {
        assertTrue(OpenFile.needsWriteConversion(true, true, null, BINARY, WINDOWS_TEXT_WRITE, CRLF));
    }

    @Test
    public void textModeWriteOnWindowsWithAnEncodingUsesTheConverter() {
        assertTrue(OpenFile.needsWriteConversion(true, true, UTF8, BINARY, WINDOWS_TEXT_WRITE, CRLF));
    }

    @Test
    public void textModeWriteToAWindowsPipeDoesNotUseTheConverter() {
        assertFalse(OpenFile.needsWriteConversion(true, false, null, BINARY, WINDOWS_TEXT_WRITE, CRLF));
    }

    @Test
    public void explicitDecoratorOrEncodingOnAWindowsPipeUsesTheConverter() {
        assertTrue(OpenFile.needsWriteConversion(true, false, null, BINARY, WINDOWS_TEXT_WRITE, EConvFlags.CR_NEWLINE_DECORATOR));
        assertTrue(OpenFile.needsWriteConversion(true, false, UTF8, BINARY, WINDOWS_TEXT_WRITE, CRLF));
    }

    @Test
    public void binmodeWriteOnWindowsDoesNotUseTheConverter() {
        assertFalse(OpenFile.needsWriteConversion(true, true, BINARY, BINARY, OpenFile.WRITABLE | OpenFile.BINMODE, 0));
    }

    @Test
    public void writeOffWindowsIsUnchanged() {
        assertFalse(OpenFile.needsWriteConversion(false, true, null, BINARY, OpenFile.WRITABLE, 0));
        assertTrue(OpenFile.needsWriteConversion(false, true, null, BINARY, OpenFile.WRITABLE | OpenFile.TEXTMODE, CRLF));
        assertTrue(OpenFile.needsWriteConversion(false, false, UTF8, BINARY, OpenFile.WRITABLE, 0));
    }
}
