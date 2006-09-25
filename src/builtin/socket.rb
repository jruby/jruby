#/***** BEGIN LICENSE BLOCK *****
# * Version: CPL 1.0/GPL 2.0/LGPL 2.1
# *
# * The contents of this file are subject to the Common Public
# * License Version 1.0 (the "License"); you may not use this file
# * except in compliance with the License. You may obtain a copy of
# * the License at http://www.eclipse.org/legal/cpl-v10.html
# *
# * Software distributed under the License is distributed on an "AS
# * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# * implied. See the License for the specific language governing
# * rights and limitations under the License.
# *
# * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
# * Copyright (C) 2005-2006 Thomas E Enebo <enebo@acm.org>
# * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
# * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
# * 
# * Alternatively, the contents of this file may be used under the terms of
# * either of the GNU General Public License Version 2 or later (the "GPL"),
# * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# * in which case the provisions of the GPL or the LGPL are applicable instead
# * of those above. If you wish to allow use of your version of this file only
# * under the terms of either the GPL or the LGPL, and not to allow others to
# * use your version of this file under the terms of the CPL, indicate your
# * decision by deleting the provisions above and replace them with the notice
# * and other provisions required by the GPL or the LGPL. If you do not delete
# * the provisions above, a recipient may use your version of this file under
# * the terms of any one of the CPL, the GPL or the LGPL.
# ***** END LICENSE BLOCK *****/
require "java"

class SocketError < StandardError; end

class BasicSocket
  def getsockname
      JavaUtilities.wrap(__getsockname)
  end
  
  def getpeername
    JavaUtilities.wrap(__getpeername)
  end
end

class Socket < BasicSocket
  include_class 'java.net.InetAddress'
  include_class 'java.net.InetSocketAddress'
  include_class 'java.net.UnknownHostException'

  module Constants
    # we don't have to define any that we don't support; see socket.c
    SOCK_STREAM, SOCK_DGRAM = 1, 2
    PF_UNSPEC = 0
    AF_UNSPEC = PF_UNSPEC
    PF_INET = 2
    AF_INET = PF_INET
    # mandatory constants we haven't implemented
    MSG_OOB = 0x01
    SOL_SOCKET = 1
    SOL_IP = 0
    SOL_TCP = 6
    #  SOL_UDP = 17
    IPPROTO_IP = 0
    IPPROTO_ICMP = 1
    IPPROTO_TCP = 6
    #  IPPROTO_UDP = 17
    #  IPPROTO_RAW = 255
    INADDR_ANY = 0x00000000
    INADDR_BROADCAST = 0xffffffff
    INADDR_LOOPBACK = 0x7f000001
    INADDR_UNSPEC_GROUP = 0xe0000000
    INADDR_ALLHOSTS_GROUP = 0xe0000001
    INADDR_MAX_LOCAL_GROUP = 0xe00000ff
    INADDR_NONE = 0xffffffff
    SO_REUSEADDR = 2
    SHUT_RD = 0
    SHUT_WR = 1
    SHUT_RDWR = 2
    
    # constants webrick crashes without
    AI_PASSIVE = 1

    # constants Rails > 1.1.4 ActiveRecord's default mysql adapter dies without during scaffold generation
    SO_KEEPALIVE = 9
    
    # drb needs defined
    TCP_NODELAY = 1
  end
  include Constants

  def self.gethostname
    # FIXME: Static methods should allow java short-hand
    InetAddress.getLocalHost.hostName
  rescue UnknownHostException => e
    raise SocketError.new("getaddrinfo: Name or service not known")
  end

  def self.gethostbyaddr(addr, type=AF_INET)
    return [ addr.getAddress.getCanonicalHostName, [], AF_INET, addr ]
  rescue UnknownHostException => e
    raise SocketError.new("getaddrinfo: Name or service not known")
  end

  def self.gethostbyname(hostname)
    addr = InetAddress.getByName(hostname)
    return [ addr.getCanonicalHostName, [], AF_INET, InetSocketAddress.new(addr, 0) ]
  rescue UnknownHostException => e
    throw SocketError.new("getaddrinfo: Name or service not known")
  end

  def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
    addrs = InetAddress.getAllByName(host)
    return addrs.inject([]) do |array, addr|
      # protocol number 6 is TCP
      array << [ "AF_INET", port, addr.getHostName, addr.getHostAddress, PF_INET, SOCK_STREAM, 6 ]
    end
  rescue UnknownHostException => e
    raise SocketError.new("getaddrinfo: Name or service not known")
  end
  
  def self.getnameinfo(addr, flags = nil)
    # since strings act a lot like arrays, we can't easily distinguish them by behaviour
    if(addr.kind_of? Array)
      return [ InetAddress.getByName(addr[2]).getCanonicalHostName, addr[1] ]
    else
      return [ addr.getAddress.getCanonicalHostName, addr.getPort ]
    end
  rescue UnknownHostException => e
    raise SocketError.new("getaddrinfo: Name or service not known")
  end
