ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/fdiv', __FILE__)

  describe "Rational#fdiv" do
    it_behaves_like(:rational_fdiv, :fdiv)
  end
end
