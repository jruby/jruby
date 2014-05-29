require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/eql', __FILE__)

ruby_version_is "1.9" do
  describe "Method#eql?" do
    it_behaves_like(:method_equal, :eql?)
  end
end
