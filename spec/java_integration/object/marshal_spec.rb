require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.util.ArrayList"

describe "A Java object" do
  it "marshals as custom Ruby marshal data" do
    list = ArrayList.new
    list << 'foo'

    hash = {:foo => list}

    marshaled = Marshal.load(Marshal.dump(hash))
    expect(marshaled[:foo].class).to eq(ArrayList)
    expect(marshaled[:foo][0]).to eq('foo')
  end
end
