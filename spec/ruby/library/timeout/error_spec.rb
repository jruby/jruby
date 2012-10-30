require File.expand_path('../../../spec_helper', __FILE__)
require 'timeout'

describe "Timeout::Error" do
  ruby_version_is ""..."1.9" do
    it "is a subclass of Interrupt" do
      Interrupt.should be_ancestor_of(Timeout::Error)
    end
  end

  ruby_version_is "1.9" do
    it "is a subclass of RuntimeError" do
      RuntimeError.should be_ancestor_of(Timeout::Error)
    end
  end
end
