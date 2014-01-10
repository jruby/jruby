ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/denominator', __FILE__)

  describe "Rational#denominator" do
    it_behaves_like(:rational_denominator, :denominator)
  end
end
