/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.om.dsl.processor;

import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.processor.layout.LayoutGenerator;
import org.jruby.truffle.om.dsl.processor.layout.LayoutParser;
import org.jruby.truffle.om.dsl.processor.layout.model.LayoutModel;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

@SupportedAnnotationTypes("org.jruby.truffle.om.dsl.api.Layout")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class OMProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Layout.class)) {
            // assert element.getKind() == ElementKind.INTERFACE : element.getKind();

            if (!(element instanceof TypeElement)) {
                throw new UnsupportedOperationException(element.toString());
            }

            if (element instanceof TypeElement) {
                processLayout((TypeElement) element);
            }
        }

        return true;
    }

    private void processLayout(TypeElement layoutElement) {
        try {
            final LayoutParser parser = new LayoutParser();
            parser.parse(layoutElement);

            final LayoutModel layout = parser.build();

            final LayoutGenerator generator = new LayoutGenerator(layout);

            JavaFileObject output = processingEnv.getFiler().createSourceFile(layout.getInterfaceFullName() + "Impl", layoutElement);

            try (PrintStream stream = new PrintStream(output.openOutputStream(), false, "US-ASCII")) {
                generator.generate(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(String file, String message) {
        try (PrintStream stream = new PrintStream(new FileOutputStream(file, true), false, "US-ASCII")) {
            stream.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
