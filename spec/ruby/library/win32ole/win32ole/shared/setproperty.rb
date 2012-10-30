platform_is :windows do
  require 'win32ole'

  describe :win32ole_setproperty, :shared => true do
    before :each do
      @ie = WIN32OLE.new("InternetExplorer.application")
    end

    it "raises ArgumentError if no argument is given" do
      lambda { @ie.send(@method) }.should raise_error ArgumentError
    end

    it "sets height to 500 and returns nil" do
      height = 500
      result = @ie.send(@method, 'Height', height)
      result.should == nil
      ruby_version_is ""..."1.9" do # 1.9 does not support collection for WIN32OLE objects
        @ie['Height'].should == height
      end
    end

  end

end
