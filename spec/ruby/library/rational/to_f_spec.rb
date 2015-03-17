require File.expand_path('../../../shared/rational/to_f', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#to_f" do
    it_behaves_like(:rational_to_f, :to_f)
  end
end
