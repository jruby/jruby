require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)

ruby_version_is ''...'2.2' do
  describe "Logger::Application#start" do
    before :each do
      @file_path = tmp("test_log.log")
      @log_file = File.open(@file_path, "w+")
      @app = LoggerSpecs::TestApp.new("TestApp", @log_file)
    end

    after :each do
      @log_file.close unless @log_file.closed?
      rm_r @file_path
    end


    it "starts the application logging start/end messages" do
      @app.start
      @log_file.rewind
      app_start, discard, app_end  = @log_file.readlines
      LoggerSpecs::strip_date(app_start).should == "INFO -- TestApp: Start of TestApp.\n"
      LoggerSpecs::strip_date(app_end).should   == "INFO -- TestApp: End of TestApp. (status: true)\n"
    end

    it "returns the status code" do
      code = @app.start
      @log_file.rewind
      app_end  = @log_file.readlines.last
      /true/.should =~ LoggerSpecs::strip_date(app_end)
      code.should == true
    end

  end
end

