require_relative '../spec_helper'
require_relative '../fixtures/classes'

describe 'Addrinfo#getnameinfo' do
  describe 'using an IP Addrinfo' do
    SocketSpecs.each_ip_protocol do |family, ip_address|
      before do
        @addr = Addrinfo.tcp(ip_address, 21)
      end

      it 'returns the node and service names' do
        host, service = @addr.getnameinfo
        service.should == 'ftp'
      end

      it 'accepts flags as an Integer as the first argument' do
        host, service = @addr.getnameinfo(Socket::NI_NUMERICSERV)
        service.should == '21'
      end
    end

    it 'returns the service of the port for a SOCK_RAW Addrinfo' do
      addr = Addrinfo.new(Socket.sockaddr_in(21, '127.0.0.1'), :INET, :RAW)

      addr.getnameinfo[1].should == 'ftp'
    end
  end

  platform_is :linux do
    platform_is_not :android, :musl do
      describe 'using a UNIX Addrinfo' do
        before do
          @addr = Addrinfo.unix('cats')
          @host = Socket.gethostname
        end

        it 'returns the hostname and UNIX socket path' do
          host, path = @addr.getnameinfo

          host.should == @host
          path.should == 'cats'
        end
      end
    end
  end
end
