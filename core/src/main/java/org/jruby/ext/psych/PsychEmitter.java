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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOOutputStream;
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

import static org.jruby.runtime.Visibility.*;

public class PsychEmitter extends RubyObject {
    public static void initPsychEmitter(Ruby runtime, RubyModule psych) {
        RubyClass psychHandler = runtime.defineClassUnder("Handler", runtime.getObject(), runtime.getObject().getAllocator(), psych);
        RubyClass psychEmitter = runtime.defineClassUnder("Emitter", psychHandler, new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new PsychEmitter(runtime, klazz);
            }
        }, psych);

        psychEmitter.defineAnnotatedMethods(PsychEmitter.class);
    }

    public PsychEmitter(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject io) {
        options = new DumperOptions();
        options.setIndent(2);

        this.io = io;

        return context.nil;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject io, IRubyObject rbOptions) {
        IRubyObject width     = rbOptions.callMethod(context, "line_width");
        IRubyObject canonical = rbOptions.callMethod(context, "canonical");
        IRubyObject level     = rbOptions.callMethod(context, "indentation");

        options = new DumperOptions();

        options.setCanonical(canonical.isTrue());
        options.setIndent((int)level.convertToInteger().getLongValue());
        options.setWidth((int)width.convertToInteger().getLongValue());

        this.io = io;

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject start_stream(ThreadContext context, IRubyObject encoding) {
        if (!(encoding instanceof RubyFixnum)) {
            throw context.runtime.newTypeError(encoding, context.runtime.getFixnum());
        }

        initEmitter(context, encoding);

        StreamStartEvent event = new StreamStartEvent(NULL_MARK, NULL_MARK);

        emit(context, event);

        return this;
    }

    @JRubyMethod
    public IRubyObject end_stream(ThreadContext context) {
        StreamEndEvent event = new StreamEndEvent(NULL_MARK, NULL_MARK);
        emit(context, event);
        return this;
    }

    @JRubyMethod
    public IRubyObject start_document(ThreadContext context, IRubyObject _version, IRubyObject tags, IRubyObject implicit) {
        DumperOptions.Version version = null;
        boolean implicitBool = implicit.isTrue();
        Map<String, String> tagsMap = null;

        RubyArray versionAry = _version.convertToArray();
        if (versionAry.size() == 2) {
            int versionInt0 = (int)versionAry.eltInternal(0).convertToInteger().getLongValue();
            int versionInt1 = (int)versionAry.eltInternal(1).convertToInteger().getLongValue();

            if (versionInt0 == 1) {
                if (versionInt1 == 0) {
                    version = DumperOptions.Version.V1_0;
                } else if (versionInt1 == 1) {
                    version = DumperOptions.Version.V1_1;
                }
            }
            if (version == null) {
                throw context.runtime.newArgumentError("invalid YAML version: " + versionAry);
            }
        }

        RubyArray tagsAry = tags.convertToArray();
        if (tagsAry.size() > 0) {
            tagsMap = new HashMap<String, String>(tagsAry.size());
            for (int i = 0; i < tagsAry.size(); i++) {
                RubyArray tagsTuple = tagsAry.eltInternal(i).convertToArray();
                if (tagsTuple.size() != 2) {
                    throw context.runtime.newRuntimeError("tags tuple must be of length 2");
                }
                IRubyObject key = tagsTuple.eltInternal(0);
                IRubyObject value = tagsTuple.eltInternal(1);
                tagsMap.put(
                        key.asJavaString(),
                        value.asJavaString());
            }
        }

        DocumentStartEvent event = new DocumentStartEvent(NULL_MARK, NULL_MARK, !implicitBool, version, tagsMap);
        emit(context, event);
        return this;
    }

    @JRubyMethod
    public IRubyObject end_document(ThreadContext context, IRubyObject implicit) {
        DocumentEndEvent event = new DocumentEndEvent(NULL_MARK, NULL_MARK, !implicit.isTrue());
        emit(context, event);
        return this;
    }

    @JRubyMethod(required = 6)
    public IRubyObject scalar(ThreadContext context, IRubyObject[] args) {
        IRubyObject value = args[0];
        IRubyObject anchor = args[1];
        IRubyObject tag = args[2];
        IRubyObject plain = args[3];
        IRubyObject quoted = args[4];
        IRubyObject style = args[5];
        
        if (!(value instanceof RubyString)) {
            throw context.runtime.newTypeError(value, context.runtime.getString());
        }

        ScalarEvent event = new ScalarEvent(
                anchor.isNil() ? null : anchor.asJavaString(),
                tag.isNil() ? null : tag.asJavaString(),
                new ImplicitTuple(plain.isTrue(),
                quoted.isTrue()),
                value.asJavaString(),
                NULL_MARK,
                NULL_MARK,
                SCALAR_STYLES[(int)style.convertToInteger().getLongValue()]);
        emit(context, event);
        return this;
    }

    @JRubyMethod(required = 4)
    public IRubyObject start_sequence(ThreadContext context, IRubyObject[] args) {
        IRubyObject anchor = args[0];
        IRubyObject tag = args[1];
        IRubyObject implicit = args[2];
        IRubyObject style = args[3];

        final int SEQUENCE_BLOCK = 1; // see psych/nodes/sequence.rb

        SequenceStartEvent event = new SequenceStartEvent(
                anchor.isNil() ? null : anchor.asJavaString(),
                tag.isNil() ? null : tag.asJavaString(),
                implicit.isTrue(),
                NULL_MARK,
                NULL_MARK,
                SEQUENCE_BLOCK != style.convertToInteger().getLongValue());
        emit(context, event);
        return this;
    }

    @JRubyMethod
    public IRubyObject end_sequence(ThreadContext context) {
        SequenceEndEvent event = new SequenceEndEvent(NULL_MARK, NULL_MARK);
        emit(context, event);
        return this;
    }

    @JRubyMethod(required = 4)
    public IRubyObject start_mapping(ThreadContext context, IRubyObject[] args) {
        IRubyObject anchor = args[0];
        IRubyObject tag = args[1];
        IRubyObject implicit = args[2];
        IRubyObject style = args[3];

        final int MAPPING_BLOCK = 1; // see psych/nodes/mapping.rb

        MappingStartEvent event = new MappingStartEvent(
                anchor.isNil() ? null : anchor.asJavaString(),
                tag.isNil() ? null : tag.asJavaString(),
                implicit.isTrue(),
                NULL_MARK,
                NULL_MARK,
                MAPPING_BLOCK != style.convertToInteger().getLongValue());
        emit(context, event);
        return this;
    }

    @JRubyMethod
    public IRubyObject end_mapping(ThreadContext context) {
        MappingEndEvent event = new MappingEndEvent(NULL_MARK, NULL_MARK);
        emit(context, event);
        return this;
    }
    
    @JRubyMethod
    public IRubyObject alias(ThreadContext context, IRubyObject anchor) {
        AliasEvent event = new AliasEvent(anchor.asJavaString(), NULL_MARK, NULL_MARK);
        emit(context, event);
        return this;
    }

    @JRubyMethod(name = "canonical=")
    public IRubyObject canonical_set(ThreadContext context, IRubyObject canonical) {
        // TODO: unclear if this affects a running emitter
        options.setCanonical(canonical.isTrue());
        return canonical;
    }

    @JRubyMethod
    public IRubyObject canonical(ThreadContext context) {
        // TODO: unclear if this affects a running emitter
        return context.runtime.newBoolean(options.isCanonical());
    }

    @JRubyMethod(name = "indentation=")
    public IRubyObject indentation_set(ThreadContext context, IRubyObject level) {
        // TODO: unclear if this affects a running emitter
        options.setIndent((int)level.convertToInteger().getLongValue());
        return level;
    }

    @JRubyMethod
    public IRubyObject indentation(ThreadContext context) {
        // TODO: unclear if this affects a running emitter
        return context.runtime.newFixnum(options.getIndent());
    }

    @JRubyMethod(name = "line_width=")
    public IRubyObject line_width_set(ThreadContext context, IRubyObject width) {
        options.setWidth((int)width.convertToInteger().getLongValue());
        return width;
    }

    @JRubyMethod
    public IRubyObject line_width(ThreadContext context) {
        return context.runtime.newFixnum(options.getWidth());
    }

    private void emit(ThreadContext context, Event event) {
        try {
            if (emitter == null) throw context.runtime.newRuntimeError("uninitialized emitter");

            emitter.emit(event);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } catch (EmitterException ee) {
            throw context.runtime.newRuntimeError(ee.toString());
        }
    }

    private void initEmitter(ThreadContext context, IRubyObject _encoding) {
        if (emitter != null) throw context.runtime.newRuntimeError("already initialized emitter");

        Encoding encoding = PsychLibrary.YAMLEncoding.values()[(int)_encoding.convertToInteger().getLongValue()].encoding;
        Charset charset = context.runtime.getEncodingService().charsetForEncoding(encoding);

        emitter = new Emitter(new OutputStreamWriter(new IOOutputStream(io, encoding), charset), options);
    }

    Emitter emitter;
    DumperOptions options = new DumperOptions();
    IRubyObject io;

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
