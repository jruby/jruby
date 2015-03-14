require 'stringio'

ruby_version_is "1.9.2" do
  describe "StringIO#set_encoding" do
    before :each do
      @default_external = Encoding.default_external
      Encoding.default_external = Encoding::UTF_8
    end

    after :each do
      Encoding.default_external = @default_external
    end

    it "sets the encoding of the underlying String to the specified encoding" do
      io = StringIO.new
      io.set_encoding Encoding::UTF_8
      io.string.encoding.should == Encoding::UTF_8
    end

    it "sets the encoding of the underlying String to the named encoding" do
      io = StringIO.new
      io.set_encoding "UTF-8"
      io.string.encoding.should == Encoding::UTF_8
    end

    it "sets the encoding of the underlying String to the default external encoding when passed nil" do
      Encoding.default_external = Encoding::UTF_8
      io = StringIO.new
      io.set_encoding nil
      io.string.encoding.should == Encoding::UTF_8
    end

    it "returns self" do
      io = StringIO.new
      io.set_encoding(Encoding::UTF_8).should equal(io)
    end
  end
end
