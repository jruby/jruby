require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/complex/hash', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#hash" do
    it_behaves_like(:complex_hash, :hash)
  end
end
