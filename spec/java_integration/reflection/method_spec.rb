require File.dirname(__FILE__) + "/../spec_helper"

import 'java_integration.fixtures.PrivateInstanceMethod'
import 'java_integration.fixtures.PrivateStaticMethod'
import 'java_integration.fixtures.ProtectedInstanceMethod'
import 'java_integration.fixtures.ProtectedStaticMethod'
import 'java_integration.fixtures.PackageInstanceMethod'
import 'java_integration.fixtures.PackageStaticMethod'

describe "A JavaMethod" do
  describe "given a private Java class method" do
    before(:each) do
      @method = PrivateStaticMethod.java_class.declared_method_smart :thePrivateMethod
      @method.accessible = true
    end
      
    it "should provide a shortcut to invoke the method" do
      lambda { @method.invoke_static }.should_not raise_error
    end

    it "should allow invocation with a Ruby nil method" do
      lambda { @method.invoke nil }.should_not raise_error
    end
  
    it "should allow invocation with a Java null method" do
      lambda { @method.invoke Java.ruby_to_java(nil) }.should_not raise_error
    end  
  end
  
  describe "given a protected Java class method" do
    before(:each) do
      @method = ProtectedStaticMethod.java_class.declared_method_smart :theProtectedMethod
      @method.accessible = true
    end
  
    it "should provide a shortcut to invoke protected Java class methods" do
      lambda { @method.invoke_static }.should_not raise_error
    end

    it "should allow invocation with a Ruby nil method" do
      lambda { @method.invoke nil }.should_not raise_error
    end

    it "should allow invocation with a Java null method" do
      lambda { @method.invoke Java.ruby_to_java(nil) }.should_not raise_error
    end
  end
  
  describe "given a package scope Java class method" do
    before(:each) do
      @method = PackageStaticMethod.java_class.declared_method_smart :thePackageScopeMethod
      @method.accessible = true    
    end
    
    it "should provide a shortcut to invoke package scope Java class methods" do
      lambda { @method.invoke_static }.should_not raise_error
    end
    
    it "should allow invocation with a Ruby nil method" do
      lambda { @method.invoke nil }.should_not raise_error
    end

    it "should allow invocation with a Java null method" do
      lambda { @method.invoke Java.ruby_to_java(nil) }.should_not raise_error
    end
  end    

  it "should provide the ability to invoke private Java instance methods on a Ruby object" do
    o = PrivateInstanceMethod.new
    method = PrivateInstanceMethod.java_class.declared_method_smart :thePrivateMethod
    method.accessible = true
    lambda { method.invoke(o) }.should_not raise_error
  end
  
  it "should provide the ability to invoke protected Java instance methods on a Ruby object" do
    o = ProtectedInstanceMethod.new
    method = ProtectedInstanceMethod.java_class.declared_method_smart :theProtectedMethod
    method.accessible = true
    lambda { method.invoke(o) }.should_not raise_error
  end
  
  it "should provide the ability to invoke package scope Java instance methods on a Ruby object" do
    o = PackageInstanceMethod.new
    method = PackageInstanceMethod.java_class.declared_method_smart :thePackageScopeMethod
    method.accessible = true
    lambda { method.invoke(o) }.should_not raise_error
  end
  
  it "should provide the ability to invoke private Java instance methods on a JavaObject" do
    o = PrivateInstanceMethod.new
    method = PrivateInstanceMethod.java_class.declared_method_smart :thePrivateMethod
    method.accessible = true
    lambda { method.invoke(o.java_object) }.should_not raise_error
  end
  
  it "should provide the ability to invoke protected Java instance methods on a JavaObject" do
    o = ProtectedInstanceMethod.new
    method = ProtectedInstanceMethod.java_class.declared_method_smart :theProtectedMethod
    method.accessible = true
    lambda { method.invoke(o.java_object) }.should_not raise_error
  end
  
  it "should provide the ability to invoke package scope Java instance methods on a JavaObject" do
    o = PackageInstanceMethod.new
    method = PackageInstanceMethod.java_class.declared_method_smart :thePackageScopeMethod
    method.accessible = true
    lambda { method.invoke(o.java_object) }.should_not raise_error
  end
end
