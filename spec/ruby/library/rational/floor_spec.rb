require File.expand_path('../../../shared/rational/floor', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#floor" do
    it_behaves_like(:rational_floor, :floor)
  end
end
