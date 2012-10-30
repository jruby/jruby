require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../shared/join', __FILE__)
require 'uri'

ruby_version_is "1.9.2" do
  describe "URI::Parser#join" do
    it_behaves_like :uri_join, :join, URI::Parser.new
  end
end
