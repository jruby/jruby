require File.expand_path('../../../spec_helper', __FILE__)

describe "Resolv#getname" do
  before(:all) do
    require 'resolv'
  end

  it "resolves 127.0.0.1" do
    lambda {
      Resolv.getname("127.0.0.1")
    }.should_not raise_error(Resolv::ResolvError)
  end

  it "raises ResolvError when there is no result" do
    res = Resolv.new([])
    lambda {
      res.getname("should.raise.error")
    }.should raise_error(Resolv::ResolvError)
  end

end
