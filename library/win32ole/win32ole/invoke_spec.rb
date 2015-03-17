platform_is :windows do
  require 'win32ole'

  describe "WIN32OLE#invoke" do
    before :each do
      @ie = WIN32OLE.new 'InternetExplorer.application'
    end

    it "get name by invoking 'Name' OLE method" do
      @ie.invoke('Name').should =~ /explorer/i
    end
  end
end
