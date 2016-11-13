/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.digest;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.ext.digest.BubbleBabble;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.JavaException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@CoreClass("Truffle::Digest")
public abstract class DigestNodes {

    @TruffleBoundary
    private static MessageDigest getMessageDigestInstance(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new JavaException(e);
        }
    }

    private static DynamicObject createDigest(RubyContext context, DigestAlgorithm algorithm) {
        return Layouts.DIGEST.createDigest(
                context.getCoreLibrary().getDigestFactory(),
                algorithm,
                getMessageDigestInstance(algorithm.getName()));
    }

    @CoreMethod(names = "md5", onSingleton = true)
    public abstract static class MD5Node extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject md5() {
            return createDigest(getContext(), DigestAlgorithm.MD5);
        }

    }

    @CoreMethod(names = "sha1", onSingleton = true)
    public abstract static class SHA1Node extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject sha1() {
            return createDigest(getContext(), DigestAlgorithm.SHA1);
        }

    }

    @CoreMethod(names = "sha256", onSingleton = true)
    public abstract static class SHA256Node extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject sha256() {
            return createDigest(getContext(), DigestAlgorithm.SHA256);
        }

    }

    @CoreMethod(names = "sha384", onSingleton = true)
    public abstract static class SHA384Node extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject sha384() {
            return createDigest(getContext(), DigestAlgorithm.SHA384);
        }

    }

    @CoreMethod(names = "sha512", onSingleton = true)
    public abstract static class SHA512Node extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject sha512() {
            return createDigest(getContext(), DigestAlgorithm.SHA512);
        }

    }

    @CoreMethod(names = "update", onSingleton = true, required = 2)
    public abstract static class UpdateNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(message)")
        public DynamicObject update(DynamicObject digestObject, DynamicObject message) {
            final MessageDigest digest = Layouts.DIGEST.getDigest(digestObject);

            RopeOperations.visitBytes(StringOperations.rope(message), (bytes, offset, length) -> digest.update(bytes, offset, length));

            return digestObject;
        }

    }

    @CoreMethod(names = "reset", onSingleton = true, required = 1)
    public abstract static class ResetNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject reset(DynamicObject digestObject) {
            Layouts.DIGEST.getDigest(digestObject).reset();
            return digestObject;
        }

    }

    @CoreMethod(names = "digest", onSingleton = true, required = 1)
    public abstract static class DigestNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject digest(DynamicObject digestObject) {
            final MessageDigest digest = Layouts.DIGEST.getDigest(digestObject);

            return createString(RopeOperations.create(
                    cloneAndDigest(digest), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID));
        }

        @TruffleBoundary
        private static byte[] cloneAndDigest(MessageDigest digest) {
            final MessageDigest clonedDigest;

            try {
                clonedDigest = (MessageDigest) digest.clone();
            } catch (CloneNotSupportedException e) {
                throw new JavaException(e);
            }

            return clonedDigest.digest();
        }

    }

    @CoreMethod(names = "digest_length", onSingleton = true, required = 1)
    public abstract static class DigestLengthNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int digestLength(DynamicObject digestObject) {
            return Layouts.DIGEST.getAlgorithm(digestObject).getLength();
        }

    }

    @CoreMethod(names = "bubblebabble", onSingleton = true, required = 1)
    public abstract static class BubbleBabbleNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public DynamicObject bubblebabble(DynamicObject message) {
            final Rope rope = StringOperations.rope(message);
            return createString(BubbleBabble.bubblebabble(rope.getBytes(), 0, rope.byteLength()));
        }

    }

}
