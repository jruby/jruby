require File.expand_path('../../../shared/rational/to_r', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#to_r" do
    it_behaves_like(:rational_to_r, :to_r)
  end
end
