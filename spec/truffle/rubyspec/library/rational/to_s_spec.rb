require File.expand_path('../../../shared/rational/to_s', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#to_s" do
    it_behaves_like(:rational_to_s, :to_s)
  end
end
