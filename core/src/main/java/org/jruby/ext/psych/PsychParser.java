/***** BEGIN LICENSE BLOCK *****
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
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.psych;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import static org.jruby.ext.psych.PsychLibrary.YAMLEncoding.*;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOInputStream;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
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
import static org.jruby.runtime.Helpers.invoke;
import org.jruby.util.ByteList;

public class PsychParser extends RubyObject {

    private static final Logger LOG = LoggerFactory.getLogger("PsychParser");
    
    public static void initPsychParser(Ruby runtime, RubyModule psych) {
        RubyClass psychParser = runtime.defineClassUnder("Parser", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new PsychParser(runtime, klazz);
            }
        }, psych);

        RubyKernel.require(runtime.getNil(),
                runtime.newString("psych/syntax_error"), Block.NULL_BLOCK);
        psychParser.defineConstant("ANY", runtime.newFixnum(YAML_ANY_ENCODING.ordinal()));
        psychParser.defineConstant("UTF8", runtime.newFixnum(YAML_UTF8_ENCODING.ordinal()));
        psychParser.defineConstant("UTF16LE", runtime.newFixnum(YAML_UTF16LE_ENCODING.ordinal()));
        psychParser.defineConstant("UTF16BE", runtime.newFixnum(YAML_UTF16BE_ENCODING.ordinal()));

        psychParser.defineAnnotatedMethods(PsychParser.class);
    }

    public PsychParser(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context, IRubyObject yaml) {
        Ruby runtime = context.runtime;

        return parse(context, yaml, runtime.getNil());
    }

    private IRubyObject stringOrNilFor(Ruby runtime, String value, boolean tainted) {
        if (value == null) return runtime.getNil(); // No need to taint nil

        return stringFor(runtime, value, tainted);
    }
    
    private RubyString stringFor(Ruby runtime, String value, boolean tainted) {
        Encoding encoding = runtime.getDefaultInternalEncoding();
        if (encoding == null) {
            encoding = UTF8Encoding.INSTANCE;
        }

        Charset charset = RubyEncoding.UTF8;
        if (encoding.getCharset() != null) {
            charset = encoding.getCharset();
        }

        ByteList bytes = new ByteList(value.getBytes(charset), encoding);
        RubyString string = RubyString.newString(runtime, bytes);
        
        string.setTaint(tainted);
        
        return string;
    }
    
    private StreamReader readerFor(ThreadContext context, IRubyObject yaml) {
        Ruby runtime = context.runtime;

        if (yaml instanceof RubyString) {
            ByteList byteList = ((RubyString)yaml).getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());

            Charset charset = byteList.getEncoding().getCharset();
            if (charset == null) charset = Charset.defaultCharset();

            InputStreamReader isr = new InputStreamReader(bais, charset);

            return new StreamReader(isr);
        }

        // fall back on IOInputStream, using default charset
        if (yaml.respondsTo("read")) {
            Charset charset = (yaml instanceof RubyIO)
                ? ((RubyIO)yaml).getReadEncoding().getCharset()
                : Charset.defaultCharset();
            return new StreamReader(new InputStreamReader(new IOInputStream(yaml), charset));
        } else {
            throw runtime.newTypeError(yaml, runtime.getIO());
        }
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context, IRubyObject yaml, IRubyObject path) {
        Ruby runtime = context.runtime;
        boolean tainted = yaml.isTaint() || yaml instanceof RubyIO;

        try {
            parser = new ParserImpl(readerFor(context, yaml));

            if (path.isNil() && yaml.respondsTo("path")) {
                path = yaml.callMethod(context, "path");
            }

            IRubyObject handler = getInstanceVariable("@handler");

            while (true) {
                event = parser.getEvent();

                // FIXME: Event should expose a getID, so it can be switched
                if (event.is(ID.StreamStart)) {
                    invoke(context, handler, "start_stream", runtime.newFixnum(YAML_ANY_ENCODING.ordinal()));
                } else if (event.is(ID.DocumentStart)) {
                    handleDocumentStart(context, (DocumentStartEvent) event, tainted, handler);
                } else if (event.is(ID.DocumentEnd)) {
                    IRubyObject notExplicit = runtime.newBoolean(!((DocumentEndEvent) event).getExplicit());
                    
                    invoke(context, handler, "end_document", notExplicit);
                } else if (event.is(ID.Alias)) {
                    IRubyObject alias = stringOrNilFor(runtime, ((AliasEvent)event).getAnchor(), tainted);

                    invoke(context, handler, "alias", alias);
                } else if (event.is(ID.Scalar)) {
                    handleScalar(context, (ScalarEvent) event, tainted, handler);
                } else if (event.is(ID.SequenceStart)) {
                    handleSequenceStart(context,(SequenceStartEvent) event, tainted, handler);
                } else if (event.is(ID.SequenceEnd)) {
                    invoke(context, handler, "end_sequence");
                } else if (event.is(ID.MappingStart)) {
                    handleMappingStart(context, (MappingStartEvent) event, tainted, handler);
                } else if (event.is(ID.MappingEnd)) {
                    invoke(context, handler, "end_mapping");
                } else if (event.is(ID.StreamEnd)) {
                    invoke(context, handler, "end_stream");
                    
                    break;
                }
            }
        } catch (ParserException pe) {
            parser = null;
            raiseParserException(context, yaml, pe, path);

        } catch (ScannerException se) {
            parser = null;
            StringBuilder message = new StringBuilder("syntax error");
            if (se.getProblemMark() != null) {
                message.append(se.getProblemMark().toString());
            }
            raiseParserException(context, yaml, se, path);

        } catch (ReaderException re) {
            parser = null;
            raiseParserException(context, yaml, re, path);

        } catch (Throwable t) {
            Helpers.throwException(t);
            return this;
        }

        return this;
    }
    
    private void handleDocumentStart(ThreadContext context, DocumentStartEvent dse, boolean tainted, IRubyObject handler) {
        Ruby runtime = context.runtime;
        DumperOptions.Version _version = dse.getVersion();
        Integer[] versionInts = _version == null ? null : _version.getArray();
        IRubyObject version = versionInts == null ?
            RubyArray.newArray(runtime) :
            RubyArray.newArray(runtime, runtime.newFixnum(versionInts[0]), runtime.newFixnum(versionInts[1]));
        
        Map<String, String> tagsMap = dse.getTags();
        RubyArray tags = RubyArray.newArray(runtime);
        if (tagsMap != null && tagsMap.size() > 0) {
            for (Map.Entry<String, String> tag : tagsMap.entrySet()) {
                IRubyObject key   = stringFor(runtime, tag.getKey(), tainted);
                IRubyObject value = stringFor(runtime, tag.getValue(), tainted);

                tags.append(RubyArray.newArray(runtime, key, value));
            }
        }
        IRubyObject notExplicit = runtime.newBoolean(!dse.getExplicit());

        invoke(context, handler, "start_document", version, tags, notExplicit);
    }
    
    private void handleMappingStart(ThreadContext context, MappingStartEvent mse, boolean tainted, IRubyObject handler) {
        Ruby runtime = context.runtime;
        IRubyObject anchor = stringOrNilFor(runtime, mse.getAnchor(), tainted);
        IRubyObject tag = stringOrNilFor(runtime, mse.getTag(), tainted);
        IRubyObject implicit = runtime.newBoolean(mse.getImplicit());
        IRubyObject style = runtime.newFixnum(translateFlowStyle(mse.getFlowStyle()));

        invoke(context, handler, "start_mapping", anchor, tag, implicit, style);
    }
        
    private void handleScalar(ThreadContext context, ScalarEvent se, boolean tainted, IRubyObject handler) {
        Ruby runtime = context.runtime;
        IRubyObject anchor = stringOrNilFor(runtime, se.getAnchor(), tainted);
        IRubyObject tag = stringOrNilFor(runtime, se.getTag(), tainted);
        IRubyObject plain_implicit = runtime.newBoolean(se.getImplicit().canOmitTagInPlainScalar());
        IRubyObject quoted_implicit = runtime.newBoolean(se.getImplicit().canOmitTagInNonPlainScalar());
        IRubyObject style = runtime.newFixnum(translateStyle(se.getStyle()));
        IRubyObject val = stringFor(runtime, se.getValue(), tainted);

        invoke(context, handler, "scalar", val, anchor, tag, plain_implicit,
                quoted_implicit, style);
    }
    
    private void handleSequenceStart(ThreadContext context, SequenceStartEvent sse, boolean tainted, IRubyObject handler) {
        Ruby runtime = context.runtime;
        IRubyObject anchor = stringOrNilFor(runtime, sse.getAnchor(), tainted);
        IRubyObject tag = stringOrNilFor(runtime, sse.getTag(), tainted);
        IRubyObject implicit = runtime.newBoolean(sse.getImplicit());
        IRubyObject style = runtime.newFixnum(translateFlowStyle(sse.getFlowStyle()));

        invoke(context, handler, "start_sequence", anchor, tag, implicit, style);
    }

    private static void raiseParserException(ThreadContext context, IRubyObject yaml, ReaderException re, IRubyObject rbPath) {
        Ruby runtime;
        RubyClass se;
        IRubyObject exception;

        runtime = context.runtime;
        se = (RubyClass)runtime.getModule("Psych").getConstant("SyntaxError");

        exception = se.newInstance(context,
                new IRubyObject[] {
                    rbPath,
                    runtime.newFixnum(0),
                    runtime.newFixnum(0),
                    runtime.newFixnum(re.getPosition()),
                    (null == re.getName() ? runtime.getNil() : runtime.newString(re.getName())),
                    (null == re.toString() ? runtime.getNil() : runtime.newString(re.toString()))
                },
                Block.NULL_BLOCK);

        RubyKernel.raise(context, runtime.getKernel(), new IRubyObject[] { exception }, Block.NULL_BLOCK);
    }

    private static void raiseParserException(ThreadContext context, IRubyObject yaml, MarkedYAMLException mye, IRubyObject rbPath) {
        Ruby runtime;
        Mark mark;
        RubyClass se;
        IRubyObject exception;

        runtime = context.runtime;
        se = (RubyClass)runtime.getModule("Psych").getConstant("SyntaxError");

        mark = mye.getProblemMark();

        exception = se.newInstance(context,
                new IRubyObject[] {
                    rbPath,
                    runtime.newFixnum(mark.getLine() + 1),
                    runtime.newFixnum(mark.getColumn() + 1),
                    runtime.newFixnum(mark.getIndex()),
                    (null == mye.getProblem() ? runtime.getNil() : runtime.newString(mye.getProblem())),
                    (null == mye.getContext() ? runtime.getNil() : runtime.newString(mye.getContext()))
                },
                Block.NULL_BLOCK);

        RubyKernel.raise(context, runtime.getKernel(), new IRubyObject[] { exception }, Block.NULL_BLOCK);
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

    @JRubyMethod
    public IRubyObject mark(ThreadContext context) {
        Ruby runtime = context.runtime;

        Event event = null;

        if (parser != null) {
            event = parser.peekEvent();

            if (event == null) event = this.event;
        }

        if (event == null) {
            return ((RubyClass)context.runtime.getClassFromPath("Psych::Parser::Mark")).newInstance(
                    context,
                    runtime.newFixnum(0),
                    runtime.newFixnum(0),
                    runtime.newFixnum(0),
                    Block.NULL_BLOCK
            );
        }

        Mark mark = event.getStartMark();

        return ((RubyClass)context.runtime.getClassFromPath("Psych::Parser::Mark")).newInstance(
                context,
                runtime.newFixnum(mark.getIndex()),
                runtime.newFixnum(mark.getLine()),
                runtime.newFixnum(mark.getColumn()),
                Block.NULL_BLOCK
        );
    }

    private Parser parser;
    private Event event;
}
