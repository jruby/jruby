/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.ext;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

@CoreClass(name = "Truffle::Digest")
public abstract class DigestNodes {

    private static final HiddenKey DIGEST_IDENTIFIER = new HiddenKey("digest");
    private static final Property DIGEST_PROPERTY;
    private static final DynamicObjectFactory DIGEST_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        DIGEST_PROPERTY = Property.create(DIGEST_IDENTIFIER, allocator.locationForType(MessageDigest.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)), 0);
        DIGEST_FACTORY = RubyBasicObject.EMPTY_SHAPE.addProperty(DIGEST_PROPERTY).createFactory();
    }

    private enum Algorithm {
        MD5
    }

    private static RubyBasicObject createDigest(RubyContext context, Algorithm algorithm) {
        final MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(algorithm.name());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return new RubyBasicObject(context.getCoreLibrary().getObjectClass(), DIGEST_FACTORY.newInstance(digest));
    }

    public static MessageDigest getDigest(RubyBasicObject digest) {
        assert digest.getDynamicObject().getShape().hasProperty(DIGEST_IDENTIFIER);
        return (MessageDigest) DIGEST_PROPERTY.get(digest.getDynamicObject(), true);
    }

    @CoreMethod(names = "md5", isModuleFunction = true)
    public abstract static class MD5Node extends CoreMethodArrayArgumentsNode {

        public MD5Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject md5() {
            return createDigest(getContext(), Algorithm.MD5);
        }

    }

    @CoreMethod(names = "update", isModuleFunction = true, required = 2)
    public abstract static class UpdateNode extends CoreMethodArrayArgumentsNode {

        public UpdateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject update(RubyBasicObject digestObject, RubyString message) {
            final ByteList bytes = message.getByteList();
            getDigest(digestObject).update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            return digestObject;
        }

    }

    @CoreMethod(names = "reset", isModuleFunction = true, required = 1)
    public abstract static class ResetNode extends CoreMethodArrayArgumentsNode {

        public ResetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject reset(RubyBasicObject digestObject) {
            getDigest(digestObject).reset();
            return digestObject;
        }

    }

    @CoreMethod(names = "digest", isModuleFunction = true, required = 1)
    public abstract static class DigestNode extends CoreMethodArrayArgumentsNode {

        public DigestNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString digest(RubyBasicObject digestObject) {
            return getContext().makeString(getDigest(digestObject).digest());
        }

    }

}
