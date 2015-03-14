require 'stringio'

ruby_version_is "1.9.2" do
  describe "StringIO#external_encoding" do
    it "gets the encoding of the underlying String" do
      io = StringIO.new
      io.set_encoding Encoding::UTF_8
      io.external_encoding.should == Encoding::UTF_8
    end
  end
end
