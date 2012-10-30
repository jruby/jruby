require File.expand_path('../../../spec_helper', __FILE__)

describe "Redefining #bactrace on an instance of Exception" do
  it "does not affect the behaviour of Exception#message" do
    e = Exception.new
    e.message.should == 'Exception'
    
    def e.backtrace; []; end
    e.message.should == 'Exception'
  end
end
