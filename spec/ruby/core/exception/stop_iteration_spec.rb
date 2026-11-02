require_relative '../../spec_helper'

describe "StopIteration#dup" do
  it "copies the result" do
    enum = [1].each
    enum.next
    -> { enum.next }.should.raise(StopIteration) { |error|
      error.dup.result.should == [1]
    }
  end
end
