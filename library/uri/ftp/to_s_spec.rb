require File.expand_path('../../../../spec_helper', __FILE__)
require 'uri'


describe "URI::FTP#to_s" do
  before :each do
    @url = URI.parse('ftp://example.com')
  end

  ruby_version_is ""..."1.9" do
    it "does not escape the leading /" do
      @url.path = '//foo'

      @url.to_s.should == 'ftp://example.com//foo'
    end
  end

  ruby_version_is "1.9" do
    it "escapes the leading /" do
      @url.path = '/foo'

      @url.to_s.should == 'ftp://example.com/%2Ffoo'
    end
  end
end
