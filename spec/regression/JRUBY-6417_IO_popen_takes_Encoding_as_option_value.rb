if defined? Encoding
  describe "JRUBY-6417: IO.popen() when given a hash with an Encoding as a value" do
    it "does not throw a TypeError" do
      result = IO.popen("ls", {:internal_encoding => Encoding::UTF_8})
      result.should be_kind_of IO
    end
  end
end

