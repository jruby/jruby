if RUBY_VERSION > "1.9"
  describe "File.basename when given a path with non-default encoding" do
    it "produces the basename path with the same encoding" do
      expect(File.basename('/foo/bar'.force_encoding('Windows-31J')).encoding).to eq Encoding::Windows_31J
    end
  end
end