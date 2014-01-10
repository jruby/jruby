require File.expand_path('../../../shared/complex/to_s', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex#to_s" do
    it_behaves_like(:complex_to_s, :to_s)
  end
end
