require File.expand_path('../../../shared/rational/remainder', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#remainder" do
    it_behaves_like(:rational_remainder, :remainder)
  end
end
