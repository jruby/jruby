require File.expand_path('../../../shared/complex/exponent', __FILE__)

ruby_version_is ""..."1.9" do
  require "complex"
  require "rational"

  describe "Complex#**" do
    it_behaves_like :complex_exponent, :**
  end
end
