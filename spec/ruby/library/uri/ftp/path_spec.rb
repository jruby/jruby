require File.expand_path('../../../../spec_helper', __FILE__)
require 'uri'

describe "URI::FTP#path=" do
  before :each do
    @url = URI.parse('ftp://example.com')
  end

  ruby_version_is "1.8.6"..."1.8.7" do
    it "does not strip the leading /" do
      @url.path = '/foo'
      @url.path.should == '/foo'
    end
  end

  ruby_version_is "1.8.7"..."1.9" do
    it "requires a leading /" do
      lambda { @url.path = 'foo' }.should raise_error(URI::InvalidComponentError)
    end

    it "strips the leading /" do
      @url.path = '/foo'
      @url.path.should == 'foo'
    end
  end

  ruby_version_is "1.9" do
    it "does not require a leading /" do
      @url.path = 'foo'
      @url.path.should == 'foo'
    end

    it "does not strip the leading /" do
      @url.path = '/foo'
      @url.path.should == '/foo'
    end
  end
end

describe "URI::FTP#path" do
  ruby_version_is "1.8.6"..."1.8.7" do
    it "copies the path section of the URI without modification" do
      url = URI.parse('ftp://example.com/%2Ffoo')

      url.path.should == '/%2Ffoo'
    end
  end

  ruby_version_is "1.8.7" do
    it "unescapes the leading /" do
      url = URI.parse('ftp://example.com/%2Ffoo')

      url.path.should == '/foo'
    end
  end
end
