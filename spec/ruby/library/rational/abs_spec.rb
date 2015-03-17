require File.expand_path('../../../shared/rational/abs', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#abs" do
    it_behaves_like(:rational_abs, :abs)
  end
end
