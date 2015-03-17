require File.expand_path('../../../shared/complex/equal_value', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex#==" do
    it_behaves_like :complex_equal_value, :==
  end
end
