require File.expand_path('../../../spec_helper', __FILE__)

with_feature :continuation_library do
  require 'continuation'

  describe "Continuation.new" do
    it "raises a NoMethodError" do
      lambda { Continuation.new }.should raise_error(NoMethodError)
    end
  end
end
