require File.expand_path('../../../shared/rational/coerce', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#coerce" do
    it_behaves_like(:rational_coerce, :coerce)
  end
end
