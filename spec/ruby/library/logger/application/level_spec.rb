require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

ruby_version_is ''...'2.2' do
  describe "Logger::Application#level=" do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
      @app = LoggerSpecs::TestApp.new("TestApp", @log_file)

    end

    after :each do
      @log_file.close unless @log_file.closed?
      rm_r @file_path
    end

    it "sets the logging threshold" do
      @app.level = Logger::ERROR
      @app.start
      @app.log(Logger::WARN, "Don't show me")
      @app.log(Logger::ERROR, "Show me")
      @log_file.rewind
      messages = @log_file.readlines
      messages.length.should == 1
      LoggerSpecs::strip_date(messages.first).should == "ERROR -- TestApp: Show me\n"
    end

    it "can set the threshold to unknown values" do
      @app.level = 10
      @app.start
      @app.log(Logger::INFO,  "Info message")
      @app.log(Logger::DEBUG, "Debug message")
      @app.log(Logger::WARN,  "Warn message")
      @app.log(Logger::ERROR, "Error message")
      @app.log(Logger::FATAL, "Fatal message")
      @log_file.rewind
      @log_file.readlines.should be_empty
    end
  end
end
