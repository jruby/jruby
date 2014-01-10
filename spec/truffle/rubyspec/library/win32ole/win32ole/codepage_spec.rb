platform_is :windows do
  require 'win32ole'

  describe "WIN32OLE.codepage" do
    # default value is influenced by Encoding.default_external in 1.9
    ruby_version_is ""..."1.9" do
      it "returns the current codepage" do
        WIN32OLE.codepage.should == WIN32OLE::CP_ACP
      end
    end
  end

  describe "WIN32OLE.codepage=" do
    it "sets codepage" do
      cp = WIN32OLE.codepage
      WIN32OLE.codepage = WIN32OLE::CP_UTF8
      WIN32OLE.codepage.should == WIN32OLE::CP_UTF8
      WIN32OLE.codepage = cp
    end
  end

end
