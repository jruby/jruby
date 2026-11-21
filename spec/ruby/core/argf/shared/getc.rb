describe :argf_getc, shared: true do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
    @multibyte = fixture __FILE__, "multibyte.txt"

    @chars  = File.read @file1
    @chars += File.read @file2
  end

  it "reads each char of files" do
    argf [@file1, @file2] do
      chars = +""
      @chars.size.times { chars << @argf.send(@method) }
      chars.should == @chars
    end
  end

  it "returns each character as a String" do
    argf [@multibyte] do
      @argf.set_encoding(Encoding::UTF_8)
      chars = 4.times.map { @argf.send(@method) }
      chars.should == ["é", "t", "é", "€"]
    end
  end
end
