ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/numerator', __FILE__)

  describe "Rational#numerator" do
    it_behaves_like(:rational_numerator, :numerator)
  end
end
