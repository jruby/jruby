/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This code is modified from the Psych JRuby extension module
 * implementation with the following header:
 *
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.stdlib.psych;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.adapaters.OutputStreamAdapter;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.emitter.EmitterException;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.events.StreamEndEvent;
import org.yaml.snakeyaml.events.StreamStartEvent;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@CoreClass(name = "Psych::Emitter")
public abstract class PsychEmitterNodes {

    @CoreMethod(unsafeNeedsAudit = true, names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, null, null);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "initialize", visibility = Visibility.PRIVATE, required = 1, optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject initialize(DynamicObject emitter, DynamicObject io, NotProvided optionsSet) {
            final DumperOptions options = new DumperOptions();
            options.setIndent(2);
            Layouts.PSYCH_EMITTER.setOptions(emitter, options);

            Layouts.PSYCH_EMITTER.setIo(emitter, io);

            return nil();
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject initialize(DynamicObject emitter, DynamicObject io, DynamicObject optionsSet) {
            final DumperOptions options = new DumperOptions();
            options.setWidth((int) ruby("options_set.line_width", "options_set", optionsSet));
            options.setCanonical((boolean) ruby("options_set.canonical", "options_set", optionsSet));
            options.setIndent((int) ruby("options_set.indentation", "options_set", optionsSet));
            Layouts.PSYCH_EMITTER.setOptions(emitter, options);

            Layouts.PSYCH_EMITTER.setIo(emitter, io);

            return nil();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "start_stream", required = 1)
    public abstract static class StartStreamNode extends CoreMethodArrayArgumentsNode {

        public StartStreamNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject startStream(DynamicObject emitter, int encoding) {
            initEmitter(getContext(), emitter, encoding);

            StreamStartEvent event = new StreamStartEvent(NULL_MARK, NULL_MARK);

            emit(getContext(), emitter, event);

            return emitter;
        }

    }


    @CoreMethod(unsafeNeedsAudit = true, names = "end_stream")
    public abstract static class EndStreamNode extends CoreMethodArrayArgumentsNode {

        public EndStreamNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject endStream(DynamicObject emitter) {
            emit(getContext(), emitter, new StreamEndEvent(NULL_MARK, NULL_MARK));
            return emitter;
        }

    }


    @CoreMethod(unsafeNeedsAudit = true, names = "start_document", required = 3)
    public abstract static class StartDocumentNode extends CoreMethodArrayArgumentsNode {

        public StartDocumentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyArray(_version)", "isRubyArray(tags)"})
        public DynamicObject startDocument(DynamicObject emitter, DynamicObject _version, DynamicObject tags, boolean implicit) {
            DumperOptions.Version version = null;
            boolean implicitBool = implicit;
            Map<String, String> tagsMap = null;

            Object[] versionAry = ArrayOperations.toObjectArray(_version);
            if (versionAry.length == 2) {
                int versionInt0 = (int)versionAry[0];
                int versionInt1 = (int)versionAry[1];

                if (versionInt0 == 1) {
                    if (versionInt1 == 0) {
                        version = DumperOptions.Version.V1_0;
                    } else if (versionInt1 == 1) {
                        version = DumperOptions.Version.V1_1;
                    }
                }
                if (version == null) {
                    // TODO CS 28-Sep-15 implement this code path
                    throw new UnsupportedOperationException();
                    //throw context.runtime.newArgumentError("invalid YAML version: " + versionAry);
                }
            }

            Object[] tagsAry = ArrayOperations.toObjectArray(tags);
            if (tagsAry.length > 0) {
                tagsMap = new HashMap<String, String>(tagsAry.length);
                for (int i = 0; i < tagsAry.length; i++) {
                    Object[] tagsTuple = ArrayOperations.toObjectArray((DynamicObject) tagsAry[i]);
                    if (tagsTuple.length != 2) {
                        // TODO CS 28-Sep-15 implement this code path
                        throw new UnsupportedOperationException();
                        //throw context.runtime.newRuntimeError("tags tuple must be of length 2");
                    }
                    Object key = tagsTuple[0];
                    Object value = tagsTuple[1];
                    tagsMap.put(
                            key.toString(),
                            value.toString());
                }
            }

            DocumentStartEvent event = new DocumentStartEvent(NULL_MARK, NULL_MARK, !implicitBool, version, tagsMap);
            emit(getContext(), emitter, event);
            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "end_document", required = 1)
    public abstract static class EndDocumentNode extends CoreMethodArrayArgumentsNode {

        public EndDocumentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject endDocument(DynamicObject emitter, boolean implicit) {
            emit(getContext(), emitter, new DocumentEndEvent(NULL_MARK, NULL_MARK, !implicit));
            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "scalar", required = 6)
    public abstract static class ScalarNode extends CoreMethodArrayArgumentsNode {

        public ScalarNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(value)")
        public DynamicObject scalar(DynamicObject emitter, DynamicObject value, Object anchor, Object tag, boolean plain, boolean quoted, int style) {
            ScalarEvent event = new ScalarEvent(
                    isNil(anchor) ? null : anchor.toString(),
                    isNil(tag) ? null : tag.toString(),
                    new ImplicitTuple(plain,
                            quoted),
                    value.toString(),
                    NULL_MARK,
                    NULL_MARK,
                    SCALAR_STYLES[style]);
            emit(getContext(), emitter, event);
            return emitter;
        }

    }


    @CoreMethod(unsafeNeedsAudit = true, names = "start_sequence", required = 4)
    public abstract static class StartSequenceNode extends CoreMethodArrayArgumentsNode {

        public StartSequenceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject startSequence(DynamicObject emitter, Object anchor, Object tag, boolean implicit, int style) {
            final int SEQUENCE_BLOCK = 1; // see psych/nodes/sequence.rb

            SequenceStartEvent event = new SequenceStartEvent(
                    isNil(anchor) ? null : anchor.toString(),
                    isNil(tag) ? null : tag.toString(),
                    implicit,
                    NULL_MARK,
                    NULL_MARK,
                    SEQUENCE_BLOCK != style);
            emit(getContext(), emitter, event);

            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "end_sequence")
    public abstract static class EndSequenceNode extends CoreMethodArrayArgumentsNode {

        public EndSequenceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject endSequence(DynamicObject emitter) {
            emit(getContext(), emitter, new SequenceEndEvent(NULL_MARK, NULL_MARK));
            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "start_mapping", required = 4)
    public abstract static class StartMappingNode extends CoreMethodArrayArgumentsNode {

        public StartMappingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject startMapping(DynamicObject emitter, Object anchor, Object tag, boolean implicit, int style) {
            final int MAPPING_BLOCK = 1; // see psych/nodes/mapping.rb

            MappingStartEvent event = new MappingStartEvent(
                    isNil(anchor) ? null : anchor.toString(),
                    isNil(tag) ? null : tag.toString(),
                    implicit,
                    NULL_MARK,
                    NULL_MARK,
                    MAPPING_BLOCK != style);

            emit(getContext(), emitter, event);

            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "end_mapping")
    public abstract static class EndMappingNode extends CoreMethodArrayArgumentsNode {

        public EndMappingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject endMapping(DynamicObject emitter) {
            emit(getContext(), emitter, new MappingEndEvent(NULL_MARK, NULL_MARK));
            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "alias", required = 1)
    public abstract static class AliasNode extends CoreMethodArrayArgumentsNode {

        public AliasNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject alias(DynamicObject emitter, Object anchor) {
            emit(getContext(), emitter, new AliasEvent(anchor.toString(), NULL_MARK, NULL_MARK));
            return emitter;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "canonical=", required = 1)
    public abstract static class SetCanonicalNode extends CoreMethodArrayArgumentsNode {

        public SetCanonicalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean setCanonical(DynamicObject emitter, boolean canonical) {
            Layouts.PSYCH_EMITTER.getOptions(emitter).setCanonical(canonical);
            return canonical;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "canonical")
    public abstract static class CanonicalNode extends CoreMethodArrayArgumentsNode {

        public CanonicalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean canonical(DynamicObject emitter) {
            return Layouts.PSYCH_EMITTER.getOptions(emitter).isCanonical();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "indentation=", required = 1)
    public abstract static class SetIndentationNode extends CoreMethodArrayArgumentsNode {

        public SetIndentationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int setIndentation(DynamicObject emitter, int indentation) {
            Layouts.PSYCH_EMITTER.getOptions(emitter).setIndent(indentation);
            return indentation;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "indentation")
    public abstract static class IndentationNode extends CoreMethodArrayArgumentsNode {

        public IndentationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int indentation(DynamicObject emitter) {
            return Layouts.PSYCH_EMITTER.getOptions(emitter).getIndent();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "line_width=", required = 1)
    public abstract static class SetLineWidthNode extends CoreMethodArrayArgumentsNode {

        public SetLineWidthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int setLineWidth(DynamicObject emitter, int width) {
            Layouts.PSYCH_EMITTER.getOptions(emitter).setWidth(width);
            return width;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "line_width")
    public abstract static class LineWidthNode extends CoreMethodArrayArgumentsNode {

        public LineWidthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int lineWidth(DynamicObject emitter) {
            return Layouts.PSYCH_EMITTER.getOptions(emitter).getWidth();
        }

    }

    private static void emit(RubyContext context, DynamicObject emitter, Event event) {
        try {
            if (Layouts.PSYCH_EMITTER.getEmitter(emitter) == null) {
                throw new UnsupportedOperationException();
                // TODO CS 28-Sep-15 implement this code path
                //throw context.getRuntime().newRuntimeError("uninitialized emitter");
            }

            Layouts.PSYCH_EMITTER.getEmitter(emitter).emit(event);
        } catch (IOException ioe) {
            throw new UnsupportedOperationException();
            // TODO CS 28-Sep-15 implement this code path
            //throw context.runtime.newIOErrorFromException(ioe);
        } catch (EmitterException ee) {
            throw new UnsupportedOperationException();
            // TODO CS 28-Sep-15 implement this code path
            //throw context.runtime.newRuntimeError(ee.toString());
        }
    }

    private static void initEmitter(RubyContext context, DynamicObject emitter, int _encoding) {
        if (Layouts.PSYCH_EMITTER.getEmitter(emitter) != null) {
            throw new UnsupportedOperationException();
            // TODO CS 28-Sep-15 implement this code path
            //throw context.runtime.newRuntimeError("already initialized emitter");
        }

        Encoding encoding = PsychParserNodes.YAMLEncoding.values()[_encoding].encoding;
        // TODO CS 24-Sep-15 uses JRuby's encoding service
        Charset charset = context.getJRubyRuntime().getEncodingService().charsetForEncoding(encoding);

        Layouts.PSYCH_EMITTER.setEmitter(emitter, new Emitter(new OutputStreamWriter(
                new OutputStreamAdapter(context, (DynamicObject) Layouts.PSYCH_EMITTER.getIo(emitter), encoding), charset),
                Layouts.PSYCH_EMITTER.getOptions(emitter)));
    }

    private static final Mark NULL_MARK = new Mark(null, 0, 0, 0, null, 0);

    // Map style constants from Psych values (ANY = 0 ... FOLDED = 5)
    // to SnakeYaml values; see psych/nodes/scalar.rb.
    private static final Character[] SCALAR_STYLES = new Character[] {
            null, // ANY; we'll choose plain
            null, // PLAIN
            '\'', // SINGLE_QUOTED
            '"',  // DOUBLE_QUOTED
            '|',  // LITERAL
            '>',  // FOLDED
    };

}
