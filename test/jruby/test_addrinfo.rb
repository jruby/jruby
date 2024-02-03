require 'test/unit'
require 'socket'
require 'thread'
require 'test/jruby/test_helper'
require 'ipaddr'

WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

class AddrinfoTest < Test::Unit::TestCase
  include TestHelper

  # these assertions emulate Addrinfo behavior in Ruby MRI 2.2.0
  
  # get system generated addresses to test with from interfaces
  # TODO: (gf) mock Addrinfo creation for platform specific behaviors ?
  def getaddrs
    begin
      @addrs ||= Socket.getifaddrs.collect { |i| i.addr }
    rescue NotImplementedError
      @addrs = []  
    end
  end
  private :getaddrs

  def test_link_afamily_pfamily
    # at least one address (loopback link interface) is AF/PF_UNSPEC
    link_addrs = getaddrs.select { |addr| addr.afamily == Socket::AF_UNSPEC && addr.pfamily == Socket::PF_UNSPEC }
    assert( link_addrs.count > 0 ) if getaddrs.count > 0  
  end

  def test_afamily
    getaddrs.each do |addr|
      assert( [ Socket::AF_INET, Socket::AF_INET6, Socket::AF_UNSPEC ].include? addr.afamily )
    end
  end

  def test_ip?
    getaddrs.each do |addr|
      case addr.afamily
      when Socket::AF_INET   then assert_equal(true, addr.ip?)
      when Socket::AF_INET6  then assert_equal(true, addr.ip?)
      when Socket::AF_UNSPEC then assert_equal(false, addr.ip?)
      else assert_equal(false, addr.ip?)
      end
    end
  end

  def test_ip_address
    getaddrs.each do |addr|
      if addr.ip?
        assert_instance_of(String, addr.ip_address)
      else
        assert_raise(SocketError) { addr.ip_address }
      end
    end
  end

  def test_ip_port
    getaddrs.each do |addr|
      if addr.ip?
        assert_equal(0, addr.ip_port)
      else
        assert_raise(SocketError) { addr.ip_port }
      end
    end
  end

  def test_ip_unpack
    getaddrs.each do |addr|
      if addr.ip?
        unpacked = addr.ip_unpack
        assert_instance_of(Array, unpacked)
        assert_equal(2, unpacked.count)
        assert_equal(addr.ip_address, unpacked[0])
        assert_equal(0, unpacked[1])
      else
        assert_raise(SocketError) { addr.ip_port }
      end
    end
  end

  def test_ipv4?
    getaddrs.each do |addr|
      if addr.afamily == Socket::AF_INET
        assert_equal(true, addr.ipv4?)
      else
        assert_equal(false, addr.ipv4?)
      end
    end
  end

  def test_ipv4_loopback?
    loopbacks = getaddrs.select do |addr|
      if addr.afamily == Socket::AF_INET
        addr.ipv4_loopback? 
      else
        assert_equal(false, addr.ipv4_loopback?)
        nil
      end
    end
    assert(loopbacks.count > 0)  # at least one ipv4 loopback
  end

  def test_ipv6?
    getaddrs.each do |addr|
      if addr.afamily == Socket::AF_INET6
        assert_equal(true, addr.ipv6?)
      else
        assert_equal(false, addr.ipv6?)
      end
    end
  end

  # Travis CI's 'trusty' environment no longer has IP6 (travis-ci/travis-ci#4964)
  unless ENV['TRAVIS']
    def test_ipv6_loopback?
      loopbacks = getaddrs.select do |addr|
        if addr.afamily == Socket::AF_INET6
          addr.ipv6_loopback?
        else
          assert_equal(false, addr.ipv6_loopback?)
          nil
        end
      end
      assert_equal(1, loopbacks.count)  # only one ipv6 loopback
    end
  end
  
  def test_pfamily
    getaddrs.each do |addr|
      assert( [ Socket::PF_INET, Socket::PF_INET6, Socket::PF_UNSPEC ].include? addr.pfamily )
    end
  end

  def test_protocol
    getaddrs.each do |addr|
      assert_equal(Socket::IPPROTO_IP, addr.protocol) # all interfaces address are IP protocol ?
    end
  end

  def test_socktype
    getaddrs.each do |addr|
      assert_equal(0, addr.socktype) # all interfaces address are socktype 0 ? 
    end
  end

  def test_to_sockaddr
    getaddrs.each do |addr|
      sockaddr = addr.to_sockaddr
      assert_instance_of(String, sockaddr)
      assert_equal(sockaddr, addr.to_s)
      if addr.afamily == Socket::AF_UNSPEC
        # better tests here would be platform and interface hardware specific
        assert_equal(17, sockaddr.bytes[0])
        assert_equal( 0, sockaddr.bytes[1])
      else
        # TODO: (gf) test ipv6 addresses when Socket.unpack_sockaddr_in works for ipv6 and ipv6
        if addr.ipv4?
          assert_equal([addr.ip_port,addr.ip_address], Socket.unpack_sockaddr_in(sockaddr))
        end
      end
    end
  end

  def test_inspect
    getaddrs.each do |addr|
      if addr.ip? 
        match = addr.inspect.match(/^#<Addrinfo: (.+)>$/)
        assert_equal(addr.ip_address, match[1])
      else
        match = addr.inspect.match(/^#<Addrinfo: PACKET\[protocol=0 (.+) hatype=(.+) HOST hwaddr=(.+)\]/)
        assert_instance_of(MatchData, match)
      end
    end
  end
  
  def test_inspect_sockaddr
    getaddrs.each do |addr|
      if addr.ip? 
        assert_equal(addr.ip_address, addr.inspect_sockaddr)
      end
    end
  end
  
  # def test_create_with_interface ; end
  # def test_create_with_interface_ipaddress ; end
  # def test_create_with_ipaddress ; end
  # def test_create_with_ipaddress_port ; end
  # def test_create_with_ipaddress_port_socktype ; end

  # def test_bind ; end
  # def test_canonname ; end
  # def test_connect ; end
  # def test_connect_from ; end
  # def test_connect_to ; end
  # def test_family_addrinfo ; end
  # def test_getnameinfo ; end
  # def test_ipv4_multicast? ; end
  # def test_ipv4_private? ; end
  # def test_ipv6_linklocal? ; end
  # def test_ipv6_mc_global? ; end
  # def test_ipv6_mc_linklocal? ; end
  # def test_ipv6_mc_nodelocal? ; end
  # def test_ipv6_mc_orglocal? ; end
  # def test_ipv6_mc_sitelocal? ; end
  # def test_ipv6_multicast? ; end
  # def test_ipv6_sitelocal? ; end
  # def test_ipv6_to_ipv4 ; end
  # def test_ipv6_unspecified? ; end
  # def test_ipv6_v4compat? ; end
  # def test_ipv6_v4mapped? ; end
  # def test_listen ; end
  # def test_marshal_dump ; end
  # def test_marshal_load ; end
  # def test_to_str ; end
  # def test_unix? ; end
  # def test_unix_path ; end

end

