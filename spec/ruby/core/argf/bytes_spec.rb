require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/each_byte', __FILE__)

ruby_version_is "1.8.7" do
  describe "ARGF.bytes" do
    it_behaves_like :argf_each_byte, :bytes
  end
end
