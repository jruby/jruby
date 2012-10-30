ruby_version_is "1.9" do
  require File.expand_path('../../../shared/rational/ceil', __FILE__)

  describe "Rational#ceil" do
    it_behaves_like(:rational_ceil, :ceil)
  end
end
