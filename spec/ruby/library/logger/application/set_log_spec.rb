require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

ruby_version_is ''...'2.2' do
  describe "Logger::Application#set_log"do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
      @app = LoggerSpecs::TestApp.new("TestApp", @log_file)
    end

    after :each do
      @log_file.close unless @log_file.closed?
      rm_r @file_path
    end

    it "sets the log device for the logger" do
      regex = /STDERR Message/
      @app.set_log(STDERR)
      lambda { @app.log(Logger::WARN, "STDERR Message") }.should output_to_fd(regex, STDERR)
    end
  end
end
