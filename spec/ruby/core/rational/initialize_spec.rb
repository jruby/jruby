ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/initialize', __FILE__)

  describe "Rational#initialize" do
    it_behaves_like(:rational_initialize, :initialize)
  end
end
