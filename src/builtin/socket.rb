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

class IPSocket < BasicSocket
end

class TCPSocket < IPSocket
  include_class('org.jruby.util.IOHandlerUnseekable') 
  include_class('org.jruby.RubyIO') 
  include_class('java.net.ConnectException') 
  include_class('java.net.Socket') {|p,c| 'JavaSocket'}
  
  def initialize(arg1, port) 
    begin
      if port
        @javaSocket = JavaSocket.new(arg1, port)
      else 
        @javaSocket = arg1
      end
        
      super @javaSocket.getInputStream, @javaSocket.getOutputStream
    rescue ConnectException => e
      raise Errno::ECONNREFUSED.new
    end
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
      io
    end
  end
end

class TCPServer < TCPSocket
  include_class('java.net.ServerSocket')
  include_class('java.net.InetAddress')

  def initialize(hostname, port) 
      addr = nil
      addr = InetAddress.getByName hostname if hostname
      @javaServerSocket = ServerSocket.new(port, 10, addr)
  end
  
  def self.open(*args)
    TCPServer.new(*args)
  end
  public_instance_methods(true).each {|method|
    define_method(method.to_sym) {|*args| raise "Not Supported" }
  }
  def accept
    socket = TCPSocket.new(@javaServerSocket.accept, nil)
  end
  
  def close
    @javaServerSocket.close()
  end
end

TCPsocket = TCPSocket
TCPserver = TCPServer
IPsocket = IPSocket
