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
      Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods).size.should == 2
    end
  end

  context "parameter annotations using #add_parameter_annotation" do
    class ClassWithAnnotatedParams
      add_parameter_annotation 'foo', [{Java::java_integration.fixtures.ParameterAnnotations::Annotated => {}}]
      def foo(x); end

      become_java!
    end

    it "has an annotated parameter" do
      Java::java_integration.fixtures.ParameterAnnotations.countAnnotated(ClassWithAnnotatedParams).size.should == 1
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
      Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods2).size.should == 2
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
      Java::java_integration.fixtures.FieldAnnotations.countAnnotated(cls).size.should == 2
    end
  end
end
