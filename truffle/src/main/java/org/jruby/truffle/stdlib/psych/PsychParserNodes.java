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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvResult;
import org.jcodings.transcode.TranscoderDB;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.RubyEncoding;
import org.jruby.runtime.Helpers;
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
import org.jruby.truffle.util.BoundaryUtils.BoundaryIterable;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@CoreClass("Psych::Parser")
public abstract class PsychParserNodes {

    @CoreMethod(names = "parse", required = 1, optional = 1)
    public abstract static class ParseNode extends CoreMethodArrayArgumentsNode {

        @Node.Child private ToStrNode toStrNode;

        public ParseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStrNode = ToStrNodeGen.create(getContext(), null, null);
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
                @Cached("createTaintNode()") TaintNode taintNode,
                @Cached("create()") BranchProfile errorProfile) {

            final boolean tainted = (boolean) taintedNode.execute(frame, "yaml.tainted? || yaml.is_a?(IO)", "yaml", yaml);

            final StreamReader reader;

            if (!RubyGuards.isRubyString(yaml) && respondToReadNode.doesRespondTo(frame, "read", yaml)) {
                reader = newStreamReader(yaml);
            } else {
                ByteList byteList = StringOperations.getByteListReadOnly(toStrNode.executeToStr(frame, yaml));
                reader = newStringReader(byteList);
            }

            final Parser parser = newParser(reader);

            try {
                if (isNil(path) && respondToPathNode.doesRespondTo(frame, "path", yaml)) {
                    path = (DynamicObject) callPathNode.call(frame, yaml, "path");
                }

                final Object handler = readHandlerNode.execute(parserObject);

                while (true) {
                    Event event = getParserEvent(parser);

                    if (isEvent(event, Event.ID.StreamStart)) {
                        callStartStreamNode.call(frame, handler, "start_stream", YAMLEncoding.YAML_ANY_ENCODING.ordinal());

                    } else if (isEvent(event, Event.ID.DocumentStart)) {
                        final DocumentStartEvent startEvent = (DocumentStartEvent) event;

                        final DumperOptions.Version versionOptions = startEvent.getVersion();
                        final Integer[] versionInts = versionOptions == null ? null : versionOptions.getArray();

                        final DynamicObject versionArray;

                        if (versionInts == null) {
                            versionArray = createArray(null, 0);
                        } else {
                            versionArray = createArray(new Object[] {
                                    versionInts[0], versionInts[1]
                            }, 2);
                        }

                        Map<String, String> tagsMap = startEvent.getTags();
                        DynamicObject tags = createArray(null, 0);

                        if (tagsMap != null && size(tagsMap) > 0) {
                            for (Map.Entry<String, String> tag : BoundaryIterable.wrap(entrySet(tagsMap))) {
                                Object key = stringFor(getKey(tag), tainted, taintNode);
                                Object value = stringFor(getValue(tag), tainted, taintNode);
                                tagPushNode.execute(frame,
                                        "tags.push [key, value]",
                                        "tags", tags,
                                        "key", key,
                                        "value", value);
                            }
                        }

                        Object notExplicit = !startEvent.getExplicit();
                        callStartDocumentNode.call(frame, handler, "start_document", versionArray, tags, notExplicit);

                    } else if (isEvent(event, Event.ID.DocumentEnd)) {
                        final DocumentEndEvent endEvent = (DocumentEndEvent) event;
                        Object notExplicit = !endEvent.getExplicit();
                        callEndDocumentNode.call(frame, handler, "end_document", notExplicit);

                    } else if (isEvent(event, Event.ID.Alias)) {
                        final AliasEvent aliasEvent = (AliasEvent) event;
                        Object alias = stringOrNilFor(aliasEvent.getAnchor(), tainted, taintNode);
                        callAliasNode.call(frame, handler, "alias", alias);

                    } else if (isEvent(event, Event.ID.Scalar)) {
                        final ScalarEvent scalarEvent = (ScalarEvent) event;

                        Object anchor = stringOrNilFor(scalarEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(scalarEvent.getTag(), tainted, taintNode);
                        Object plain_implicit = scalarEvent.getImplicit().canOmitTagInPlainScalar();
                        Object quoted_implicit = scalarEvent.getImplicit().canOmitTagInNonPlainScalar();
                        Object style = translateStyle(scalarEvent.getStyle());
                        Object val = stringFor(scalarEvent.getValue(), tainted, taintNode);

                        callScalarNode.call(frame, handler, "scalar", val, anchor, tag, plain_implicit, quoted_implicit, style);

                    } else if (isEvent(event, Event.ID.SequenceStart)) {
                        final SequenceStartEvent sequenceStartEvent = (SequenceStartEvent) event;

                        Object anchor = stringOrNilFor(sequenceStartEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(sequenceStartEvent.getTag(), tainted, taintNode);
                        Object implicit = sequenceStartEvent.getImplicit();
                        Object style = translateFlowStyle(sequenceStartEvent.getFlowStyle());

                        callStartSequenceNode.call(frame, handler, "start_sequence", anchor, tag, implicit, style);

                    } else if (isEvent(event, Event.ID.SequenceEnd)) {
                        callEndSequenceNode.call(frame, handler, "end_sequence");

                    } else if (isEvent(event, Event.ID.MappingStart)) {
                        final MappingStartEvent mappingStartEvent = (MappingStartEvent) event;

                        Object anchor = stringOrNilFor(mappingStartEvent.getAnchor(), tainted, taintNode);
                        Object tag = stringOrNilFor(mappingStartEvent.getTag(), tainted, taintNode);
                        Object implicit = mappingStartEvent.getImplicit();
                        Object style = translateFlowStyle(mappingStartEvent.getFlowStyle());

                        callStartMappingNode.call(frame, handler, "start_mapping", anchor, tag, implicit, style);

                    } else if (isEvent(event, Event.ID.MappingEnd)) {
                        callEndMappingNode.call(frame, handler, "end_mapping");

                    } else if (isEvent(event, Event.ID.StreamEnd)) {
                        callEndStreamNode.call(frame, handler, "end_stream");
                        break;
                    }
                }
            } catch (ParserException | ScannerException pe) {
                errorProfile.enter();
                final Mark mark = pe.getProblemMark();

                raiseSyntaxErrorSnippetNode.execute(frame,
                        "raise Psych::SyntaxError.new(file, line, col, offset, problem, context)",
                        "file", path,
                        "line", mark.getLine(),
                        "col", mark.getColumn(),
                        "offset", mark.getIndex(),
                        "problem", pe.getProblem() == null ? nil() : createUTF8String(pe.getProblem()),
                        "context", pe.getContext() == null ? nil() : createUTF8String(pe.getContext()));
            } catch (ReaderException re) {
                errorProfile.enter();

                raiseSyntaxErrorSnippetNode.execute(frame,
                        "raise Psych::SyntaxError.new(file, line, col, offset, problem, context)",
                        "file", path,
                        "line", 0,
                        "col", 0,
                        "offset", re.getPosition(),
                        "problem", re.getName() == null ? nil() : createUTF8String(re.getName()),
                        "context", toString(re) == null ? nil() : createUTF8String(toString(re)));
            } catch (Throwable t) {
                errorProfile.enter();
                Helpers.throwException(t);
                return parserObject;
            }

            return parserObject;
        }

        @TruffleBoundary
        private StreamReader newStreamReader(DynamicObject yaml) {
            final Encoding enc = UTF8Encoding.INSTANCE;
            final Charset charset = enc.getCharset();
            return new StreamReader(new InputStreamReader(new InputStreamAdapter(getContext(), yaml), charset));
        }

        @TruffleBoundary
        private StreamReader newStringReader(ByteList byteList) {
            Encoding encoding = byteList.getEncoding();

            if (!(encoding instanceof UnicodeEncoding)) {
                byteList = strConvEnc(getContext(), byteList, encoding);

                encoding = UTF8Encoding.INSTANCE;
            }

            return new StreamReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(
                                    byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize()),
                            encoding.getCharset()));
        }

