require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv.charset_map" do
    it "acts as a map" do
      Iconv.charset_map.respond_to?(:[]).should be_true
      Iconv.charset_map.respond_to?(:include?).should be_true
      Iconv.charset_map.respond_to?(:to_hash).should be_true

      Iconv.charset_map.include?("x-nonexistent-encoding").should be_false
    end

    #  it "maps from canonical name to system dependent name" do
    #  end

    it "returns nil when given an unknown encoding name" do
      Iconv.charset_map["x-nonexistent-encoding"].should be_nil
    end
  end
end
