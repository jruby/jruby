require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.util.ArrayList"

describe "A Java object" do
  it "marshals as custom Ruby marshal data" do
    list = ArrayList.new
    list << 'foo'

    hash = {:foo => list}

    marshaled = Marshal.load(Marshal.dump(hash))
    marshaled[:foo].class.should == ArrayList
    marshaled[:foo][0].should == 'foo'
  end
end
