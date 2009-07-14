package org.jruby.anno;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jruby.RubyModule.MethodClumper;
import org.jruby.internal.runtime.methods.DumpingInvocationMethodFactory;
import org.jruby.util.JRubyClassLoader;

public class InvokerGenerator {
    public static final boolean DEBUG = false;
    
    public static void main(String[] args) throws Exception {
        FileReader fr = new FileReader(args[0]);
        BufferedReader br = new BufferedReader(fr);
        
        List<String> classNames = new ArrayList<String>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                classNames.add(line);
            }
        } finally {
            br.close();
        }

        DumpingInvocationMethodFactory dumper = new DumpingInvocationMethodFactory(args[1], new JRubyClassLoader(ClassLoader.getSystemClassLoader()));

        for (String name : classNames) {
            MethodClumper clumper = new MethodClumper();
            
            try {
                if (DEBUG) System.out.println("generating for class " + name);
                Class cls = Class.forName(name, false, InvokerGenerator.class.getClassLoader());

                clumper.clump(cls);

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
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
