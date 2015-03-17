require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

ruby_version_is ''...'2.2' do
  describe "Logger::Application.new" do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
    end

    after :each do
      @log_file.close unless @log_file.closed?
      rm_r @file_path
    end

    it "starts the logger on a new application" do
      LoggerSpecs::TestApp.new("TestApp", @log_file).start
      @log_file.rewind            # go back to the beginning to read the contents

      first, second, third = @log_file.readlines
      LoggerSpecs::strip_date(first).should  == "INFO -- TestApp: Start of TestApp.\n"
      LoggerSpecs::strip_date(second).should == "WARN -- TestApp: Test log message\n"
      LoggerSpecs::strip_date(third).should  == "INFO -- TestApp: End of TestApp. (status: true)\n"
    end

    it "defaults application name to ''" do
      LoggerSpecs::TestApp.new(nil, @log_file).start
      @log_file.rewind

      first, second, third =  @log_file.readlines
      LoggerSpecs::strip_date(first).should  == "INFO -- : Start of .\n"
      LoggerSpecs::strip_date(second).should == "WARN -- : Test log message\n"
      LoggerSpecs::strip_date(third).should  == "INFO -- : End of . (status: true)\n"
    end

    it "defaults logs to STDERR" do
      regex = /INFO.*WARN.*INFO.*/m
      lambda { LoggerSpecs::TestApp.new(nil, nil).start }.should output_to_fd(regex, STDERR)
      @log_file.rewind
    end
  end
end
