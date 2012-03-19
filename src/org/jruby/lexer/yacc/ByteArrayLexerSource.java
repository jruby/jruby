package org.jruby.lexer.yacc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jruby.util.ByteList;

/**
 *
 * @author nicksieger
 */
public class ByteArrayLexerSource extends LexerSource {
    private Cursor readCursor;
    private Cursor mainCursor, pushbackCursor;
    private final boolean captureSource;

    public ByteArrayLexerSource(String sourceName, byte[] in, List<String> list, int line,
            boolean extraPositionInformation) {
        super(sourceName, list, line, extraPositionInformation);
        this.readCursor = new ByteArrayCursor(in);
        this.mainCursor = readCursor;
        this.pushbackCursor = new PushbackCursor(mainCursor, new ByteList(128));
        this.captureSource = list != null;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public boolean matchMarker(ByteList marker, boolean indent, boolean withNewline) throws IOException {
        int matchPos = 0;
        if (indent) {
            for (int c = readCursor.at(matchPos);
                 c != RubyYaccLexer.EOF && Character.isWhitespace(c) && c != '\n';
                 c = readCursor.at(++matchPos));
        }
        for (int i = 0; i < marker.length(); i++) {
            if (readCursor.at(matchPos) != marker.get(i)) {
                return false;
            }
            matchPos++;
        }
        if (withNewline) {
            int c = readCursor.at(matchPos++);
            if (c == '\r') {
                // skip
                c = readCursor.at(matchPos);
            }
            if (c != '\n' && c != RubyYaccLexer.EOF) {
                return false;
            }
        }
        for (int i = 0; i < matchPos; i++) {
            readCursor.read();
        }
        return true;
    }

    @Override
    public int read() {
        return readCursor.read();
    }

    @Override
    public ByteList readUntil(char marker) throws IOException {
        return readUntil(marker, true);
    }

    private ByteList readUntil(char marker, boolean nullIfEnd) throws IOException {
        ByteList result = new ByteList(128);
        int c;
        while ((c = readCursor.read()) != marker && c != RubyYaccLexer.EOF) {
            result.append(c);
        }

        if (nullIfEnd && c == RubyYaccLexer.EOF) {
            return null;
        }

        return result;
    }

    @Override
    public ByteList readLineBytes() throws IOException {
        return readUntil('\n', false);
    }

    @Override
    public int skipUntil(int marker) throws IOException {
        int c;
        do {
            c = readCursor.read();
        } while (c != marker && c != RubyYaccLexer.EOF);
        return c;
    }

    @Override
    public void unread(int c) {
        if (c == RubyYaccLexer.EOF) {
            return;
        }
        if (captureSource) {
            uncaptureFeature(c);
        }
        readCursor.unread(c);
    }

    @Override
    public void unreadMany(CharSequence line) {
        for (int i = line.length() - 1; i >= 0; i--) {
            unread(line.charAt(i));
        }
    }

    @Override
    public boolean peek(int c) throws IOException {
        return readCursor.at(0) == c;
    }

    @Override
    public boolean lastWasBeginOfLine() {
        int c = readCursor.at(-1);
        return c == '\n' || c == RubyYaccLexer.EOF;
    }

    @Override
    public boolean wasBeginOfLine() {
        final int c = readCursor.at(-2);
        return c == '\n' || (c == RubyYaccLexer.EOF && c != readCursor.at(-1));
    }

    @Override
    public String getCurrentLine() {
        int lineOffset = 0;
        int c;
        while ((c = readCursor.at(lineOffset - 1)) != '\n' && c != RubyYaccLexer.EOF) {
            lineOffset--;
        }
        String ptr = makePointer(-(lineOffset + 1));
        StringBuilder lineBuilder = new StringBuilder();
        for (c = readCursor.at(lineOffset); c != '\n' && c != RubyYaccLexer.EOF; c = readCursor.at(++lineOffset)) {
            lineBuilder.append((char) c);
        }
        lineBuilder.append('\n').append(ptr);
        return lineBuilder.toString();
    }

    @Override
    public InputStream getRemainingAsStream() {
        ByteList buf = new ByteList(128);
        int c;
        while ((c = read()) != RubyYaccLexer.EOF) {
            buf.append(c);
        }
        return new ByteArrayInputStream(buf.getUnsafeBytes(), 0, buf.length());
    }

    private int forward(int c) {
        if (c != RubyYaccLexer.EOF) {
            offset++;
            switch (c) {
                case '\n':
                    line++;
                    break;
                case '\r':
                    // We are peeking ahead.  but we need to make sure we uncapture after this read.  Otherwise
                    // we double report \n 
                    if ((c = read()) != '\n') {
                        unread(c);
                        c = '\r';
                    } else {
                      if (captureSource){
                        uncaptureFeature(c);
                        captureFeature('\r');
                      }
                    }
                    break;
            }
        }
        if (captureSource) {
            captureFeature(c);
        }
        return c;
    }

    private void backward(int c) {
        offset--;
        if (c == '\n') {
            line--;
            if (readCursor.at(-1) == '\r') {
                unread('\r');
            }
        }
    }

    interface Cursor {
        int read();
        void unread(int c);
        /**
         * Read the character at the offset given without moving the cursor
         * @param offset relative to the current cursor position; -1 means previous character
         * @return the character at that offset
         */
        int at(int offset);
    }

    class ByteArrayCursor implements Cursor {
        private final byte[] region;
        private int index;
        public ByteArrayCursor(byte[] region) {
            this.region = region;
            this.index = 0;
        }
        public int read() {
            if (index >= region.length) {
                return forward(RubyYaccLexer.EOF);
            }
            return 0xff & forward(region[index++]);
        }
        public void unread(int c) {
            if (index > 0 && region[index - 1] == c) {
                index--;
                backward(c);
            } else {
                readCursor = pushbackCursor;
                pushbackCursor.unread(c);
            }
        }
        public int at(int offset) {
            int location = index + offset;
            if (location >= region.length || location < 0) {
                return RubyYaccLexer.EOF;
            }
            return 0xff & region[location];
        }
    }

    class PushbackCursor implements Cursor {
        private final Cursor parent;
        private final ByteList region;
        public PushbackCursor(Cursor prev, ByteList region) {
            this.parent = prev;
            this.region = region;
        }
        public int read() {
            int index = region.length() - 1;
            if (index < 0) {
                readCursor = parent;
                return parent.read();
            }
            int c = 0xff & region.get(index);
            region.setRealSize(index);
            return forward(c);
        }
        public void unread(int c) {
            region.append(c);
            backward(c);
        }
        public int at(int offset) {
            if (offset < 0) {
                return parent.at(offset);
            } else if (offset >= region.length()) {
                return parent.at(offset - region.length());
            } else {
                return 0xff & region.get(region.length() - offset - 1);
            }
        }
    }
}
