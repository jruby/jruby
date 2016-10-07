# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Exception

  def ==(other)
    other.instance_of?(__class__) &&
      message == other.message &&
      backtrace == other.backtrace
  end

  def message
    self.to_s
  end

  def to_s
    msg = Truffle.invoke_primitive :exception_message, self
    if msg.nil?
      self.class.to_s
    else
      msg.to_s
    end
  end

  # This is here rather than in yaml.rb because it contains "private"
  # information, ie, the list of ivars. Putting it over in the yaml
  # source means it's easy to forget about.
  def to_yaml_properties
    list = super
    list.delete :@backtrace
    list.delete :@custom_backtrace
    return list
  end

  # Needed to properly implement #exception, which must clone and call
  # #initialize again, BUT not a subclasses initialize.
  alias_method :__initialize__, :initialize

  # Indicates if the Exception has a backtrace set
  def backtrace?
    backtrace ? true : false
  end

  def set_backtrace(bt)
    case bt
    when Array
      if bt.all? { |s| s.kind_of? String }
        @custom_backtrace = bt
      else
        raise TypeError, "backtrace must be Array of String"
      end
    when String
      @custom_backtrace = [bt]
    when nil
      @custom_backtrace = nil
    else
      raise TypeError, "backtrace must be Array of String"
    end
  end

  def set_context(ctx)
    if ctx.kind_of? Exception
      @parent = ctx
    else
      set_backtrace(ctx)
    end
  end

  def inspect
    s = self.to_s
    if s.empty?
      self.class.name
    else
      "#<#{self.class.name}: #{s}>"
    end
  end

  class << self
    alias_method :exception, :new
  end

  def exception(message=nil)
    if message
      unless message.equal? self
        # As strange as this might seem, this IS actually the protocol
        # that MRI implements for this. The explicit call to
        # Exception#initialize (via __initialize__) is exactly what MRI
        # does.
        e = clone
        Truffle.privately do
          e.__initialize__(message)
        end
        return e
      end
    end

    self
  end

  def location
    [context.file.to_s, context.line]
  end
end

class PrimitiveFailure < Exception
end

class ScriptError < Exception
end

class StandardError < Exception
end

class SignalException < Exception
end

class NoMemoryError < Exception
end

class ZeroDivisionError < StandardError
end

class ArgumentError < StandardError
  def to_s
    if @given and @expected
      if @method_name
        "method '#{@method_name}': given #{@given}, expected #{@expected}"
      else
        "given #{@given}, expected #{@expected}"
      end
    else
      super
    end
  end
end

class UncaughtThrowError < ArgumentError
end

class IndexError < StandardError
end

class StopIteration < IndexError
end

class RangeError < StandardError
end

class FloatDomainError < RangeError
end

class LocalJumpError < StandardError
end

class NameError < StandardError

  def initialize(*args)
    name = args.size > 1 ? args.pop : nil
    super(*args)
    Truffle.invoke_primitive :name_error_set_name, self, name
  end
end

class NoMethodError < NameError

  def initialize(*arguments)
    args = arguments.size > 2 ? arguments.pop : nil
    super(*arguments) # TODO BJF Jul 24, 2016 Need to handle NoMethodError.new(1,2,3,4)
    Truffle.invoke_primitive :no_method_error_set_args, self, args
  end
end

class RuntimeError < StandardError
end

class SecurityError < Exception
end

class ThreadError < StandardError
end

class FiberError < StandardError
end

class TypeError < StandardError
end

class FloatDomainError < RangeError
end

class RegexpError < StandardError
end

class LoadError < ScriptError
  attr_accessor :path

  class InvalidExtensionError < LoadError
  end

  class MRIExtensionError < InvalidExtensionError
  end
end

class NotImplementedError < ScriptError
end

class Interrupt < SignalException
end

class IOError < StandardError
end

class EOFError < IOError
end

class LocalJumpError < StandardError
end

class SyntaxError < ScriptError
  attr_accessor :column
  attr_accessor :line
  attr_accessor :file
  attr_accessor :code

  def reason
    @reason_message
  end
end

