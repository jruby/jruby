require File.dirname(__FILE__) + "/../spec_helper"

java_import java.lang.reflect.Field
java_import "java_integration.fixtures.PrivateField"
java_import "java_integration.fixtures.ProtectedField"
java_import "java_integration.fixtures.PublicField"
java_import "java_integration.fixtures.PackageField"

describe "A JavaClass" do
  it "should provide a look up for fields using a Java formatted name" do
    expect(PrivateField.java_class.declared_field(:strField)).not_to eq(nil)
    expect(ProtectedField.java_class.declared_field(:strField)).not_to eq(nil)
    expect(PublicField.java_class.declared_field(:strField)).not_to eq(nil)
    expect(PackageField.java_class.declared_field(:strField)).not_to eq(nil)
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
        expect { @field.set_value @obj.java_object, "42" }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj.java_object, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        expect(@field.value(@obj.java_object)).to eq("42")

        @field.set_value @obj.java_object, nil
        expect(@field.value(@obj.java_object)).to be_nil
      end
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj.java_object, "42" }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj.java_object, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        expect(@field.value(@obj.java_object)).to eq("42")

        @field.set_value @obj.java_object, nil
        expect(@field.value(@obj.java_object)).to be_nil
      end
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj.java_object, "42" }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj.java_object, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil.to_java }.not_to raise_error
      end


      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        expect(@field.value(@obj.java_object)).to eq("42")

        @field.set_value @obj.java_object, nil
        expect(@field.value(@obj.java_object)).to be_nil
      end
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj.java_object, "42" }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj.java_object, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj.java_object, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj.java_object, "42"
        expect(@field.value(@obj.java_object)).to eq("42")

        @field.set_value @obj.java_object, nil
        expect(@field.value(@obj.java_object)).to be_nil
      end

      it "should set/get value directly" do
        @field.set_value @obj, "aaa"
        expect( @field.value(@obj) ).to eq("aaa")
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
        expect { @field.set_value @obj, "42" }.not_to raise_error
        expect { @field.set_value @obj, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        expect(@field.value(@obj)).to eq("42")

        @field.set_value @obj, nil
        expect(@field.value(@obj)).to be_nil
      end
    end
    
    describe "with a protected field" do
      before(:each) do
        @obj = ProtectedField.new
        @field = ProtectedField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj, "42" }.not_to raise_error
        expect { @field.set_value @obj, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        expect(@field.value(@obj)).to eq("42")

        @field.set_value @obj, nil
        expect(@field.value(@obj)).to be_nil
      end
    end
    
    describe "with a public field" do
      before(:each) do
        @obj = PublicField.new
        @field = PublicField.java_class.declared_field :strField
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj, "42" }.not_to raise_error
        expect { @field.set_value @obj, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        expect(@field.value(@obj)).to eq("42")

        @field.set_value @obj, nil
        expect(@field.value(@obj)).to be_nil
      end
    end
    
    describe "with a package field" do
      before(:each) do
        @obj = PackageField.new
        @field = PackageField.java_class.declared_field :strField
        @field.accessible = true
      end

      it "should set Ruby values" do
        expect { @field.set_value @obj, "42" }.not_to raise_error
        expect { @field.set_value @obj, nil }.not_to raise_error
      end

      it "should set Java values" do
        expect { @field.set_value @obj, "42".to_java }.not_to raise_error
        expect { @field.set_value @obj, nil.to_java }.not_to raise_error
      end

      it "should get Ruby values" do 
        @field.set_value @obj, "42"
        expect(@field.value(@obj)).to eq("42")

        @field.set_value @obj, nil
        expect(@field.value(@obj)).to be_nil
      end
    end
  end
end
