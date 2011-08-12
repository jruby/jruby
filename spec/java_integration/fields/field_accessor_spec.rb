require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.PrivateField"
java_import "java_integration.fixtures.ProtectedField"
java_import "java_integration.fixtures.PublicField"
java_import "java_integration.fixtures.PackageField"

describe "field_accessor" do
  {
    :public => PublicField,
    :protected => ProtectedField,
    :package => PackageField,
    :private => PrivateField
  }.each do |visibility, base_cls|
    
    describe "for #{visibility} fields" do
      before :each do
        @cls = base_cls.clone
        @cls.field_accessor :strField => :field
        @cls.field_accessor :strFieldStatic => :field_static
      end

      it "makes those fields accessible as Ruby instance methods" do
        lambda {
          base_cls.new.field
        }.should raise_error NoMethodError

        lambda {
          obj = @cls.new
          obj.field.should == "1764"
          obj.field = "foo"
          obj.field.should == "foo"

          @cls.field_static.should == "1764"
          @cls.field_static = "foo"
          @cls.field_static.should == "foo"
        }.should_not raise_error
      end
    end
    
  end

  it "throws an error for a field which does not exist" do
    lambda {
      class PackageField
        field_accessor(:totallyBogus)
      end
    }.should raise_error
    
    lambda {
      class PackageField
        field_accessor(:strField, :totallyBogus)
      end
    }.should raise_error
  end

  it "fails on final fields" do
    lambda {
      class PrivateField
        field_accessor(:finalStrField)
      end
    }.should raise_error
  end
end
