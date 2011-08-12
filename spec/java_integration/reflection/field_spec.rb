require File.dirname(__FILE__) + "/../spec_helper"

java_import java.lang.reflect.Field
java_import "java_integration.fixtures.PrivateField"
java_import "java_integration.fixtures.ProtectedField"
java_import "java_integration.fixtures.PublicField"
java_import "java_integration.fixtures.PackageField"

describe "A JavaClass" do
  it "should provide a look up for fields using a Java formatted name" do
    PrivateField.java_class.declared_field(:strField).should_not == nil
    ProtectedField.java_class.declared_field(:strField).should_not == nil
    PublicField.java_class.declared_field(:strField).should_not == nil
    PackageField.java_class.declared_field(:strField).should_not == nil
  end

  it "should provide a look up for a fields using a Ruby formatted name" do
    PrivateField.java_class.declared_field(:str_field).should_not == nil
    ProtectedField.java_class.declared_field(:str_field).should_not == nil
    PublicField.java_class.declared_field(:str_field).should_not == nil
    PackageField.java_class.declared_field(:str_field).should_not == nil
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
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        @field.value(@obj.java_object).should == "42"

        @field.set_value @obj.java_object, nil
        @field.value(@obj.java_object).should be_nil
      end
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        @field.value(@obj.java_object).should == "42"

        @field.set_value @obj.java_object, nil
        @field.value(@obj.java_object).should be_nil
      end
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil.to_java }.should_not raise_error
      end


      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        @field.value(@obj.java_object).should == "42"

        @field.set_value @obj.java_object, nil
        @field.value(@obj.java_object).should be_nil
      end
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj.java_object, "42" }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj.java_object, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj.java_object, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        @field.value(@obj.java_object).should == "42"

        @field.set_value @obj.java_object, nil
        @field.value(@obj.java_object).should be_nil
      end
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
        lambda { @field.set_value @obj, "42" }.should_not raise_error
        lambda { @field.set_value @obj, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        @field.value(@obj).should == "42"

        @field.set_value @obj, nil
        @field.value(@obj).should be_nil
      end
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj, "42" }.should_not raise_error
        lambda { @field.set_value @obj, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        @field.value(@obj).should == "42"

        @field.set_value @obj, nil
        @field.value(@obj).should be_nil
      end
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj, "42" }.should_not raise_error
        lambda { @field.set_value @obj, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        @field.value(@obj).should == "42"

        @field.set_value @obj, nil
        @field.value(@obj).should be_nil
      end
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        lambda { @field.set_value @obj, "42" }.should_not raise_error
        lambda { @field.set_value @obj, nil }.should_not raise_error
      end

      it "should set Java values" do
        lambda { @field.set_value @obj, "42".to_java }.should_not raise_error
        lambda { @field.set_value @obj, nil.to_java }.should_not raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        @field.value(@obj).should == "42"

        @field.set_value @obj, nil
        @field.value(@obj).should be_nil
      end
    end
  end
end
