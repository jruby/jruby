ruby_version_is "1.9" do

  require File.expand_path('../../../shared/complex/abs', __FILE__)

  describe "Complex#magnitude" do
    it_behaves_like(:complex_abs, :magnitude)
  end
end
