require File.dirname(__FILE__) + "/../spec_helper"

import java.lang.reflect.Field
import "java_integration.fixtures.PrivateField"
import "java_integration.fixtures.ProtectedField"
import "java_integration.fixtures.PublicField"

describe "Java field reflection" do
  it "should provide a look up for a private field using a Java formatted name" do
    PrivateField.java_class.declared_field(:strField).should_not == nil
  end

  it "should provide a look up for a private field using a Ruby formatted name" do
    pending "JavaClass does not provide underscore access to fields" do
      PrivateField.java_class.declared_field(:str_field).should_not == nil
    end
  end
  
  it "should provide a look up for a protected field using a Java formatted name" do
    ProtectedField.java_class.declared_field(:strField).should_not == nil
  end

  it "should provide a look up for a protected field using a Ruby formatted name" do
    pending "JavaClass does not provide underscore access to fields" do
      ProtectedField.java_class.declared_field(:str_field).should_not == nil
    end
  end
  
  it "should provide a look up for a public field using a Java formatted name" do
    PublicField.java_class.declared_field(:strField).should_not == nil
  end

  it "should provide a look up for a public field using a Ruby formatted name" do
    pending "JavaClass does not provide underscore access to fields" do
      PublicField.java_class.declared_field(:str_field).should_not == nil
    end
  end
  
  describe "when using a private member" do
    before(:each) do
      @obj = PrivateField.new
      @field = PrivateField.java_class.declared_field :strField
      @field.accessible = true
    end

    it "should set non-null Ruby values on private members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
      end
    end
    
    it "should set null Ruby values on private members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end
    end
    
    it "should set non-null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
    end
    
    it "should set null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
    end
    
    it "should get non-null Ruby values from private members"
    it "should get null Ruby values from private members"
  end
  
  describe "when using a protected member" do
    before(:each) do
      @obj = ProtectedField.new
      @field = ProtectedField.java_class.declared_field :strField
      @field.accessible = true
    end

    it "should set non-null Ruby values on protected members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
      end
    end
    
    it "should set null Ruby values on protected members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end
    end    

    it "should set non-null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
    end
    
    it "should set null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
    end
    
    it "should get non-null Ruby values from private members"
    it "should get null Ruby values from private members"    
  end
  
  describe "when using a public member" do
    before(:each) do
      @obj = PublicField.new
      @field = PublicField.java_class.declared_field :strField
      @field.accessible = true
    end

    it "should set null Ruby values on public members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
      end
    end

    it "should set Ruby non-null values on public members" do
      pending "JavaField does not automatically coerce" do
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end
    end      
    
    it "should set non-null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java("42") }.should_not raise_error
    end
    
    it "should set null Java values on protected members" do
      lambda { @field.set_value @obj.java_object, Java.ruby_to_java(nil) }.should_not raise_error
    end

    it "should get non-null Ruby values from private members"
    it "should get null Ruby values from private members"
  end
end
    
