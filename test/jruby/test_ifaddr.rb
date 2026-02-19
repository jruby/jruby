require 'test/unit'
require 'socket'
require 'test/jruby/test_helper'
require 'ipaddr'

class IfaddrTest < Test::Unit::TestCase
  include TestHelper

  # def test_create_with_interface_address ; end
  # def test_create_with_interface ; end
    
  def test_addr
    getifaddrs.each { |ifaddr| assert_instance_of(Addrinfo, ifaddr.addr) }
  end

  def test_broadaddr
    getifaddrs.each do |ifaddr|
      # broadcast addresses for:
      # - ipv4 address except loopback
      # - packet address except on loopback inteface (00:00:00:00:00:00)
      # TODO: (gf) deal with point-to-point interfaces

      #next if ifaddr.addr.ipv4_loopback?

      if [Socket::AF_UNSPEC, Socket::AF_INET, Socket::AF_INET6].include?(ifaddr.addr.afamily)
        if ifaddr.broadaddr.nil? # nil is okay with broadcast: 00:00:00:00:00:00
          sockaddr = ifaddr.addr.to_sockaddr
          assert(sockaddr.end_with?("\x00\x00\x00\x00\x00\x00"), "sockaddr: #{sockaddr.inspect}")
        else
          assert_instance_of(Addrinfo, ifaddr.broadaddr)
        end
      else
        #assert_equal(nil, ifaddr.broadaddr) # Travis-CI point-to-point interfaces fail here
      end
    end
  end

  # def test_dstaddr
  #   pend 'check point-to-point to return dst address'
  #   getifaddrs.each do |ifaddr|
  #     # TODO: (gf) check for point-to-point interface )
  #     # assert_instance_of(Addrinfo, ifaddr.dstaddr)
  #     assert_equal(ifaddr.dstaddr,nil)
  #   end
  # end

  # def test_flags
  #   pend 'fix #flags'
  #   getifaddrs.each do |ifaddr|
  #     # Ruby MRI flags described: http://ruby-doc.org/stdlib-2.2.0.preview1/libdoc/socket/rdoc/Socket.html#method-c-getaddrinfo 
  #     # TODO: (gf) fix #flags to return something other than nil, platform dependent ?
  #   end
  # end

  def test_ifindex
    getifaddrs.each do |ifaddr|
      assert_instance_of(Integer, ifaddr.ifindex)
      ifindex = ifaddr.ifindex # is in expected range:
      assert(ifindex <= getifaddrs.size, "ifindex: #{ifindex} (total: #{getifaddrs.size})")
      assert(ifindex > 0, "ifindex: #{ifindex}") # lo == 1
    end
  end

  def test_inspect
    getifaddrs.each do |ifaddr|
      assert(ifaddr.inspect.index( ifaddr.name ) != nil ) # at least contains name
    end
  end

  def test_name
    getifaddrs.each { |ifaddr| assert_instance_of(String, ifaddr.name) } # is a string
  end

  def test_netmask
    getifaddrs.each do |ifaddr|
      if ifaddr.addr.ip?
        assert_instance_of(Addrinfo, ifaddr.netmask)
      else
        assert_equal(nil, ifaddr.netmask)
      end
    end
  end

  private

  def getifaddrs
    begin
      @ifaddrs ||= Socket.getifaddrs
    rescue NotImplementedError
      @ifaddrs = []
    end
  end

end

