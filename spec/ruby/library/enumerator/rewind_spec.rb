require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7"..."1.9" do
  require File.expand_path('../../../shared/enumerator/rewind', __FILE__)
  require 'enumerator'

  describe "Enumerator#rewind" do
    it_behaves_like(:enum_rewind, :rewind)
  end
end
