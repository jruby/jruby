require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

describe "Logger#close" do
  before :each do
    @path = tmp("test_log.log")
    @log_file = File.open(@path, "w+")
    @logger = Logger.new(@path)
  end

  after :each do
    @log_file.close unless @log_file.closed?
    rm_r @path
  end

  ruby_version_is "" ... "1.9" do
    it "closes the logging device" do
      @logger.close
      lambda { @logger.add(nil, "Foo") }.should raise_error(IOError)
    end

    it "fails when called on a closed device" do
      @logger.close
      lambda { @logger.close }.should raise_error(IOError)
    end
  end

  ruby_version_is "1.9" do
    it "closes the logging device" do
      @logger.close
      lambda { @logger.add(nil, "Foo") }.should complain(/\Alog writing failed\./)
    end
  end
end
