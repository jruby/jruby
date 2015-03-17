require File.expand_path('../../../shared/rational/divide', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#quo" do
    it_behaves_like(:rational_divide, :quo)
  end
end
