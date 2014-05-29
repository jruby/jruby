ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/remainder', __FILE__)

  describe "Rational#remainder" do
    it_behaves_like(:rational_remainder, :remainder)
  end
end
