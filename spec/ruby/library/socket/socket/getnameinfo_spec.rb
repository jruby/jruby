require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

require 'socket'

describe "Socket.getnameinfo" do
  before :all do
    @reverse_lookup = BasicSocket.do_not_reverse_lookup
    BasicSocket.do_not_reverse_lookup = true
  end

  after :all do
    BasicSocket.do_not_reverse_lookup = @reverse_lookup
  end

  it "gets the name information and don't resolve it" do
    sockaddr = Socket.sockaddr_in SocketSpecs.port, '127.0.0.1'
    name_info = Socket.getnameinfo(sockaddr, Socket::NI_NUMERICHOST | Socket::NI_NUMERICSERV)
    name_info.should == ['127.0.0.1', "#{SocketSpecs.port}"]
  end

  it "gets the name information and resolve the host" do
    sockaddr = Socket.sockaddr_in SocketSpecs.port, '127.0.0.1'
    name_info = Socket.getnameinfo(sockaddr, Socket::NI_NUMERICSERV)
    name_info[0].should be_valid_DNS_name
    name_info[1].should == SocketSpecs.port.to_s
  end

  it "gets the name information and resolves the service" do
    sockaddr = Socket.sockaddr_in 80, '127.0.0.1'
    name_info = Socket.getnameinfo(sockaddr)
    name_info.size.should == 2
    name_info[0].should be_valid_DNS_name
    # see http://www.iana.org/assignments/port-numbers
    name_info[1].should =~ /^(www|http|www-http)$/
  end

  it "gets a 3-element array and doesn't resolve hostname" do
    name_info = Socket.getnameinfo(["AF_INET", SocketSpecs.port, '127.0.0.1'], Socket::NI_NUMERICHOST | Socket::NI_NUMERICSERV)
    name_info.should == ['127.0.0.1', "#{SocketSpecs.port}"]
  end

  it "gets a 3-element array and resolves the service" do
    name_info = Socket.getnameinfo ["AF_INET", 80, '127.0.0.1']
    name_info[1].should =~ /^(www|http|www-http)$/
  end

  it "gets a 4-element array and doesn't resolve hostname" do
    name_info = Socket.getnameinfo(["AF_INET", SocketSpecs.port, 'foo', '127.0.0.1'], Socket::NI_NUMERICHOST | Socket::NI_NUMERICSERV)
    name_info.should == ['127.0.0.1', "#{SocketSpecs.port}"]
  end

  it "gets a 4-element array and resolves the service" do
    name_info = Socket.getnameinfo ["AF_INET", 80, 'foo', '127.0.0.1']
    name_info[1].should =~ /^(www|http|www-http)$/
  end

end
