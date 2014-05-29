require File.expand_path('../../../shared/enumerator/each', __FILE__)

ruby_version_is "1.8.7"..."1.9" do
  describe "Enumerator#each" do
    it_behaves_like(:enum_each, :each)
  end
end
