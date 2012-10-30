require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require 'enumerator'
  require File.expand_path('../../../shared/enumerator/enum_cons', __FILE__)

  describe "Enumerator#enum_cons" do
    it_behaves_like(:enum_cons, :enum_cons)
  end
end
