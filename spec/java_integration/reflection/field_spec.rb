require File.dirname(__FILE__) + "/../spec_helper"

import java.lang.reflect.Field
import "java_integration.fixtures.PrivateField"
import "java_integration.fixtures.ProtectedField"
import "java_integration.fixtures.PublicField"
import "java_integration.fixtures.PackageField"

describe "A JavaClass" do
  it "should provide a look up for fields using a Java formatted name" do
    PrivateField.java_class.declared_field(:strField).should_not == nil
    ProtectedField.java_class.declared_field(:strField).should_not == nil
    PublicField.java_class.declared_field(:strField).should_not == nil
    PackageField.java_class.declared_field(:strField).should_not == nil
  end

  it "should provide a look up for a fields using a Ruby formatted name" do
    pending "JavaClass does not provide underscore access to fields" do
      PrivateField.java_class.declared_field(:str_field).should_not == nil
      ProtectedField.java_class.declared_field(:str_field).should_not == nil
      PublicField.java_class.declared_field(:str_field).should_not == nil
      PackageField.java_class.declared_field(:str_field).should_not == nil
    end
  end
end

describe "A JavaField" do
  describe "given a partially unwrapped Java object" do
    describe "with a private field" do
      before(:each) do
        @obj = PrivateField.new
        @field = PrivateField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
          lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
      end

      it "should get Ruby values"
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
          lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
      end

      it "should get Ruby values"
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
          lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
      end

      it "should get Ruby values"
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
          lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
        lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
      end

      it "should get Ruby values"
    end
  end
  
  describe "given a Ruby-wrapped Java object" do
    describe "with a private field" do
      before(:each) do
        @obj = PrivateField.new
        @field = PrivateField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj, "42" }.should_not raise_error
          lambda { @field.set_value @obj, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        pending "JavaField can not accept a Ruby-wrapped Java object" do
          lambda { @field.set_value @obj, Java.ruby_to_java("42") }.should_not raise_error
          lambda { @field.set_value @obj, Java.ruby_to_java(nil) }.should_not raise_error
        end
      end

      it "should get Ruby values"
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj, "42" }.should_not raise_error
          lambda { @field.set_value @obj, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        pending "JavaField can not accept a Ruby-wrapped Java object" do
          lambda { @field.set_value @obj, Java.ruby_to_java("42") }.should_not raise_error
          lambda { @field.set_value @obj, Java.ruby_to_java(nil) }.should_not raise_error
        end
      end

      it "should get Ruby values"
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj, "42" }.should_not raise_error
          lambda { @field.set_value @obj, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        pending "JavaField can not accept a Ruby-wrapped Java object" do
          lambda { @field.set_value @obj, Java.ruby_to_java("42") }.should_not raise_error
          lambda { @field.set_value @obj, Java.ruby_to_java(nil) }.should_not raise_error
        end
      end

      it "should get Ruby values"
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        pending "JavaField does not automatically coerce" do
          lambda { @field.set_value @obj, "42" }.should_not raise_error
          lambda { @field.set_value @obj, nil }.should_not raise_error
        end
      end

      it "should set Java values" do
        pending "JavaField can not accept a Ruby-wrapped Java object" do
          lambda { @field.set_value @obj, Java.ruby_to_java("42") }.should_not raise_error
          lambda { @field.set_value @obj, Java.ruby_to_java(nil) }.should_not raise_error
        end
      end

      it "should get Ruby values"
    end
  end
end

