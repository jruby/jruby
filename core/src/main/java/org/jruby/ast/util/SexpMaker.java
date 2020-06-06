package org.jruby.ast.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jruby.ir.IRScope;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;

public class SexpMaker {

    private static class DigestBuilder {
        final MessageDigest digest;

        DigestBuilder(MessageDigest digest) {
            this.digest = digest;
        }

        public DigestBuilder append(Object o) {
            append(o.toString());
            return this;
        }

        public DigestBuilder append(ByteList str) {
            digest.update(str.unsafeBytes(), str.getBegin(), str.getRealSize());
            return this;
        }

        public DigestBuilder append(String str) {
            digest.update(str.getBytes());
            return this;
        }

        public DigestBuilder append(boolean b) {
            append((byte) (b ? 1 : 0));
            return this;
        }

        public DigestBuilder append(char ch) {
            digest.update((byte)(ch >> 8));
            digest.update((byte)(ch));
            return this;
        }

        public DigestBuilder append(int i) {
            append((char) (i >> 16));
            append((char) i);
            return this;
        }

        public DigestBuilder append(long l) {
            append((int) (l >> 32));
            append((int) l);
            return this;
        }

        public DigestBuilder append(double d) {
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

        DigestBuilder builder = new DigestBuilder(sha1);
        builder.append(scope.getId()).append('\n').append(scope.getScopeId());

        byte[] digest = builder.digest.digest();

        return new String(ConvertBytes.twosComplementToHexBytes(digest, false));
    }
}
