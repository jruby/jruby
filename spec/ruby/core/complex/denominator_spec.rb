
ruby_version_is "1.9" do

  require File.expand_path('../../../shared/complex/denominator', __FILE__)

  describe "Complex#denominator" do
    it_behaves_like(:complex_denominator, :denominator)
  end
end
