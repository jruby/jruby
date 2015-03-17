require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/sec', __FILE__)

ruby_version_is "1.9" do
  describe "DateTime#second" do
    it_behaves_like :datetime_sec, :second
  end
end
