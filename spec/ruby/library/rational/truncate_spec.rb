require File.expand_path('../../../shared/rational/truncate', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#truncate" do
    it_behaves_like(:rational_truncate, :truncate)
  end
end
