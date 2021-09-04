require 'open3'

if RUBY_VERSION > '1.9'
  describe "Open3.popen3" do
    it "accepts and ignores empty trailing options hash" do
      # capture3 is an easy way to test popen3 options behavior
      result = Open3.capture3('echo')

      expect(result[0]).to eq "\n"
    end
  end
end