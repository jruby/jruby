require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

describe "JRuby annotation processing:" do
  context "method annotations using #add_method_annotation" do
    class ClassWithAnnotatedMethods
      def foo; end
      add_method_annotation Java::java_integration.fixtures.MethodAnnotations::Annotated

      def bar; end
      add_method_annotation Java::java_integration.fixtures.MethodAnnotations::Annotated

      def baz; end

      become_java!
    end

    it "has two annotated methods" do
      pending
      Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods).size.should == 2
    end
  end

  context "method annotations using #java_signature" do
    class ClassWithAnnotatedMethods2
      java_signature("@java_integration.fixtures.MethodAnnotations.Annotated foo()")
      def foo; end

      java_signature("@java_integration.fixtures.MethodAnnotations.Annotated bar()")
      def bar; end

      def baz; end

      become_java!
    end

    it "has two annotated methods" do
      pending
      Java::java_integration.fixtures.MethodAnnotations.countAnnotated(ClassWithAnnotatedMethods2).size.should == 2
    end
  end
end