class SystemExit < Exception

  ##
  # Process exit status if this exception is raised

  attr_reader :status

  ##
  # Creates a SystemExit exception with optional status and message.  If the
  # status is omitted, Process::EXIT_SUCCESS is used.
  #--
  # *args is used to simulate optional prepended argument like MRI

  def initialize(first=nil, *args)
    if first.kind_of?(Fixnum)
      status = first
      super(*args)
    else
      status = Process::EXIT_SUCCESS
      super
    end

    @status = status
  end

  ##
  # Returns true is exiting successfully, false if not. A successful exit is
  # one with a status equal to 0 (zero). Any other status is considered a
  # unsuccessful exit.

  def success?
    status == Process::EXIT_SUCCESS
  end

end


class SystemCallError < StandardError


  def self.errno_error(message, errno, location)
    Truffle.primitive :exception_errno_error
    raise PrimitiveFailure, "SystemCallError.errno_error failed"
  end

  # We use .new here because when errno is set, we attempt to
  # lookup and return a subclass of SystemCallError, specifically,
  # one of the Errno subclasses.
  def self.new(*args)
    # This method is used 2 completely different ways. One is when it's called
    # on SystemCallError, in which case it tries to construct a Errno subclass
    # or makes a generic instead of itself.
    #
    # Otherwise it's called on a Errno subclass and just helps setup
    # a instance of the subclass
    if self.equal? SystemCallError
      case args.size
      when 1
        if args.first.kind_of?(Fixnum)
          errno = args.first
          message = nil
        else
          errno = nil
          message = StringValue(args.first)
        end
        location = nil
      when 2
        message, errno = args
        location = nil
      when 3
        message, errno, location = args
      else
        raise ArgumentError, "wrong number of arguments (#{args.size} for 1..3)"
      end

      # If it corresponds to a known Errno class, create and return it now
      if errno && error = SystemCallError.errno_error(message, errno, location)
        return error
      else
        return super(message, errno, location)
      end
    else
      case args.size
      when 0
        message = nil
        location = nil
      when 1
        message = StringValue(args.first)
        location = nil
      when 2
        message, location = args
      else
        raise ArgumentError, "wrong number of arguments (#{args.size} for 0..2)"
      end

      if defined?(self::Errno) && self::Errno.kind_of?(Fixnum)
        errno = self::Errno
        error = SystemCallError.errno_error(message, self::Errno, location)
        if error && error.class.equal?(self)
          return error
        end
      end

      error = allocate
      Rubinius::Unsafe.set_class error, self
      Truffle.privately { error.initialize(*args) }
      return error
    end
  end

  # Must do this here because we have a unique new and otherwise .exception will
  # call Exception.new because of the alias in Exception.
  class << self
    alias_method :exception, :new
  end

  # Use splat args here so that arity returns -1 to match MRI.
  def initialize(*args)
    kls = self.class
    message, errno, location = args
    Truffle.invoke_primitive :exception_set_errno, self, errno

    msg = "unknown error"
    msg << " @ #{StringValue(location)}" if location
    msg << " - #{StringValue(message)}" if message
    super(msg)
  end
end

class KeyError < IndexError
end

class SignalException < Exception

  attr_reader :signo
  attr_reader :signm

  def initialize(signo = nil, signm = nil)
    # MRI overrides this behavior just for SignalException itself
    # but not for anything that inherits from it, therefore we
    # need this ugly check to make sure it works as intented.
    return super(signo) unless self.class == SignalException
    if signo.is_a? Integer
      unless @signm = Signal::Numbers[signo]
        raise ArgumentError, "invalid signal number #{signo}"
      end
      @signo = signo
      @signm = signm || "SIG#{@signm}"
    elsif signo
      if signm
        raise ArgumentError, "wrong number of arguments (2 for 1)"
      end
      signm = signo
      if signo.kind_of?(Symbol)
        signm = signm.to_s
      else
        signm = StringValue(signm)
      end
      signm = signm[3..-1] if signm.prefix? "SIG"
      unless @signo = Signal::Names[signm]
        raise ArgumentError, "invalid signal name #{signm}"
      end
      @signm = "SIG#{signm}"
    end
    super(@signm)
  end
end

class StopIteration
  attr_accessor :result
  private :result=
end

##
# Base class for various exceptions raised in the VM.

class Rubinius::VMException < Exception
end

##
# Raised in the VM when an assertion fails.

class Rubinius::AssertionError < Rubinius::VMException
end

##
# Raised in the VM when attempting to read/write outside
# the bounds of an object.

class Rubinius::ObjectBoundsExceededError < Rubinius::VMException
end
