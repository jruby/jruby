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
        expect {
          base_cls.new.field
        }.to raise_error NoMethodError

        expect {
          obj = @cls.new
          expect(obj.field).to eq("1764")
          obj.field = "foo"
          expect(obj.field).to eq("foo")

          expect(@cls.field_static).to eq("1764")
          @cls.field_static = "foo"
          expect(@cls.field_static).to eq("foo")
        }.not_to raise_error
      end
    end
    
  end

  it "throws an error for a field which does not exist" do
    expect {
      class PackageField
        field_accessor(:totallyBogus)
      end
    }.to raise_error(NameError)
    
    expect {
      class PackageField
        field_accessor(:strField, :totallyBogus)
      end
    }.to raise_error(NameError)
  end

  it "fails on final fields" do
    expect {
      class PrivateField
        field_accessor(:finalStrField)
      end
    }.to raise_error(SecurityError)
  end
end
