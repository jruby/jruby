require File.expand_path('../../../shared/rational/modulo', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#%" do
    it_behaves_like(:rational_modulo, :%)
  end
end
