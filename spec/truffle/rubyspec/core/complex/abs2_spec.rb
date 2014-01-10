ruby_version_is "1.9" do

  require File.expand_path('../../../shared/complex/abs2', __FILE__)

  describe "Complex#abs2" do
    it_behaves_like(:complex_abs2, :abs2)
  end
end
