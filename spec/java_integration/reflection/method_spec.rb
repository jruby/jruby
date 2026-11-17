require File.dirname(__FILE__) + "/../spec_helper"

java_import 'java_integration.fixtures.PrivateInstanceMethod'
java_import 'java_integration.fixtures.PrivateStaticMethod'
java_import 'java_integration.fixtures.ProtectedInstanceMethod'
java_import 'java_integration.fixtures.ProtectedStaticMethod'
java_import 'java_integration.fixtures.PackageInstanceMethod'
java_import 'java_integration.fixtures.PackageStaticMethod'

describe "A JavaMethod" do
  describe "given a private Java class method" do
    before(:each) do
      @method = PrivateStaticMethod.java_class.declared_method :thePrivateMethod
      @method.accessible = true
    end
      
    it "should provide a shortcut to invoke the method" do
      expect { @method.invoke_static }.not_to raise_error
    end

    it "should allow invocation with a Ruby nil method" do
      expect { @method.invoke nil }.not_to raise_error
    end
  
    it "should allow invocation with a Java null method" do
      expect { @method.invoke nil.to_java }.not_to raise_error
    end  
  end
  
  describe "given a protected Java class method" do
    before(:each) do
      @method = ProtectedStaticMethod.java_class.declared_method :theProtectedMethod
      @method.accessible = true
    end
  
    it "should provide a shortcut to invoke protected Java class methods" do
      expect { @method.invoke_static }.not_to raise_error
    end

    it "should allow invocation with a Ruby nil method" do
      expect { @method.invoke nil }.not_to raise_error
    end

    it "should allow invocation with a Java null method" do
      expect { @method.invoke nil.to_java }.not_to raise_error
    end
  end
  
  describe "given a package scope Java class method" do
    before(:each) do
      @method = PackageStaticMethod.java_class.declared_method :thePackageScopeMethod
      @method.accessible = true    
    end
    
    it "should provide a shortcut to invoke package scope Java class methods" do
      expect { @method.invoke_static }.not_to raise_error
    end
    
    it "should allow invocation with a Ruby nil method" do
      expect { @method.invoke nil }.not_to raise_error
    end

    it "should allow invocation with a Java null method" do
      expect { @method.invoke nil.to_java }.not_to raise_error
    end
  end    

  it "should provide the ability to invoke private Java instance methods on a Ruby object" do
    o = PrivateInstanceMethod.new
    method = PrivateInstanceMethod.java_class.declared_method :thePrivateMethod
    method.accessible = true
    expect { method.invoke(o) }.not_to raise_error
  end
  
  it "should provide the ability to invoke protected Java instance methods on a Ruby object" do
    o = ProtectedInstanceMethod.new
    method = ProtectedInstanceMethod.java_class.declared_method :theProtectedMethod
    method.accessible = true
    expect { method.invoke(o) }.not_to raise_error
  end
  
  it "should provide the ability to invoke package scope Java instance methods on a Ruby object" do
    o = PackageInstanceMethod.new
    method = PackageInstanceMethod.java_class.declared_method :thePackageScopeMethod
    method.accessible = true
    expect { method.invoke(o) }.not_to raise_error
  end
  
  it "should provide the ability to invoke private Java instance methods on a JavaObject" do
    o = PrivateInstanceMethod.new
    method = PrivateInstanceMethod.java_class.declared_method :thePrivateMethod
    method.accessible = true
    expect { method.invoke(o.java_object) }.not_to raise_error
  end
  
  it "should provide the ability to invoke protected Java instance methods on a JavaObject" do
    o = ProtectedInstanceMethod.new
    method = ProtectedInstanceMethod.java_class.declared_method :theProtectedMethod
    method.accessible = true
    expect { method.invoke(o.java_object) }.not_to raise_error
  end
  
  it "should provide the ability to invoke package scope Java instance methods on a JavaObject" do
    o = PackageInstanceMethod.new
    method = PackageInstanceMethod.java_class.declared_method :thePackageScopeMethod
    method.accessible = true
    expect { method.invoke(o.java_object) }.not_to raise_error
  end
end
