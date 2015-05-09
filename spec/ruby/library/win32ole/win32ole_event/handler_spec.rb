platform_is :windows do
  require 'win32ole'

  # TODO: need specs here
  describe "WIN32OLE_EVENT#handler" do
    before :each do
      @ole = WIN32OLE.new('InternetExplorer.Application')
      @event = ''
    end

    after :each do
      @ole = nil
    end

  end

  describe "WIN32OLE_EVENT#handler=" do
    before :each do
      @ole = WIN32OLE.new('InternetExplorer.Application')
      @event = ''
    end

    after :each do
      @ole = nil
    end

  end
end
