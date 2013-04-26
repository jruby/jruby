require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/equal', __FILE__)

ruby_version_is ""..."2.0" do
  describe "Proc#==" do
    it_behaves_like(:proc_equal, :==)
  end
end
