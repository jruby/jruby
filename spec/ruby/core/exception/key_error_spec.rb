require_relative '../../spec_helper'

describe "KeyError" do
  it "accepts :receiver and :key options" do
    receiver = mock("receiver")
    key = mock("key")

    error = KeyError.new(receiver: receiver, key: key)

    error.receiver.should == receiver
    error.key.should == key

    error = KeyError.new("message", receiver: receiver, key: key)

    error.message.should == "message"
    error.receiver.should == receiver
    error.key.should == key
  end
end

describe "KeyError#dup" do
  it "copies the receiver and key" do
    error = KeyError.new("key", receiver: :receiver, key: :key).dup
    error.receiver.should == :receiver
    error.key.should == :key
  end
end
