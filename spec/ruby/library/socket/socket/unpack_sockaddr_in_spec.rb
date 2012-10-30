require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)
require 'socket'

describe "Socket#unpack_sockaddr_in" do

  it "decodes the host name and port number of a packed sockaddr_in" do
    sockaddr = Socket.sockaddr_in SocketSpecs.port, '127.0.0.1'
    Socket.unpack_sockaddr_in(sockaddr).should == [SocketSpecs.port, '127.0.0.1']
  end

  it "raises an ArgumentError when the sin_family is not AF_INET" do
    sockaddr = Socket.sockaddr_un '/tmp/x'
    lambda { Socket.unpack_sockaddr_in sockaddr }.should raise_error(ArgumentError)
  end

end
