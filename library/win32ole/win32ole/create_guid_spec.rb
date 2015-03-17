platform_is :windows do
  require 'win32ole'

  ruby_version_is "1.9" do
    describe "WIN32OLE.create_guid" do
      it "generates guid with valid format" do
        WIN32OLE.create_guid.should =~ /^\{[A-Z0-9]{8}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{12}/
      end
    end
  end
end
