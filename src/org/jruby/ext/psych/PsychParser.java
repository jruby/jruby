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
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.ScannerException;
import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class PsychParser extends RubyObject {
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
    public IRubyObject parse(ThreadContext context, IRubyObject target) {
        Ruby runtime = context.runtime;
        boolean tainted = target.isTaint();
        
        // FIXME? only supports Unicode, since we have to produces strings...
        StreamReader reader;
        if (target.respondsTo("read")) {
            reader = new StreamReader(new InputStreamReader(new IOInputStream(target)));
            if (target instanceof RubyIO) {
                tainted = true;
            }
        } else {
            reader = new StreamReader(new StringReader(target.convertToString().asJavaString()));
        }
        Parser parser = new ParserImpl(reader);
        IRubyObject handler = getInstanceVariable("@handler");
        Event event;

        while (true) {
            try {
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
                        runtime.getNil() :
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
                            runtime.newBoolean(dse.getExplicit()));
                } else if (event.is(ID.DocumentEnd)) {
                    DocumentEndEvent dee = (DocumentEndEvent)event;
                    invoke(
                            context,
                            handler,
                            "end_document",
                            runtime.newBoolean(dee.getExplicit()));
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
                    IRubyObject plain_implicit = runtime.newBoolean(se.getImplicit().isFirst());
                    IRubyObject quoted_implicit = runtime.newBoolean(se.getImplicit().isSecond());
                    IRubyObject style = runtime.newFixnum(se.getStyle());
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
                    IRubyObject style = runtime.newFixnum(sse.getFlowStyle() ? 1 : 0);

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
                    IRubyObject style = runtime.newFixnum(mse.getFlowStyle() ? 1 : 0);

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
            } catch (ParserException pe) {
                parser = null;
                RubyKernel.raise(context, runtime.getKernel(),
                    new IRubyObject[] {runtime.getModule("Psych").getConstant("SyntaxError"), runtime.newString(pe.getLocalizedMessage())},
                    Block.NULL_BLOCK);
            } catch (ScannerException se) {
                parser = null;
                StringBuilder message = new StringBuilder("syntax error");
                if (se.getProblemMark() != null) {
                    message.append(se.getProblemMark().toString());
                }
                throw runtime.newArgumentError(message.toString());
            }
        }

        return this;
    }
}
