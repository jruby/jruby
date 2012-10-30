require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/initialize_exceptions', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv.conv" do
    it_behaves_like :iconv_initialize_exceptions, :conv, "test"

    it "acts exactly as if opening a converter and invoking #iconv once" do
      Iconv.conv("utf-8", "iso-8859-1", "expos\xe9").should == encode("expos\xc3\xa9", "utf-8")

      str = mock("string-like")
      str.should_receive(:to_str).and_return("cacha\xc3\xa7a")
      Iconv.conv("iso-8859-1", "utf-8", str).should == encode("cacha\xe7a", "iso-8859-1")

      Iconv.conv("utf-16", "us-ascii", "a").should equal_utf16("\xfe\xff\0a")
      # each call is completely independent; never retain context!
      Iconv.conv("utf-16", "us-ascii", "b").should equal_utf16("\xfe\xff\0b")

      Iconv.conv("us-ascii", "iso-8859-1", nil).should == ""

      Iconv.conv("utf-16", "utf-8", "").should == ""

      lambda do
        Iconv.conv("utf-8", "utf-8", "test\xff")
      end.should raise_error(Iconv::IllegalSequence)

      lambda do
        Iconv.conv("utf-8", "utf-8", "euro \xe2")
      end.should raise_error(Iconv::InvalidCharacter)
    end
  end
end
