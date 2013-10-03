require 'rspec'
require 'delegate'

describe "SimpleDelegator" do
  it "passes block when object does not implement method" do
    hash = { "key" => "value" }

    class TestDelegator < SimpleDelegator
    end

    wrapped_hash = TestDelegator.new(hash)
    wrapped_wrapped_hash = TestDelegator.new(wrapped_hash)

    mapped_result = wrapped_wrapped_hash.map do |key, value|
      [key,value]
    end

    mapped_result.should == [["key", "value"]]
  end
end
