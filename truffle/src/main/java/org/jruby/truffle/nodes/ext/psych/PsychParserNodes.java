/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of this code is modifed from the Psych JRuby extension module
 * implementation with the following header:
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
package org.jruby.truffle.nodes.ext.psych;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.RubyEncoding;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.events.*;
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

@CoreClass(name = "Truffle::Psych::Parser")
public abstract class PsychParserNodes {

    public static final int ANY_ENCODING = 0;
    public static final int UTF8_ENCODING = 1;
    public static final int UTF16LE_ENCODING = 2;
    public static final int UTF16BE_ENCODING = 3;

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, null);
        }

    }

    @CoreMethod(names = "parse", required = 1, optional = 1)
    public abstract static class ParseNode extends CoreMethodArrayArgumentsNode {

        public ParseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(yaml)")
        public Object parse(DynamicObject parserObject, DynamicObject yaml, NotProvided path) {
            return doParse(parserObject, yaml, nil());
        }

        @Specialization(guards = {"isRubyString(yaml)", "isNil(path)"})
        public Object parseNil(DynamicObject parserObject, DynamicObject yaml, DynamicObject path) {
            return doParse(parserObject, yaml, nil());
        }

        @Specialization(guards = {"isRubyString(yaml)", "isRubyString(path)"})
        public Object parse(DynamicObject parserObject, DynamicObject yaml, DynamicObject path) {
            return doParse(parserObject, yaml, path);
        }

        @CompilerDirectives.TruffleBoundary
        private Object doParse(DynamicObject parserObject, DynamicObject yaml, DynamicObject path) {
            boolean tainted = (boolean) ruby("yaml.tainted? || yaml.is_a?(IO)", "yaml", yaml);

            Parser parser = null;

            try {
                parser = new ParserImpl(readerFor(yaml));
                Layouts.PSYCH_PARSER_LAYOUT.setParser(parserObject, parser);

                if (isNil(path) && (boolean) ruby("yaml.respond_to? :path", "yaml", yaml)) {
                    path = (DynamicObject) ruby("yaml.path", "yaml", yaml);
                }

                Object handler = getInstanceVariable("@handler");

                while (true) {
                    Event event = parser.getEvent();
                    Layouts.PSYCH_PARSER_LAYOUT.setEvent(parserObject, event);

                    // FIXME: Event should expose a getID, so it can be switched
                    if (event.is(Event.ID.StreamStart)) {
                        invoke(handler, "start_stream", ANY_ENCODING);
                    } else if (event.is(Event.ID.DocumentStart)) {
                        handleDocumentStart((DocumentStartEvent) event, tainted, handler);
                    } else if (event.is(Event.ID.DocumentEnd)) {
                        Object notExplicit = !((DocumentEndEvent) event).getExplicit();
                        invoke(handler, "end_document", notExplicit);
                    } else if (event.is(Event.ID.Alias)) {
                        Object alias = stringOrNilFor(((AliasEvent)event).getAnchor(), tainted);
                        invoke(handler, "alias", alias);
                    } else if (event.is(Event.ID.Scalar)) {
                        handleScalar((ScalarEvent) event, tainted, handler);
                    } else if (event.is(Event.ID.SequenceStart)) {
                        handleSequenceStart((SequenceStartEvent) event, tainted, handler);
                    } else if (event.is(Event.ID.SequenceEnd)) {
                        invoke(handler, "end_sequence");
                    } else if (event.is(Event.ID.MappingStart)) {
                        handleMappingStart((MappingStartEvent) event, tainted, handler);
                    } else if (event.is(Event.ID.MappingEnd)) {
                        invoke(handler, "end_mapping");
                    } else if (event.is(Event.ID.StreamEnd)) {
                        invoke(handler, "end_stream");

                        break;
                    }
                }
            } catch (ParserException pe) {
                parser = null;
                Layouts.PSYCH_PARSER_LAYOUT.setParser(parserObject, parser);
                raiseParserException(yaml, pe, path);
            } catch (ScannerException se) {
                parser = null;
                Layouts.PSYCH_PARSER_LAYOUT.setParser(parserObject, parser);
                StringBuilder message = new StringBuilder("syntax error");
                if (se.getProblemMark() != null) {
                    message.append(se.getProblemMark().toString());
                }
                raiseParserException(yaml, se, path);
            } catch (ReaderException re) {
                parser = null;
                Layouts.PSYCH_PARSER_LAYOUT.setParser(parserObject, parser);
                raiseParserException(yaml, re, path);
            } catch (Throwable t) {
                Helpers.throwException(t);
                Layouts.PSYCH_PARSER_LAYOUT.setParser(parserObject, parser);
                return parserObject;
            }

            return parserObject;
        }

        private StreamReader readerFor(DynamicObject yaml) {
            if (RubyGuards.isRubyString(yaml)) {
                ByteList byteList = Layouts.STRING.getByteList(yaml);
                Encoding enc = byteList.getEncoding();

                // if not unicode, transcode to UTF8
                if (!(enc instanceof UnicodeEncoding)) {
                    byteList = EncodingUtils.strConvEnc(getContext().getRuntime().getCurrentContext(), byteList, enc, UTF8Encoding.INSTANCE);
                    enc = UTF8Encoding.INSTANCE;
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());

                Charset charset = enc.getCharset();

                assert charset != null : "charset for encoding " + enc + " should not be null";

                InputStreamReader isr = new InputStreamReader(bais, charset);

                return new StreamReader(isr);
            }

            // fall back on IOInputStream, using default charset
            if ((boolean) ruby("yaml.respond_to? :read", "yaml", yaml)) {
                Encoding enc = ((boolean) ruby("yaml.is_a? RubyIO", "yaml", yaml))
                        ? UTF8Encoding.INSTANCE // ((RubyIO)yaml).getReadEncoding()
                        : UTF8Encoding.INSTANCE;
                Charset charset = enc.getCharset();
                throw new UnsupportedOperationException();
                //return new StreamReader(new InputStreamReader(new IOInputStream(yaml), charset));
            } else {
                throw new UnsupportedOperationException();
                //throw runtime.newTypeError(yaml, runtime.getIO());
            }
        }

        private void handleDocumentStart(DocumentStartEvent dse, boolean tainted, Object handler) {
            DumperOptions.Version _version = dse.getVersion();
            Integer[] versionInts = _version == null ? null : _version.getArray();
            Object version = versionInts == null ?
                    Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0):
                    Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{versionInts[0], versionInts[1]}, 2);

            Map<String, String> tagsMap = dse.getTags();
            DynamicObject tags = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
            if (tagsMap != null && tagsMap.size() > 0) {
                for (Map.Entry<String, String> tag : tagsMap.entrySet()) {
                    Object key   = stringFor(tag.getKey(), tainted);
                    Object value = stringFor(tag.getValue(), tainted);
                    ruby("tags.push [key, value]", "tags", tags, "key", key, "value", value);
                }
            }
            Object notExplicit = !dse.getExplicit();

            invoke(handler, "start_document", version, tags, notExplicit);
        }

        private void handleMappingStart(MappingStartEvent mse, boolean tainted, Object handler) {
            Object anchor = stringOrNilFor(mse.getAnchor(), tainted);
            Object tag = stringOrNilFor(mse.getTag(), tainted);
            Object implicit = mse.getImplicit();
            Object style = translateFlowStyle(mse.getFlowStyle());

            invoke(handler, "start_mapping", anchor, tag, implicit, style);
        }

        private void handleScalar(ScalarEvent se, boolean tainted, Object handler) {
            Object anchor = stringOrNilFor(se.getAnchor(), tainted);
            Object tag = stringOrNilFor(se.getTag(), tainted);
            Object plain_implicit = se.getImplicit().canOmitTagInPlainScalar();
            Object quoted_implicit = se.getImplicit().canOmitTagInNonPlainScalar();
            Object style = translateStyle(se.getStyle());
            Object val = stringFor(se.getValue(), tainted);

            invoke(handler, "scalar", val, anchor, tag, plain_implicit,
                    quoted_implicit, style);
        }

        private void handleSequenceStart(SequenceStartEvent sse, boolean tainted, Object handler) {
            Object anchor = stringOrNilFor(sse.getAnchor(), tainted);
            Object tag = stringOrNilFor(sse.getTag(), tainted);
            Object implicit = sse.getImplicit();
            Object style = translateFlowStyle(sse.getFlowStyle());

            invoke(handler, "start_sequence", anchor, tag, implicit, style);
        }

        private void raiseParserException(DynamicObject yaml, ReaderException re, DynamicObject rbPath) {
            throw new UnsupportedOperationException();
        }

        private static void raiseParserException(DynamicObject yaml, MarkedYAMLException mye, DynamicObject rbPath) {
            throw new UnsupportedOperationException();
        }

        private Object invoke(Object receiver, String name, Object... args) {
            return ruby("receiver.send(name, *args)", "receiver", receiver, "name", getSymbol(name),
                    "args", Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), args, args.length));
        }

        private static int translateStyle(Character style) {
            if (style == null) return 0; // any

            switch (style) {
                case 0: return 1; // plain
                case '\'': return 2; // single-quoted
                case '"': return 3; // double-quoted
                case '|': return 4; // literal
                case '>': return 5; // folded
                default: return 0; // any
            }
        }

        private static int translateFlowStyle(Boolean flowStyle) {
            if (flowStyle == null) return 0; // any

            if (flowStyle) return 2;
            return 1;
        }

        private Object stringOrNilFor(String value, boolean tainted) {
            if (value == null) return nil(); // No need to taint nil

            return stringFor(value, tainted);
        }

        private Object stringFor(String value, boolean tainted) {
            // TODO CS 23-Sep-15 this is JRuby's internal encoding, not ours
            Encoding encoding = getContext().getRuntime().getDefaultInternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
            }

            Charset charset = RubyEncoding.UTF8;
            if (encoding.getCharset() != null) {
                charset = encoding.getCharset();
            }

            ByteList bytes = new ByteList(value.getBytes(charset), encoding);
            Object string = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), bytes, StringSupport.CR_UNKNOWN, null);

            if (tainted) {
                ruby("string.taint", "string", string);
            }

            return string;
        }

        private Object getInstanceVariable(String name) {
            return ruby(name);
        }

    }

}
