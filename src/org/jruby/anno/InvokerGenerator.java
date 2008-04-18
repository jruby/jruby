package org.jruby.anno;

import java.util.List;
import java.util.Map;
import org.jruby.RubyModule.MethodClumper;
import org.jruby.internal.runtime.methods.DumpingInvocationMethodFactory;
import org.jruby.util.JRubyClassLoader;

public class InvokerGenerator {
    public static void main(String[] args) throws Exception {
        Class classList = Class.forName("org.jruby.generated.GeneratedInvokerClassList");
        String[] classNames = (String[])classList.getField("classNames").get(null);

        MethodClumper clumper = new MethodClumper();

        for (String name : classNames) {
            Class cls = Class.forName(name);

            clumper.clumpFromClass(cls);
        }

        DumpingInvocationMethodFactory dumper = new DumpingInvocationMethodFactory(args[0], new JRubyClassLoader(ClassLoader.getSystemClassLoader()));


        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }

        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }

        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_8().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }

        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_8().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }

        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_9().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }

        for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_9().entrySet()) {
            dumper.getAnnotatedMethodClass(entry.getValue());
        }
    }
}
