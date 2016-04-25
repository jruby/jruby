/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.ext.digest.BubbleBabble;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.BytesVisitor;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@CoreClass(name = "Truffle::Digest")
public abstract class DigestNodes {

    private enum Algorithm {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA384("SHA-384"),
        SHA512("SHA-512");

        private final String name;

        Algorithm(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static DynamicObject createDigest(RubyContext context, Algorithm algorithm) {
        final MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(algorithm.getName());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        final DynamicObject rubyClass = context.getCoreLibrary().getDigestClass();

        return DigestLayoutImpl.INSTANCE.createDigest(Layouts.CLASS.getInstanceFactory(rubyClass), digest);
    }

    @CoreMethod(names = "md5", onSingleton = true)
    public abstract static class MD5Node extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject md5() {
            return createDigest(getContext(), Algorithm.MD5);
        }

    }

    @CoreMethod(names = "sha1", onSingleton = true)
    public abstract static class SHA1Node extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sha1() {
            return createDigest(getContext(), Algorithm.SHA1);
        }

    }

    @CoreMethod(names = "sha256", onSingleton = true)
    public abstract static class SHA256Node extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sha256() {
            return createDigest(getContext(), Algorithm.SHA256);
        }

    }

    @CoreMethod(names = "sha384", onSingleton = true)
    public abstract static class SHA384Node extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sha384() {
            return createDigest(getContext(), Algorithm.SHA384);
        }

    }

    @CoreMethod(names = "sha512", onSingleton = true)
    public abstract static class SHA512Node extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sha512() {
            return createDigest(getContext(), Algorithm.SHA512);
        }

    }

    @CoreMethod(names = "update", onSingleton = true, required = 2)
    public abstract static class UpdateNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public DynamicObject update(DynamicObject digestObject, DynamicObject message) {
            final MessageDigest digest = DigestLayoutImpl.INSTANCE.getDigest(digestObject);

            RopeOperations.visitBytes(StringOperations.rope(message), new BytesVisitor() {

                @Override
                public void accept(byte[] bytes, int offset, int length) {
                    digest.update(bytes, offset, length);
                }

            });

            return digestObject;
        }

    }

    @CoreMethod(names = "reset", onSingleton = true, required = 1)
    public abstract static class ResetNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject reset(DynamicObject digestObject) {
            DigestLayoutImpl.INSTANCE.getDigest(digestObject).reset();
            return digestObject;
        }

    }

    @CoreMethod(names = "digest", onSingleton = true, required = 1)
    public abstract static class DigestNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject digest(DynamicObject digestObject) {
            final MessageDigest digest = DigestLayoutImpl.INSTANCE.getDigest(digestObject);

            // TODO CS 18-May-15 this cloning isn't ideal for the key operation

            final MessageDigest clonedDigest;

            try {
                clonedDigest = (MessageDigest) digest.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }

            return createString(RopeOperations.create(
                    clonedDigest.digest(), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID));
        }

    }

    @CoreMethod(names = "digest_length", onSingleton = true, required = 1)
    public abstract static class DigestLengthNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int digestLength(DynamicObject digestObject) {
            return DigestLayoutImpl.INSTANCE.getDigest(digestObject).getDigestLength();
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
