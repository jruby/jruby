require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/continuation/call', __FILE__)

with_feature :continuation_library do
  require 'continuation'

  describe "Continuation#call" do
    it_behaves_like :continuation_call, :[]
  end
end
