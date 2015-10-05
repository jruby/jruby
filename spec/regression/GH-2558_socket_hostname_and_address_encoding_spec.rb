require 'socket'

if RUBY_VERSION >= '1.9'
  describe "Socket.gethostname" do
    it "returns current host name with UTF-8 encoding" do
      expect(Socket.gethostname.encoding).to eq(Encoding::UTF_8)
    end
  end

  describe "IPSocket.getaddress" do
    it "returns hostname for specified address with UTF-8 encoding" do
      expect(IPSocket.getaddress('localhost').encoding).to eq(Encoding::UTF_8)
    end
  end
end
