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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.RubyEncoding;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.adapaters.InputStreamAdapter;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.ReaderException;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@CoreClass("Psych::Parser")
public abstract class PsychParserNodes {

    @CoreMethod(names = "parse", required = 1, optional = 1)
    public abstract static class ParseNode extends CoreMethodArrayArgumentsNode {

        @Node.Child private ToStrNode toStrNode;

        public ParseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStrNode = ToStrNodeGen.create(getContext(), getSourceSection(), null);
        }

        public abstract Object executeParse(VirtualFrame frame, DynamicObject parserObject, DynamicObject yaml, Object path);

        @Specialization
        public Object parse(VirtualFrame frame, DynamicObject parserObject, DynamicObject yaml, NotProvided path) {
            return executeParse(frame, parserObject, yaml, nil());
        }

        @Specialization
        public Object parse(
                VirtualFrame frame,
                DynamicObject parserObject,
                DynamicObject yaml,
                DynamicObject path,
                @Cached("new()") SnippetNode taintedNode,
                @Cached("create()") DoesRespondDispatchHeadNode respondToReadNode,
                @Cached("create()") DoesRespondDispatchHeadNode respondToPathNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callPathNode,
                @Cached("createReadHandlerNode()") ReadObjectFieldNode readHandlerNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callStartStreamNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callStartDocumentNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callEndDocumentNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callAliasNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callScalarNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callStartSequenceNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callEndSequenceNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callStartMappingNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callEndMappingNode,
                @Cached("createMethodCall()") CallDispatchHeadNode callEndStreamNode,
                @Cached("new()") SnippetNode raiseSyntaxErrorSnippetNode,
                @Cached("new()") SnippetNode tagPushNode,
                @Cached("createTaintNode()") TaintNode taintNode) {
            CompilerDirectives.bailout("Psych parsing cannot be compiled");

            final boolean tainted = (boolean) taintedNode.execute(frame, "yaml.tainted? || yaml.is_a?(IO)", "yaml", yaml);

            final StreamReader reader;

            if (!RubyGuards.isRubyString(yaml) && respondToReadNode.doesRespondTo(frame, "read", yaml)) {
                final Encoding enc = UTF8Encoding.INSTANCE;
                final Charset charset = enc.getCharset();
                reader = new StreamReader(new InputStreamReader(new InputStreamAdapter(getContext(), yaml), charset));
            } else {
                ByteList byteList = StringOperations.getByteListReadOnly(toStrNode.executeToStr(frame, yaml));
                Encoding encoding = byteList.getEncoding();

                if (!(encoding instanceof UnicodeEncoding)) {
                    byteList = EncodingUtils.strConvEnc(getContext().getJRubyRuntime().getCurrentContext(),
                            byteList, encoding, UTF8Encoding.INSTANCE);

                    encoding = UTF8Encoding.INSTANCE;
                }

                reader = new StreamReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(
                                        byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize()),
                                encoding.getCharset()));
            }

            final Parser parser = new ParserImpl(reader);

            try {
                if (isNil(path) && respondToPathNode.doesRespondTo(frame, "path", yaml)) {
                    path = (DynamicObject) callPathNode.call(frame, yaml, "path");
                }

                final Object handler = readHandlerNode.execute(parserObject);

                while (true) {
                    Event event = parser.getEvent();

                    if (event.is(Event.ID.StreamStart)) {
                        callStartStreamNode.call(frame, handler, "start_stream", YAMLEncoding.YAML_ANY_ENCODING.ordinal());
                    } else if (event.is(Event.ID.DocumentStart)) {
                        final DocumentStartEvent startEvent = (DocumentStartEvent) event;

                        final DumperOptions.Version versionOptions = startEvent.getVersion();
                        final Integer[] versionInts = versionOptions == null ? null : versionOptions.getArray();

                        final DynamicObject versionArray;

                        if (versionInts == null) {
                            versionArray = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
                        } else {
                            versionArray = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{
                                    versionInts[0], versionInts[1]
                            }, 2);
                        }

                        Map<String, String> tagsMap = startEvent.getTags();
                        DynamicObject tags = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);

                        if (tagsMap != null && tagsMap.size() > 0) {
                            for (Map.Entry<String, String> tag : tagsMap.entrySet()) {
                                Object key = stringFor(tag.getKey(), tainted, taintNode);
                                Object value = stringFor(tag.getValue(), tainted, taintNode);
                                tagPushNode.execute(frame,
                                        "tags.push [key, value]",
                                        "tags", tags,
                                        "key", key,
                                        "value", value);
                            }
                        }

                        Object notExplicit = !startEvent.getExplicit();
                        callStartDocumentNode.call(frame, handler, "start_document", versionArray, tags, notExplicit);
                    } else if (event.is(Event.ID.DocumentEnd)) {
                        final DocumentEndEvent endEvent = (DocumentEndEvent) event;
                        Object notExplicit = !endEvent.getExplicit();
                        callEndDocumentNode.call(frame, handler, "end_document", notExplicit);
                    } else if (event.is(Event.ID.Alias)) {
                        final AliasEvent aliasEvent = (AliasEvent) event;
                        Object alias = stringOrNilFor(aliasEvent.getAnchor(), tainted, taintNode);
                        callAliasNode.call(frame, handler, "alias", alias);
                    } else if (event.is(Event.ID.Scalar)) {
                        final ScalarEvent scalarEvent = (ScalarEvent) event;

                        Object anchor = stringOrNilFor(scalarEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(scalarEvent.getTag(), tainted, taintNode);
                        Object plain_implicit = scalarEvent.getImplicit().canOmitTagInPlainScalar();
                        Object quoted_implicit = scalarEvent.getImplicit().canOmitTagInNonPlainScalar();
                        Object style = translateStyle(scalarEvent.getStyle());
                        Object val = stringFor(scalarEvent.getValue(), tainted, taintNode);

                        callScalarNode.call(frame, handler, "scalar", val, anchor, tag, plain_implicit, quoted_implicit,
                                style);
                    } else if (event.is(Event.ID.SequenceStart)) {
                        final SequenceStartEvent sequenceStartEvent = (SequenceStartEvent) event;

                        Object anchor = stringOrNilFor(sequenceStartEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(sequenceStartEvent.getTag(), tainted, taintNode);
                        Object implicit = sequenceStartEvent.getImplicit();
                        Object style = translateFlowStyle(sequenceStartEvent.getFlowStyle());

                        callStartSequenceNode.call(frame, handler, "start_sequence", anchor, tag, implicit, style);
                    } else if (event.is(Event.ID.SequenceEnd)) {
                        callEndSequenceNode.call(frame, handler, "end_sequence");
                    } else if (event.is(Event.ID.MappingStart)) {
                        final MappingStartEvent mappingStartEvent = (MappingStartEvent) event;

                        Object anchor = stringOrNilFor(mappingStartEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(mappingStartEvent.getTag(), tainted, taintNode);
                        Object implicit = mappingStartEvent.getImplicit();
                        Object style = translateFlowStyle(mappingStartEvent.getFlowStyle());

                        callStartMappingNode.call(frame, handler, "start_mapping", anchor, tag, implicit, style);
                    } else if (event.is(Event.ID.MappingEnd)) {
                        callEndMappingNode.call(frame, handler, "end_mapping");
                    } else if (event.is(Event.ID.StreamEnd)) {
                        callEndStreamNode.call(frame, handler, "end_stream");
                        break;
                    }
                }
            } catch (ParserException pe) {
                final Mark mark = pe.getProblemMark();

                raiseSyntaxErrorSnippetNode.execute(frame,
                        "raise Psych::SyntaxError.new(file, line, col, offset, problem, context)",
                        "file", path,
                        "line", mark.getLine(),
                        "col", mark.getColumn(),
                        "offset", mark.getIndex(),
                        "problem", pe.getProblem() == null ? nil() : createString(new ByteList(pe.getProblem().getBytes(StandardCharsets.UTF_8))),
                        "context", pe.getContext() == null ? nil() : createString(new ByteList(pe.getContext().getBytes(StandardCharsets.UTF_8))));
            } catch (ScannerException se) {
                final Mark mark = se.getProblemMark();

                raiseSyntaxErrorSnippetNode.execute(frame,
                        "raise Psych::SyntaxError.new(file, line, col, offset, problem, context)",
                        "file", path,
                        "line", mark.getLine(),
                        "col", mark.getColumn(),
                        "offset", mark.getIndex(),
                        "problem", se.getProblem() == null ? nil() : createString(new ByteList(se.getProblem().getBytes(StandardCharsets.UTF_8))),
                        "context", se.getContext() == null ? nil() : createString(new ByteList(se.getContext().getBytes(StandardCharsets.UTF_8))));
            } catch (ReaderException re) {
                raiseSyntaxErrorSnippetNode.execute(frame,
                        "raise Psych::SyntaxError.new(file, line, col, offset, problem, context)",
                        "file", path,
                        "line", 0,
                        "col", 0,
                        "offset", re.getPosition(),
                        "problem", re.getName() == null ? nil() : createString(new ByteList(re.getName().getBytes(StandardCharsets.UTF_8))),
                        "context", re.toString() == null ? nil() : createString(new ByteList(re.toString().getBytes(StandardCharsets.UTF_8))));
            } catch (Throwable t) {
                Helpers.throwException(t);
                return parserObject;
            }

            return parserObject;
        }

        protected ReadObjectFieldNode createReadHandlerNode() {
            return ReadObjectFieldNodeGen.create("@handler", nil());
        }

        protected TaintNode createTaintNode() {
            return TaintNodeGen.create(getContext(), null, null);
        }

        private static final int STYLE_PLAIN = 1;
        private static final int STYLE_SINGLE_QUOTED = 2;
        private static final int STYLE_DOUBLE_QUOTED = 3;
        private static final int STYLE_LITERAL = 4;
        private static final int STYLE_FOLDED = 5;
        private static final int STYLE_ANY = 0;
        private static final int STYLE_FLOW = 2;
        private static final int STYLE_NOT_FLOW = 1;

        private static int translateStyle(Character style) {
            switch (style) {
                case 0:
                    return STYLE_PLAIN;
                case '\'':
                    return STYLE_SINGLE_QUOTED;
                case '"':
                    return STYLE_DOUBLE_QUOTED;
                case '|':
                    return STYLE_LITERAL;
                case '>':
                    return STYLE_FOLDED;
                default:
                    return STYLE_ANY;
            }
        }

        private static int translateFlowStyle(Boolean flowStyle) {
            if (flowStyle == null) {
                return STYLE_ANY;
            } else if (flowStyle) {
                return STYLE_FLOW;
            } else {
                return STYLE_NOT_FLOW;
            }
        }

        private Object stringOrNilFor(String value, boolean tainted, TaintNode taintNode) {
            if (value == null) {
                return nil();
            } else {
                return stringFor(value, tainted, taintNode);
            }
        }

        private Object stringFor(String value, boolean tainted, TaintNode taintNode) {
            Encoding encoding = getContext().getJRubyRuntime().getDefaultInternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
            }

            Charset charset = RubyEncoding.UTF8;

            if (encoding.getCharset() != null) {
                charset = encoding.getCharset();
            }

            final Object string = createString(value.getBytes(charset), encoding);

            if (tainted) {
                taintNode.executeTaint(string);
            }

            return string;
        }

    }

}
