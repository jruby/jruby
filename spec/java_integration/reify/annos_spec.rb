require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

describe "JRuby annotation processing:" do
  context "method annotations using #add_method_annotation" do
    class ClassWithAnnotatedMethods
      add_method_annotation 'foo', {Java::java_integration.fixtures.MethodAnnotations::Annotated => {}}
      def foo; end

      add_method_annotation 'bar', {Java::java_integration.fixtures.MethodAnnotations::Annotated => {}}
      def bar; end

      def baz; end

      become_java!
    end

    it "has two annotated methods" do
      expect(Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods).size).to eq(2)
    end
  end

  context "parameter annotations using #add_parameter_annotation" do
    class ClassWithAnnotatedParams
      add_parameter_annotation 'foo', [{Java::java_integration.fixtures.ParameterAnnotations::Annotated => {}}]
      def foo(x); end

      become_java!
    end

    it "has an annotated parameter" do
      expect(Java::java_integration.fixtures.ParameterAnnotations.countAnnotated(ClassWithAnnotatedParams).size).to eq(1)
    end
  end

  context "method annotations using #java_signature" do
    class ClassWithAnnotatedMethods2
      java_signature("@java_integration.fixtures.MethodAnnotations.Annotated void foo()")
      def foo; end

      java_signature("@java_integration.fixtures.MethodAnnotations.Annotated void bar()")
      def bar; end

      def baz; end

      become_java!
    end

    it "has two annotated methods" do
      expect(Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods2).size).to eq(2)
    end
  end

  context "method annotations with parameters using #java_signature" do
    class ClassWithAnnotatedMethods3
      java_signature("@java_integration.fixtures.EveryTypeAnnotations.Annotated("+
        "astr=\"Hello\", abyte=0xde, ashort=0xEF_FF, anint=0xFFff_EeeE, along=0xFFFF_EEEE_0000_9999,"+
        "afloat=3.5, adouble=1024.1024, abool=true, anbool=false, achar='?',"+
        "anenum=java.lang.annotation.RetentionPolicy.RUNTIME, aClass=java.lang.String.java_class,"+
        "Darray={@jakarta.annotation.Resource(description=\"first\"), @jakarta.annotation.Resource(description=\"second\")})"+
        " void foo()")
      def foo; end
  
      java_signature("@java_integration.fixtures.EveryTypeAnnotations.Annotated() void bar()")
      def bar; end
        

      java_signature("@java_integration.fixtures.EveryTypeAnnotations.Annotated("+
        "astr=\"This is a long string \\n with \\\" a quote\", abyte=127, ashort=32767, anint=2147483647, along=9223372036854775807)"+
        " void foo_unsign()")
      def foo_unsign; end
        
      java_signature("@java_integration.fixtures.EveryTypeAnnotations.Annotated("+
        "astr=\"\", abyte=-128, ashort=-32768, anint=-2147483648, along=-9223372036854775808,"+
        "afloat=-3.5, adouble=-10_24.10_24, abool=false, anbool=true, achar='\\'',"+
        "anenum=java.lang.annotation.RetentionPolicy.SOURCE, aClass=java_integration.fixtures.EveryTypeAnnotations.Annotated.java_class,"+
        "Darray={})"+
        " void foo_sign()")
      def foo_sign; end
  
      def baz; end
  
      become_java!
    end

    it "has four annotated methods" do
      expect(Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3).size).to eq(4)
    end
    it "has default values" do
      output = Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3)["bar"].to_a
      easy_out = output[0..-2]
      arry = output[-1]
      expect(easy_out).to eq(["none", 0, 0,0,0,0.0, 0.0,false, true,0, java.lang.annotation.RetentionPolicy::CLASS, java.lang.Object.java_class.to_java])
      expect(arry).to_not be_nil
      expect(arry.to_a).to eq([])
    end
    it "has hex-set values" do
      output = Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3)["foo"].to_a
      easy_out = output[0..-2]
      arry = output[-1]
      expect(easy_out).to eq(["Hello", -34, -4097,-4370, -18769007044199, 3.5, 1024.1024,true, false,'?'.ord, java.lang.annotation.RetentionPolicy::RUNTIME, java.lang.String.java_class.to_java])
      expect(arry).to_not be_nil
expect(arry.map  &:description).to eq(%w{first second})
    end
    
    it "has signed base10-set values" do
      output = Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3)["foo_sign"].to_a
      easy_out = output[0..-2]
      arry = output[-1]
      expect(easy_out).to eq(["", -0x7f-1, -0x7fff-1, -0x7fffffff-1, -0x7fff_ffff_ffff_ffff-1, -3.5, -1024.1024,false, true,"'".ord, java.lang.annotation.RetentionPolicy::SOURCE, Java::java_integration.fixtures.EveryTypeAnnotations::Annotated.java_class.to_java])
      expect(arry).to_not be_nil
expect(arry.to_a).to eq([])
    end
    it "has unsigned base10-set values" do
      output = Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3)["foo_unsign"].to_a
      easy_out = output[0..-2]
      arry = output[-1]
      expect(easy_out).to eq(["This is a long string \n with \" a quote", 0x7f, 0x7fff, 0x7fffffff, 0x7fff_ffff_ffff_ffff, 0.0, 0.0, false, true,0, java.lang.annotation.RetentionPolicy::CLASS, java.lang.Object.java_class.to_java])
      expect(arry).to_not be_nil
expect(arry.to_a).to eq([])
    end
  end

  context "field annotations using #add_field_annotation" do
    let(:cls) do
      Class.new do
        java_field "java.lang.String foo"
        add_field_annotation(:foo, Java::java_integration.fixtures.FieldAnnotations::Annotated => {})

        java_field "java.lang.String bar"
        add_field_annotation(:bar, Java::java_integration.fixtures.FieldAnnotations::Annotated => {})

        java_field "java.lang.String baz"

        become_java!
      end
    end

    it "has two annotated fields" do
      expect(Java::java_integration.fixtures.FieldAnnotations.countAnnotated(cls).size).to eq(2)
    end
  end


  context "field annotations using #java_field annotation support" do
    let(:cls) do
      Class.new do
        java_field "@java_integration.fixtures.FieldAnnotations.Annotated java.lang.String foo"
  
        java_field "@java_integration.fixtures.FieldAnnotations.Annotated java.lang.String bar"
  
        java_field "java.lang.String baz"
  
        become_java!
      end
    end
  
    it "has two annotated fields" do
      expect(Java::java_integration.fixtures.FieldAnnotations.countAnnotated(cls).size).to eq(2)
    end
  end
end
