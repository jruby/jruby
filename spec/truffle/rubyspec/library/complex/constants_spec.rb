require File.expand_path('../../../shared/complex/constants', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex::I" do
    it_behaves_like :complex_I, :I
  end
end
