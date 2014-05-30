require File.expand_path('../../fixtures/common', __FILE__)

describe :to_s, :shared => true do

  it "returns the self's name if no message is set" do
    Exception.new.send(@method).should == 'Exception'
    ExceptionSpecs::Exceptional.new.send(@method).should == 'ExceptionSpecs::Exceptional'
  end

  it "returns self's message if set" do
    ExceptionSpecs::Exceptional.new('!!').send(@method).should == '!!'
  end

  ruby_version_is "1.9.3" do
    it "calls #to_s on the message" do
      message = mock("message")
      message.should_receive(:to_s).and_return("message")
      ExceptionSpecs::Exceptional.new(message).send(@method).should == "message"
    end
  end

end
