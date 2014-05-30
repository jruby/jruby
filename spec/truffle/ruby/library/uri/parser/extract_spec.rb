require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../shared/extract', __FILE__)
require 'uri'

ruby_version_is "1.9.2" do
  describe "URI::Parser#extract" do
    it_behaves_like :uri_extract, :extract, URI::Parser.new
  end
end
