if RUBY_VERSION >= "1.9"
  describe "A string encoded as 'UTF-32'" do
    it "transcodes as UTF-32BE" do
      a = "a"
      a32 = a.encode("UTF-32")
      a8 = a32.encode(Encoding::UTF_8)
      expect(a8).to eq(a)
    end
  end
end
