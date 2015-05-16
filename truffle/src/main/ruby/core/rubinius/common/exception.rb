 # Copyright (c) 2007-2014, Evan Phoenix and contributors
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

# Only part of Rubinius' exception.rb

class Exception

  # Needed to properly implement #exception, which must clone and call
  # #initialize again, BUT not a subclasses initialize.
  alias_method :__initialize__, :initialize

  # Indicates if the Exception has a backtrace set
  def backtrace?
    backtrace ? true : false # Truffle: simplified
  end

  def set_backtrace(bt)
    if false # bt.kind_of? Rubinius::Backtrace # Truffle: not supported
      @backtrace = bt
    else
      # See if we stashed a Backtrace object away, and use it.
      if false # hidden_bt = Rubinius::Backtrace.detect_backtrace(bt) # Truffle: not yet supported
        @backtrace = hidden_bt
      else
        type_error = TypeError.new "backtrace must be Array of String"
        case bt
        when Array
          if bt.all? { |s| s.kind_of? String }
            @custom_backtrace = bt
          else
            raise type_error
          end
        when String
          @custom_backtrace = [bt]
        when nil
          @custom_backtrace = nil
        else
          raise type_error
        end
      end
    end
  end

  def set_context(ctx)
    if ctx.kind_of? Exception
      @parent = ctx
    else
      set_backtrace(ctx)
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
        Rubinius.privately do # Truffle: added the privately block as Exception#initialize (and its alias) should be private
          e.__initialize__(message)
        end
        return e
      end
    end

    self
  end

end

class NameError < StandardError
  attr_reader :name

  def initialize(*args)
    super(args.shift)
    @name = args.shift
  end
end

class NoMethodError < NameError
  attr_reader :name
  attr_reader :args

  def initialize(*arguments)
    super(arguments.shift)
    @name = arguments.shift
    @args = arguments.shift
  end
end

class StopIteration < IndexError
end

class StopIteration
  attr_accessor :result
  private :result=
end

class SystemCallError < StandardError

  attr_reader :errno

  def self.errno_error(message, errno)
    Rubinius.primitive :exception_errno_error
    raise PrimitiveFailure, "SystemCallError.errno_error failed"
  end

  # We use .new here because when errno is set, we attempt to
  # lookup and return a subclass of SystemCallError, specificly,
  # one of the Errno subclasses.
  def self.new(*args)
    case args.size
    when 0
      message = errno = undefined
    when 1
      message = args.first
      errno = undefined
    else
      message, errno = args
    end

    # This method is used 2 completely different ways. One is when it's called
    # on SystemCallError, in which case it tries to construct a Errno subclass
    # or makes a generic instead of itself.
    #
    # Otherwise it's called on a Errno subclass and just helps setup
    # a instance of the subclass
    if self.equal? SystemCallError
      if undefined.equal? message
        raise ArgumentError, "must supply at least a message/errno"
      end

      if undefined.equal? errno
        if message.kind_of?(Fixnum)
          if inst = SystemCallError.errno_error(nil, message)
            return inst
          else # It's some random errno
            errno = message
            message = nil
          end
        else
          errno = nil
        end
      else
        message = StringValue(message) if message

        if errno.kind_of? Fixnum
          if error = SystemCallError.errno_error(message, errno)
            return error
          end
        end
      end

      return super(message, errno)
    else
      unless undefined.equal? errno
        raise ArgumentError, "message is the only argument"
      end

      if message and !undefined.equal?(message)
        message = StringValue(message)
      end

      if self::Errno.kind_of? Fixnum
        error = SystemCallError.errno_error(message, self::Errno)
      else
        error = allocate
      end

      if error
        Rubinius::Unsafe.set_class error, self
        Rubinius.privately { error.initialize(*args) }
        return error
      end

      raise TypeError, "invalid Errno subclass"
    end
  end

  # Must do this here because we have a unique new and otherwise .exception will
  # call Exception.new because of the alias in Exception.
  class << self
    alias_method :exception, :new
  end

  # Use splat args here so that arity returns -1 to match MRI.
  def initialize(*args)
    message, errno = args
    @errno = errno

    msg = "unknown error"
    msg << " - #{StringValue(message)}" if message
    super(msg)
  end
end
