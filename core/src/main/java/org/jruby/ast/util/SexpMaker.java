package org.jruby.ast.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

import org.jruby.ir.IRScope;
import org.jruby.util.ConvertBytes;

public class SexpMaker {
    private static final AtomicLong JITTED_METHOD_NUMBER = new AtomicLong();
    private interface Builder {
        Builder append(String str);
        Builder append(char ch);
        Builder append(int i);
        Builder append(Object o);
        Builder append(boolean b);
        Builder append(long l);
        Builder append(double d);
    }

    private static class DigestBuilder implements Builder {
        MessageDigest d;

        DigestBuilder(MessageDigest digest) {
            this.d = digest;
        }

        @Override
        public Builder append(Object o) {
            append(o.toString());
            return this;
        }

        @Override
        public Builder append(String str) {
            d.update(str.getBytes());
            return this;
        }

        @Override
        public Builder append(boolean b) {
            append((byte) (b ? 1 : 0));
            return this;
        }

        @Override
        public Builder append(char ch) {
            d.update((byte)(ch >> 8));
            d.update((byte)(ch));
            return this;
        }

        @Override
        public Builder append(int i) {
            append((char) (i >> 16));
            append((char) i);
            return this;
        }

        @Override
        public Builder append(long l) {
            append((int) (l >> 32));
            append((int) l);
            return this;
        }

        @Override
        public Builder append(double d) {
            append(Double.doubleToLongBits(d));
            return this;
        }
    }

    public static String sha1(IRScope scope) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }

        DigestBuilder db = new DigestBuilder(sha1);

        db.append(scope.getId());
        db.append('\n');
        // CON FIXME: We need a better way to uniquely identify this...right?
        db.append(JITTED_METHOD_NUMBER.getAndIncrement());

        byte[] digest = db.d.digest();

        return new String(ConvertBytes.twosComplementToHexBytes(digest, false));
    }
}
