require File.expand_path('../../../shared/rational/ceil', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Rational#ceil" do
    it_behaves_like(:rational_ceil, :ceil)
  end
end
