require File.expand_path('../../../shared/rational/numerator', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#numerator" do
    it_behaves_like(:rational_numerator, :numerator)
  end
end
