require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

ruby_version_is ''...'2.2' do
  describe "Logger::Application#log" do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
      @app = LoggerSpecs::TestApp.new("TestApp", @log_file)
      @app.start
    end

    after :each do
      @log_file.close unless @log_file.closed?
      rm_r @file_path
    end

    it "logs a message" do
      @app.log(Logger::WARN, "Test message")
      @log_file.rewind
      message = @log_file.readlines.last
      LoggerSpecs.strip_date(message).should == "WARN -- TestApp: Test message\n"
    end

    it "receives a severity" do
      @app.log(Logger::INFO,  "Info message")
      @app.log(Logger::DEBUG, "Debug message")
      @app.log(Logger::WARN,  "Warn message")
      @app.log(Logger::ERROR, "Error message")
      @app.log(Logger::FATAL, "Fatal message")
      @log_file.rewind
      messages = @log_file.readlines[3..-1] # remove default messages

      LoggerSpecs.strip_date(messages[0]).should == "INFO -- TestApp: Info message\n"
      LoggerSpecs.strip_date(messages[1]).should == "DEBUG -- TestApp: Debug message\n"
      LoggerSpecs.strip_date(messages[2]).should == "WARN -- TestApp: Warn message\n"
      LoggerSpecs.strip_date(messages[3]).should == "ERROR -- TestApp: Error message\n"
      LoggerSpecs.strip_date(messages[4]).should == "FATAL -- TestApp: Fatal message\n"
    end

    it "uses app name for Application Name" do
      @app.log(Logger::INFO,  "Info message")
      @log_file.rewind
      test_message = @log_file.readlines.last
      Regexp.new(/TestApp/).should =~ LoggerSpecs.strip_date(test_message)
    end

    it "receives a block and calls it if message is nil" do
      temp = 0
      @app.log(Logger::INFO, nil) { temp = 1 }
      temp.should == 1
    end
  end

  describe "Logger::Application#log=" do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
      @app = LoggerSpecs::TestApp.new("TestApp", @log_file)
      @app.start
    end

    after :each do
      @log_file.close
      rm_r @file_path
    end

    it "sets the log device" do
      regex = /STDERR Message/
      @app.log = STDERR
      lambda { @app.log(Logger::WARN, "STDERR Message") }.should output_to_fd(regex, STDERR)
    end
  end
end
