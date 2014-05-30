require File.expand_path('../../../shared/complex/numerator', __FILE__)

ruby_version_is "1.9" do
  describe "Complex#numerator" do
    it_behaves_like(:complex_numerator, :numerator)
  end
end
