platform_is :windows do
  require 'win32ole'

  # TODO Add an expectation!
  ruby_version_is "1.9" do
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
end
