platform_is :windows do
  require 'win32ole'

  # TODO Add an expectation!
  describe "WIN32OLE_EVENT#off_event" do
    before :each do
      @ole = WIN32OLE.new('InternetExplorer.Application')
      @event = ''
    end

    after :each do
      @ole = nil
    end

  end
end
