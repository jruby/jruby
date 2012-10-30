ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/divide', __FILE__)

  describe "Rational#quo" do
    it_behaves_like(:rational_divide, :quo)
  end
end
