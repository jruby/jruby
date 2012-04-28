/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.psych;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyException;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOInputStream;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.unsafe.UnsafeFactory;
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
import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class PsychParser extends RubyObject {

    private static final Logger LOG = LoggerFactory.getLogger("PsychParser");

    public static final int YAML_ANY_ENCODING = 0;
    public static final int YAML_UTF8_ENCODING = UTF8Encoding.INSTANCE.getIndex();
    public static final int YAML_UTF16LE_ENCODING = UTF16LEEncoding.INSTANCE.getIndex();
    public static final int YAML_UTF16BE_ENCODING = UTF16BEEncoding.INSTANCE.getIndex();
    
    public static void initPsychParser(Ruby runtime, RubyModule psych) {
        RubyClass psychParser = runtime.defineClassUnder("Parser", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new PsychParser(runtime, klazz);
            }
        }, psych);

        psychParser.defineConstant("ANY", runtime.newFixnum(YAML_ANY_ENCODING));
        psychParser.defineConstant("UTF8", runtime.newFixnum(YAML_UTF8_ENCODING));
        psychParser.defineConstant("UTF16LE", runtime.newFixnum(YAML_UTF16LE_ENCODING));
        psychParser.defineConstant("UTF16BE", runtime.newFixnum(YAML_UTF16BE_ENCODING));

        psychParser.defineAnnotatedMethods(PsychParser.class);

        psych.defineClassUnder("SyntaxError", runtime.getSyntaxError(), RubyException.EXCEPTION_ALLOCATOR);
    }

    public PsychParser(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context, IRubyObject yaml) {
        Ruby runtime = context.runtime;

        return parse(context, yaml, RubyString.newString(runtime, "<unknown>"));
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context, IRubyObject yaml, IRubyObject path) {
        Ruby runtime = context.runtime;
        boolean tainted = yaml.isTaint();
        
        // FIXME? only supports Unicode, since we have to produces strings...
        try {
            StreamReader reader;
            if (yaml.respondsTo("read")) {
                reader = new StreamReader(new InputStreamReader(new IOInputStream(yaml), RubyEncoding.UTF8));
                if (yaml instanceof RubyIO) {
                    tainted = true;
                }
            } else {
                reader = new StreamReader(new StringReader(yaml.convertToString().asJavaString()));
            }
            parser = new ParserImpl(reader);
            IRubyObject handler = getInstanceVariable("@handler");

            while (true) {
                event = parser.getEvent();

                // FIXME: Event should expose a getID, so it can be switched
                if (event.is(ID.StreamStart)) {
                    invoke(
                            context,
                            handler,
                            "start_stream",
                            runtime.newFixnum(YAML_ANY_ENCODING));
                } else if (event.is(ID.DocumentStart)) {
                    DocumentStartEvent dse = (DocumentStartEvent)event;

                    Integer[] versionInts = dse.getVersion();
                    IRubyObject version = versionInts == null ?
                        RubyArray.newArray(runtime) :
                        RubyArray.newArray(runtime, runtime.newFixnum(versionInts[0]), runtime.newFixnum(versionInts[1]));
                    
                    Map<String, String> tagsMap = dse.getTags();
                    RubyArray tags = RubyArray.newArray(runtime);
                    if (tags.size() > 0) {
                        for (Map.Entry<String, String> tag : tagsMap.entrySet()) {
                            RubyString key   = RubyString.newString(runtime, tag.getKey());
                            RubyString value = RubyString.newString(runtime, tag.getValue());
                            key.setTaint(tainted);
                            value.setTaint(tainted);

                            tags.append(RubyArray.newArray(
                                    runtime,
                                    key,
                                    value));
                        }
                    }

                    invoke(
                            context,
                            handler,
                            "start_document",
                            version,
                            tags,
                            runtime.newBoolean(!dse.getExplicit()));
                } else if (event.is(ID.DocumentEnd)) {
                    DocumentEndEvent dee = (DocumentEndEvent)event;
                    invoke(
                            context,
                            handler,
                            "end_document",
                            runtime.newBoolean(!dee.getExplicit()));
                } else if (event.is(ID.Alias)) {
                    AliasEvent ae = (AliasEvent)event;
                    IRubyObject alias = runtime.getNil();
                    if (ae.getAnchor() != null) {
                        alias = RubyString.newString(runtime, ae.getAnchor());
                        alias.setTaint(tainted);
                    }

                    invoke(
                            context,
                            handler,
                            "alias",
                            alias);
                } else if (event.is(ID.Scalar)) {
                    ScalarEvent se = (ScalarEvent)event;
                    IRubyObject anchor = se.getAnchor() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, se.getAnchor());
                    IRubyObject tag = se.getTag() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, se.getTag());
                    IRubyObject plain_implicit = runtime.newBoolean(se.getImplicit().canOmitTagInPlainScalar());
                    IRubyObject quoted_implicit = runtime.newBoolean(se.getImplicit().canOmitTagInNonPlainScalar());
                    IRubyObject style = runtime.newFixnum(translateStyle(se.getStyle()));
                    IRubyObject val = RubyString.newString(runtime, se.getValue());

                    val.setTaint(tainted);
                    anchor.setTaint(tainted);
                    tag.setTaint(tainted);

                    invoke(
                            context,
                            handler,
                            "scalar",
                            val,
                            anchor,
                            tag,
                            plain_implicit,
                            quoted_implicit,
                            style);
                } else if (event.is(ID.SequenceStart)) {
                    SequenceStartEvent sse = (SequenceStartEvent)event;
                    IRubyObject anchor = sse.getAnchor() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, sse.getAnchor());
                    IRubyObject tag = sse.getTag() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, sse.getTag());
                    IRubyObject implicit = runtime.newBoolean(sse.getImplicit());
                    IRubyObject style = runtime.newFixnum(translateFlowStyle(sse.getFlowStyle()));

                    anchor.setTaint(tainted);
                    tag.setTaint(tainted);

                    invoke(
                            context,
                            handler,
                            "start_sequence",
                            anchor,
                            tag,
                            implicit,
                            style);
                } else if (event.is(ID.SequenceEnd)) {
                    invoke(
                            context,
                            handler,
                            "end_sequence");
                } else if (event.is(ID.MappingStart)) {
                    MappingStartEvent mse = (MappingStartEvent)event;
                    IRubyObject anchor = mse.getAnchor() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, mse.getAnchor());
                    IRubyObject tag = mse.getTag() == null ?
                        runtime.getNil() :
                        RubyString.newString(runtime, mse.getTag());
                    IRubyObject implicit = runtime.newBoolean(mse.getImplicit());
                    IRubyObject style = runtime.newFixnum(translateFlowStyle(mse.getFlowStyle()));

                    anchor.setTaint(tainted);
                    tag.setTaint(tainted);

                    invoke(
                            context,
                            handler,
                            "start_mapping",
                            anchor,
                            tag,
                            implicit,
                            style);
                } else if (event.is(ID.MappingEnd)) {
                    invoke(
                            context,
                            handler,
                            "end_mapping");
                } else if (event.is(ID.StreamEnd)) {
                    invoke(
                            context,
                            handler,
                            "end_stream");
                    break;
                }
            }
        } catch (ParserException pe) {
            parser = null;
            RubyKernel.raise(context, runtime.getKernel(),
                    new IRubyObject[] {runtime.getModule("Psych").getConstant("SyntaxError"), runtime.newString(syntaxError(context, yaml, pe))},
                    Block.NULL_BLOCK);
        } catch (ScannerException se) {
            parser = null;
            StringBuilder message = new StringBuilder("syntax error");
            if (se.getProblemMark() != null) {
                message.append(se.getProblemMark().toString());
            }
            throw runtime.newArgumentError(message.toString());
        } catch (ReaderException re) {
            parser = null;
            RubyKernel.raise(context, runtime.getKernel(),
                    new IRubyObject[] {runtime.getModule("Psych").getConstant("SyntaxError"), runtime.newString(re.getLocalizedMessage())},
                    Block.NULL_BLOCK);
        } catch (Throwable t) {
            UnsafeFactory.getUnsafe().throwException(t);
            return this;
        }

        return this;
    }

    private static String syntaxError(ThreadContext context, IRubyObject yaml, MarkedYAMLException mye) {
        String path;
        if (yaml.respondsTo("path")) {
            path = yaml.callMethod(context, "path").toString();
        } else {
            path = "<unknown>";
        }
        return path + ": couldn't parse YAML at line " + mye.getProblemMark().getLine() + " column " + mye.getProblemMark().getColumn();
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
            if (event == null) {
                event = this.event;
            }
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

    @JRubyMethod(name = "external_encoding=")
    public IRubyObject external_encoding_set(ThreadContext context, IRubyObject encoding) {
        // stubbed
        return encoding;
    }

    private Parser parser;
    private Event event;
}
