platform_is :windows do
  require 'win32ole'

  describe "WIN32OLE#_getproperty" do
    before :each do
      @ie = WIN32OLE.new 'InternetExplorer.application'
    end

    it "gets name" do
      @ie._getproperty(0, [], []).should =~ /explorer/i
    end

  end

end
