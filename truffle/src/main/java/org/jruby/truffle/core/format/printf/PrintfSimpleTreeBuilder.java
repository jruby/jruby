/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.printf;

import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.LiteralFormatNode;
import org.jruby.truffle.core.format.control.SequenceNode;
import org.jruby.truffle.core.format.convert.ToDoubleWithCoercionNodeGen;
import org.jruby.truffle.core.format.convert.ToIntegerNodeGen;
import org.jruby.truffle.core.format.convert.ToStringNodeGen;
import org.jruby.truffle.core.format.format.FormatCharacterNodeGen;
import org.jruby.truffle.core.format.format.FormatFloatNodeGen;
import org.jruby.truffle.core.format.format.FormatIntegerBinaryNodeGen;
import org.jruby.truffle.core.format.format.FormatIntegerNodeGen;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.format.read.array.ReadArgumentIndexValueNodeGen;
import org.jruby.truffle.core.format.read.array.ReadHashValueNodeGen;
import org.jruby.truffle.core.format.read.array.ReadIntegerNodeGen;
import org.jruby.truffle.core.format.read.array.ReadStringNodeGen;
import org.jruby.truffle.core.format.read.array.ReadValueNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBytesNodeGen;
import org.jruby.truffle.core.format.write.bytes.WritePaddedBytesNodeGen;
import org.jruby.truffle.core.rope.CodeRange;

import java.util.ArrayList;
import java.util.List;

public class PrintfSimpleTreeBuilder {

    private final RubyContext context;
    private final List<FormatNode> sequence = new ArrayList<>();
    private final List<SprintfConfig> configs;

    public static final int DEFAULT = -1;

    private static final byte[] EMPTY_BYTES = new byte[]{};

    public PrintfSimpleTreeBuilder(RubyContext context, List<SprintfConfig> configs) {
        this.context = context;
        this.configs = configs;
    }

    private void buildTree() {
        for (SprintfConfig config : configs) {
            final FormatNode node;
            if (config.isLiteral()) {
                node = WriteBytesNodeGen.create(new LiteralFormatNode(config.getLiteralBytes()));
            } else {
                final FormatNode valueNode;

                if (config.getNamesBytes() != null) {
                    final DynamicObject key = context.getSymbolTable().getSymbol(context.getRopeTable().getRope(config.getNamesBytes(), USASCIIEncoding.INSTANCE, CodeRange.CR_7BIT));
                    valueNode = ReadHashValueNodeGen.create(key, new SourceNode());
                } else if (config.getAbsoluteArgumentIndex() != null) {
                    valueNode = ReadArgumentIndexValueNodeGen.create(config.getAbsoluteArgumentIndex(), new SourceNode());
                } else {
                    valueNode = ReadValueNodeGen.create(new SourceNode());
                }

                final FormatNode widthNode;
                if (config.isWidthStar()) {
                    widthNode = ReadIntegerNodeGen.create(new SourceNode());
                } else if (config.isArgWidth()){
                    widthNode = ReadArgumentIndexValueNodeGen.create(config.getWidth(), new SourceNode());
                } else {
                    widthNode = new LiteralFormatNode(config.getWidth() == null ? -1 : config.getWidth());
                }

                final FormatNode precisionNode;
                if(config.isPrecisionStar()){
                    precisionNode = ReadIntegerNodeGen.create(new SourceNode());
                }  else if(config.isPrecisionArg()){
                    precisionNode = ReadArgumentIndexValueNodeGen.create(config.getPrecision(), new SourceNode());
                } else {
                    precisionNode = new LiteralFormatNode(config.getPrecision() == null ? -1 : config.getPrecision());
                }


                switch (config.getFormatType()){
                    case INTEGER:
                        final char format;
                        switch (config.getFormat()) {
                            case 'b':
                            case 'B':
                                format = config.getFormat();
                                break;
                            case 'd':
                            case 'i':
                            case 'u':
                                format = 'd';
                                break;
                            case 'o':
                                format = 'o';
                                break;
                            case 'x':
                            case 'X':
                                format = config.getFormat();
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        if(config.getFormat() == 'b' || config.getFormat() == 'B'){
                            node = WriteBytesNodeGen.create(
                                FormatIntegerBinaryNodeGen.create(format,
                                    config.isPlus(), config.isFsharp(),
                                    config.isMinus(),
                                    config.isHasSpace(),
                                    config.isZero(),
                                    widthNode,
                                    precisionNode,
                                    ToIntegerNodeGen.create(valueNode)));
                        } else {
                            node = WriteBytesNodeGen.create(
                                FormatIntegerNodeGen.create(format, config.isHasSpace(), config.isZero(), config.isPlus(), config.isMinus(), config.isFsharp(),
                                    widthNode,
                                    precisionNode,
                                    ToIntegerNodeGen.create(valueNode)));
                        }
                        break;
                    case FLOAT:
                        switch (config.getFormat()){
                            case 'f':
                            case 'e':
                            case 'E':
                            case 'g':
                            case 'G':
                                node = WriteBytesNodeGen.create(
                                    FormatFloatNodeGen.create(
                                        config.getFormat(), config.isHasSpace(), config.isZero(), config.isPlus(), config.isMinus(), config.isFsharp(),
                                        widthNode,
                                        precisionNode,
                                        ToDoubleWithCoercionNodeGen.create(
                                            valueNode)));
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        break;
                    case OTHER:
                        switch (config.getFormat()){
                            case 'c':
                                node = WriteBytesNodeGen.create(
                                    FormatCharacterNodeGen.create(config.isMinus(), widthNode,
                                        valueNode));
                                break;
                            case 's':
                            case 'p':
                                final String conversionMethodName = config.getFormat() == 's' ? "to_s" : "inspect";
                                final FormatNode conversionNode;

                                if(config.getAbsoluteArgumentIndex() == null && config.getNamesBytes() == null) {
                                    conversionNode = ReadStringNodeGen.create(true, conversionMethodName, false, EMPTY_BYTES, new SourceNode());
                                } else {
                                    conversionNode = ToStringNodeGen.create(true, conversionMethodName, false, EMPTY_BYTES, valueNode);
                                }

                                if (config.getWidth() != null || config.isWidthStar()) {
                                    node = WritePaddedBytesNodeGen.create(config.isMinus(), widthNode, conversionNode);
                                } else {
                                    node = WriteBytesNodeGen.create(conversionNode);
                                }
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("unsupported type: " + config.getFormatType().toString());
                }

            }
            sequence.add(node);
        }


    }



    public FormatNode getNode() {
        buildTree();
        return new SequenceNode(sequence.toArray(new FormatNode[sequence.size()]));
    }

}