end

class IPSocket < BasicSocket
  include_class 'java.net.InetAddress'
  include_class 'java.net.InetSocketAddress'
  include_class 'java.net.UnknownHostException'

  def addr
    addr = getsockname
    return [ "AF_INET", addr.getPort,
      (BasicSocket.do_not_reverse_lookup ? addr.getAddress.getHostAddress : addr.getHostName),
      addr.getAddress.getHostAddress ]
  end

  def peeraddr()
    addr = getpeername 
    return [ "AF_INET", addr.getPort,
      (BasicSocket.do_not_reverse_lookup ? addr.getAddress.getHostAddress : addr.getHostName),
      addr.getAddress.getHostAddress ]
  end

  def self.getaddress(hostname)
    return InetAddress.getByName(hostname).getHostAddress
  rescue UnknownHostException => e
    throw SocketError.new("getaddrinfo: Name or service not known")
  end
end

class TCPSocket < IPSocket
  include Socket::Constants
  include_class 'java.net.InetAddress'
  include_class 'java.net.InetSocketAddress'
  include_class 'java.net.ConnectException'
  include_class 'java.nio.channels.SocketChannel'
  include_class('java.net.Socket') {|p,c| 'JavaSocket'}
  include_class 'java.net.UnknownHostException'
  
  def initialize(arg1, port) 
    begin
      if port
        begin
          addr = InetSocketAddress.new(InetAddress.getByName(arg1), port)
          channel = SocketChannel.open(addr)
          channel.finishConnect
        rescue UnknownHostException
          raise SocketError.new("getaddrinfo: Name or service not known")
        end
      else 
        channel = arg1
      end
        
      super channel
    rescue ConnectException => e
      raise Errno::ECONNREFUSED.new
    end
  end

  def self.gethostbyname(hostname)
    addr = InetAddress.getByName(hostname)
    return [ addr.getCanonicalHostName, [], AF_INET, addr.getHostAddress ]
  rescue UnknownHostException => e
    throw SocketError.new("getaddrinfo: Name or service not known")
  end

  def self.open(*args)
    sock = new(*args)
    if block_given?
      begin
        value = yield sock
      ensure
        sock.close unless sock.closed?
      end
      value
    else
      sock
    end
  end

  def self.gethostbyname(hostname)
    addr = InetAddress.getByName(hostname)
    return [ addr.getCanonicalHostName, [], AF_INET, addr.getHostAddress ]
  rescue UnknownHostException => e
    throw SocketError.new("getaddrinfo: Name or service not known")
  end
    
  # Stubbed out for drb (perhaps some of these are possible?)
  def setsockopt(*args)
  end
end

class TCPServer < TCPSocket
  include_class('java.net.ServerSocket')
  include_class('java.nio.channels.ServerSocketChannel')
  include_class('java.net.InetAddress')
  include_class('java.net.UnknownHostException')
  include_class('java.net.InetSocketAddress')
  include_class('java.net.UnknownHostException')

  def initialize(hostname, port) 
    hostname ||= '0.0.0.0'
    addr = InetAddress.getByName hostname
    @javaServerSocketChannel = ServerSocketChannel.open
    @socket_address = InetSocketAddress.new(addr, port)
    @javaServerSocketChannel.socket.bind(@socket_address)

    super @javaServerSocketChannel, nil
  rescue UnknownHostException => e
    raise SocketError.new("getaddrinfo: Name or service not known")
  end
  
  def self.open(*args)
    TCPServer.new(*args)
  end

  [:peeraddr, :getpeername].each do |symbol|
      define_method(symbol) {|*args| raise "Not Supported" }
  end

  def accept
    socket = TCPSocket.new(@javaServerSocketChannel.accept, nil)
  end
  
  def close
    TCPSocket.new(@javaServerSocketChannel.accept, nil)
  end
  
  def listen(backlog)
    # Java's server socket does not allow us to change the backlog after it has already been bound
    #@javaServerSocketChannel.socket.bind(@socket_address,backlog)
    0
  end
end

TCPsocket = TCPSocket
TCPserver = TCPServer
IPsocket = IPSocket