        @TruffleBoundary
        private ParserImpl newParser(StreamReader reader) {
            return new ParserImpl(reader);
        }

        @TruffleBoundary
        private Event getParserEvent(Parser parser) {
            return parser.getEvent();
        }

        @TruffleBoundary
        private boolean isEvent(Event event, ID id) {
            return event.is(id);
        }

        private DynamicObject createUTF8String(String value) {
            return createString(StringOperations.encodeRope(value, UTF8Encoding.INSTANCE));
        }

        @TruffleBoundary
        private int size(Map<String, String> tagsMap) {
            return tagsMap.size();
        }

        @TruffleBoundary
        private Set<Entry<String, String>> entrySet(Map<String, String> tagsMap) {
            return tagsMap.entrySet();
        }

        @TruffleBoundary
        private String getKey(Map.Entry<String, String> tag) {
            return tag.getKey();
        }

        @TruffleBoundary
        private String getValue(Map.Entry<String, String> tag) {
            return tag.getValue();
        }

        @TruffleBoundary
        private String toString(ReaderException re) {
            return re.toString();
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

        @TruffleBoundary
        private Object stringOrNilFor(String value, boolean tainted, TaintNode taintNode) {
            if (value == null) {
                return nil();
            } else {
                return stringFor(value, tainted, taintNode);
            }
        }

        @TruffleBoundary
        private Object stringFor(String value, boolean tainted, TaintNode taintNode) {
            Encoding encoding = getContext().getEncodingManager().getDefaultInternalEncoding();

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

        private ByteList strConvEnc(RubyContext context, ByteList byteList, Encoding encoding) {
            return strConvEnc2(context, byteList, encoding, UTF8Encoding.INSTANCE);
        }

        private static ByteList strConvEnc2(RubyContext context, ByteList value, Encoding fromEncoding, Encoding toEncoding) {
            return strConvEncOpts(context, value, fromEncoding, toEncoding, 0, null);
        }

        private static ByteList strConvEncOpts(RubyContext context, ByteList str, Encoding fromEncoding,
                                                Encoding toEncoding, int ecflags, Object ecopts) {
            if (toEncoding == null) return str;
            if (fromEncoding == null) fromEncoding = str.getEncoding();
            if (fromEncoding == toEncoding) return str;
            if ((toEncoding.isAsciiCompatible() && isAsciiOnly(str)) ||
                    toEncoding == ASCIIEncoding.INSTANCE) {
                if (str.getEncoding() != toEncoding) {
                    str = str.dup();
                    str.setEncoding(toEncoding);
                }
                return str;
            }

            ByteList strByteList = str;
            int len = strByteList.getRealSize();
            ByteList newStr = new ByteList(len);
            int olen = len;

            EConv ec = econvOpenOpts(context, fromEncoding.getName(), toEncoding.getName(), ecflags, ecopts);
            if (ec == null) return str;

            byte[] sbytes = strByteList.getUnsafeBytes();
            Ptr sp = new Ptr(strByteList.getBegin());
            int start = sp.p;

            byte[] destbytes;
            Ptr dp = new Ptr(0);
            EConvResult ret;
            int convertedOutput = 0;

            // these are in the while clause in MRI
            destbytes = newStr.getUnsafeBytes();
            int dest = newStr.begin();
            dp.p = dest + convertedOutput;
            ret = ec.convert(sbytes, sp, start + len, destbytes, dp, dest + olen, 0);

            while (ret == EConvResult.DestinationBufferFull) {
                int convertedInput = sp.p - start;
                int rest = len - convertedInput;
                convertedOutput = dp.p - dest;
                newStr.setRealSize(convertedOutput);
                if (convertedInput != 0 && convertedOutput != 0 &&
                        rest < (Integer.MAX_VALUE / convertedOutput)) {
                    rest = (rest * convertedOutput) / convertedInput;
                } else {
                    rest = olen;
                }
                olen += rest < 2 ? 2 : rest;
                newStr.ensure(olen);

                // these are the while clause in MRI
                destbytes = newStr.getUnsafeBytes();
                dest = newStr.begin();
                dp.p = dest + convertedOutput;
                ret = ec.convert(sbytes, sp, start + len, destbytes, dp, dest + olen, 0);
            }
            ec.close();

            switch (ret) {
                case Finished:
                    len = dp.p;
                    newStr.setRealSize(len);
                    newStr.setEncoding(toEncoding);
                    return newStr;

                default:
                    // some error, return original
                    return str;
            }
        }

        private static EConv econvOpenOpts(RubyContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, Object opthash) {
            EConv ec = TranscoderDB.open(sourceEncoding, destinationEncoding, ecflags);
            return ec;
        }

        private static boolean isAsciiOnly(ByteList string) {
            return string.getEncoding().isAsciiCompatible() && scanForCodeRange(string) == StringSupport.CR_7BIT;
        }

        private static int scanForCodeRange(ByteList string) {
            return StringSupport.codeRangeScan(EncodingUtils.getActualEncoding(string.getEncoding(), string), string);
        }

    }

}
