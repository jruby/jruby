require File.expand_path('../../../shared/complex/real', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#real" do
    it_behaves_like(:complex_real, :real)
  end
end
