require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

ruby_version_is "1.9.2" do
  describe "Socket::Option.int" do
    it "creates a new Socket::Option for SO_LINGER" do
      so = Socket::Option.int(:INET, :SOCKET, :KEEPALIVE, 5)
      so.family.should == Socket::Constants::AF_INET
      so.level.should == Socket::Constants::SOL_SOCKET
      so.optname.should == Socket::Constants::SO_KEEPALIVE
      so.data.should == [5].pack('i')
    end
  end

  describe "Socket::Option#int" do
    it "returns int option" do
      so = Socket::Option.int(:INET, :SOCKET, :KEEPALIVE, 17)
      so.int.should == 17

      so = Socket::Option.int(:INET, :SOCKET, :KEEPALIVE, 32765)
      so.int.should == 32765
    end
    it "raises TypeError if option has not good size" do
      so = Socket::Option.new(:UNSPEC, :SOCKET, :KEEPALIVE, [0, 0].pack('i*'))
      lambda { so.int }.should raise_error(TypeError)
    end
  end
end
