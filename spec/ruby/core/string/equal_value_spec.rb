require_relative '../../spec_helper'
require_relative 'shared/eql'

describe "String#==" do
  it_behaves_like :string_eql_value, :==

  it "returns false if obj does not respond to to_str" do
    ('hello' == 5).should == false
    not_supported_on :opal do
      ('hello' == :hello).should == false
    end
    ('hello' == mock('x')).should == false
  end

  it "returns obj == self if obj responds to to_str" do
    obj = Object.new

    # String#== merely checks if #to_str is defined. It does
    # not call it.
    obj.stub!(:to_str)

    obj.should_receive(:==).and_return(true)

    ('hello' == obj).should == true
  end

  it "returns false for a BasicObject" do
    ('hello' == BasicObject.new).should == false
  end

  it "asks respond_to_missing? of an object without respond_to?" do
    obj = Class.new(BasicObject) do
      def respond_to_missing?(name, include_private)
        name == :to_str
      end

      def ==(other)
        true
      end
    end.new

    ('hello' == obj).should == true
  end

  it "is not fooled by NUL characters" do
    ("abc\0def" == "abc\0xyz").should == false
  end
end
