require File.expand_path('../../../shared/complex/inspect', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#inspect" do
    it_behaves_like(:complex_inspect, :inspect)
  end
end
