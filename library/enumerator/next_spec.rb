require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7"..."1.9" do
  require File.expand_path('../../../shared/enumerator/next', __FILE__)
  require 'enumerator'

  describe "Enumerator#next" do
    it_behaves_like(:enum_next,:next)
  end
end
