# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
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

module Kernel
  def Array(obj)
    ary = Rubinius::Type.check_convert_type obj, Array, :to_ary

    return ary if ary

    if array = Rubinius::Type.check_convert_type(obj, Array, :to_a)
      array
    else
      [obj]
    end
  end
  module_function :Array

  def Complex(*args)
    Truffle.privately do
      Complex.convert(*args)
    end
  end
  module_function :Complex

  def Float(obj)
    case obj
    when String
      Rubinius::Type.coerce_string_to_float obj, true
    else
      Rubinius::Type.coerce_object_to_float obj
    end
  end
  module_function :Float

  ##
  # MRI uses a macro named NUM2DBL which has essentially the same semantics as
  # Float(), with the difference that it raises a TypeError and not a
  # ArgumentError. It is only used in a few places (in MRI and Rubinius).
  #--
  # If we can, we should probably get rid of this.

  def FloatValue(obj)
    exception = TypeError.new 'no implicit conversion to float'

    case obj
    when String
      raise exception
    else
      begin
        Rubinius::Type.coerce_object_to_float obj
      rescue
        raise exception
      end
    end
  end
  private :FloatValue

  def Hash(obj)
    return {} if obj.nil? || obj == []

    if hash = Rubinius::Type.check_convert_type(obj, Hash, :to_hash)
      return hash
    end

    raise TypeError, "can't convert #{obj.class} into Hash"
  end
  module_function :Hash

  def Integer(obj, base=nil)
    if obj.kind_of? String
      if obj.empty?
        raise ArgumentError, "invalid value for Integer: (empty string)"
      else
        base ||= 0
        return obj.to_inum(base, true)
      end
    end

    if base
      raise ArgumentError, "base is only valid for String values"
    end

    case obj
    when Integer
      obj
    when Float
      if obj.nan? or obj.infinite?
        raise FloatDomainError, "unable to coerce #{obj} to Integer"
      else
        obj.to_int
      end
    when NilClass
      raise TypeError, "can't convert nil into Integer"
    else
      # Can't use coerce_to or try_convert because I think there is an
      # MRI bug here where it will return the value without checking
      # the return type.
      if obj.respond_to? :to_int
        if val = obj.to_int
          return val
        end
      end

      Rubinius::Type.coerce_to obj, Integer, :to_i
    end
  end
  module_function :Integer

  def Rational(a, b = 1)
    Truffle.privately do
      Rational.convert a, b
    end
  end
  module_function :Rational

  def String(obj)
    return obj if obj.kind_of? String

    unless obj.respond_to?(:to_s)
      raise TypeError, "can't convert #{obj.class} into String"
    end

    begin
      str = obj.to_s
    rescue NoMethodError
      raise TypeError, "can't convert #{obj.class} into String"
    end

    unless str.kind_of? String
      raise TypeError, "#to_s did not return a String"
    end

    return str
  end
  module_function :String

  ##
  # MRI uses a macro named StringValue which has essentially the same
  # semantics as obj.coerce_to(String, :to_str), but rather than using that
  # long construction everywhere, we define a private method similar to
  # String().
  #
  # Another possibility would be to change String() as follows:
  #
  #   String(obj, sym=:to_s)
  #
  # and use String(obj, :to_str) instead of StringValue(obj)

  def StringValue(obj)
    Rubinius::Type.coerce_to obj, String, :to_str
  end
  module_function :StringValue

  def `(str) #`
    str = StringValue(str) unless str.kind_of?(String)

    io = IO.popen(str)
    output = io.read
    io.close

    Rubinius::Type.external_string output
  end
  module_function :` # `

  def =~(other)
    nil
  end

  def !~(other)
    r = self =~ other ? false : true
    Truffle.invoke_primitive(:regexp_set_last_match, $~)
    r
  end

  def itself
    self
  end

  def abort(msg=nil)
    Process.abort msg
  end
  module_function :abort

  def autoload(name, file)
    Object.autoload(name, file)
  end
  private :autoload

  def autoload?(name)
    Object.autoload?(name)
  end
  private :autoload?

  alias_method :iterator?, :block_given?

  def define_singleton_method(*args, &block)
    singleton_class.send(:define_method, *args, &block)
  end

  def display(port=$>)
    port.write self
  end

  def exec(*args)
    Process.exec(*args)
  end
  module_function :exec

  def exit(code=0)
    Process.exit(code)
  end
  module_function :exit

  def exit!(code=1)
    Process.exit!(code)
  end
  module_function :exit!

  def extend(*modules)
    raise ArgumentError, "wrong number of arguments (0 for 1+)" if modules.empty?

    modules.reverse_each do |mod|
      if !mod.kind_of?(Module) or mod.kind_of?(Class)
        raise TypeError, "wrong argument type #{mod.class} (expected Module)"
      end

      Truffle.privately do
        mod.extend_object self
      end

      Truffle.privately do
        mod.extended self
      end
    end
    self
  end

  alias_method :__extend__, :extend

  def getc
    $stdin.getc
  end
  module_function :getc

  def inspect
    prefix = "#<#{self.class}:0x#{self.__id__.to_s(16)}"

    # The protocol here seems odd, but it's to match MRI.
    #
    # MRI side-calls to the C function that implements Kernel#to_s. If that
    # method is overridden, the new Ruby method is never called. So, we inline
    # the code for Kernel#to_s here because we simply dispatch to Ruby
    # methods.
    ivars = __instance_variables__

    if ivars.empty?
      return Rubinius::Type.infect "#{prefix}>", self
    end

    # Otherwise, if it's already been inspected, return the ...
    return "#{prefix} ...>" if Thread.guarding? self

    # Otherwise, gather the ivars and show them.
    parts = []

    Thread.recursion_guard self do
      ivars.each do |var|
        parts << "#{var}=#{__instance_variable_get__(var).inspect}"
      end
    end

    if parts.empty?
      str = "#{prefix}>"
    else
      str = "#{prefix} #{parts.join(' ')}>"
    end

    Rubinius::Type.infect(str, self)

    return str
  end

  alias_method :__instance_variable_get__, :instance_variable_get

  # Both of these are for defined? when used inside a proxy obj that
  # may undef the regular method. The compiler generates __ calls.
  alias_method :__instance_variable_defined_p__, :instance_variable_defined?
  alias_method :__respond_to_p__, :respond_to?

  def load(filename, wrap = false)
    filename = Rubinius::Type.coerce_to_path filename

    # load absolute path
    if filename.start_with? File::SEPARATOR
      return Truffle::Kernel.load File.expand_path(filename), wrap
    end

    # if path starts with . only try relative paths
    if filename.start_with? '.'
      return Truffle::Kernel.load File.expand_path(filename), wrap
    end

    # try to resolve with current working directory
    if File.exist? filename
      return Truffle::Kernel.load File.expand_path(filename), wrap
    end

    # try to find relative path in $LOAD_PATH
    $LOAD_PATH.each do |dir|
      path = File.expand_path(File.join(dir, filename))
      if File.exist? path
        return Truffle::Kernel.load path, wrap
      end
    end

    # file not found trigger an error
    Truffle::Kernel.load filename, wrap
  end
  module_function :load

  def loop
    return to_enum(:loop) { Float::INFINITY } unless block_given?

    result = nil
    begin
      while true
        yield
      end
    rescue StopIteration => si
       result = si.result
    end
    result
  end
  module_function :loop

  def object_id
    Truffle.primitive :object_id
    raise PrimitiveFailure, "Kernel#object_id primitive failed"
  end

  def open(obj, *rest, &block)
    if obj.respond_to?(:to_open)
      obj = obj.to_open(*rest)

      if block_given?
        return yield(obj)
      else
        return obj
      end
    end

    path = Rubinius::Type.coerce_to_path obj

    if path.kind_of? String and path.prefix? '|'
      return IO.popen(path[1..-1], *rest, &block)
    end

    File.open(path, *rest, &block)
  end
  module_function :open

  def p(*a)
    return nil if a.empty?
    a.each { |obj| $stdout.puts obj.inspect }
    $stdout.flush

    a.size == 1 ? a.first : a
  end
  module_function :p

  def print(*args)
    args.each do |obj|
      $stdout.write obj.to_s
    end
    nil
  end
  module_function :print

  def public_method(name)
    name = Rubinius::Type.coerce_to_symbol name
    code = Rubinius.find_public_method(self, name)

    if code
      Method.new(self, code[1], code[0], name)
    elsif respond_to_missing?(name, false)
      Method.new(self, self.class, Rubinius::MissingMethod.new(self,  name), name)
    else
      raise NameError, "undefined method `#{name}' for #{self.inspect}"
    end
  end

  def public_singleton_methods
    m = Rubinius::Type.object_singleton_class self
    methods = m.method_table.public_names

    while m = m.direct_superclass
      unless Rubinius::Type.object_kind_of?(m, Rubinius::IncludedModule) or
             Rubinius::Type.singleton_class_object(m)
        break
      end

      methods.concat m.method_table.public_names
    end

    methods
  end
  private :public_singleton_methods

  def putc(int)
    $stdout.putc(int)
  end
  module_function :putc

  def puts(*a)
    $stdout.puts(*a)
    nil
  end
  module_function :puts

  def rand(limit=0)
    if limit == 0
      return Thread.current.randomizer.random_float
    end

    if limit.kind_of?(Range)
      return Thread.current.randomizer.random(limit)
    else
      limit = Integer(limit).abs

      if limit == 0
        Thread.current.randomizer.random_float
      else
        Thread.current.randomizer.random_integer(limit - 1)
      end
    end
  end
  module_function :rand

  def readline(sep=$/)
    ARGF.readline(sep)
  end
  module_function :readline

  def readlines(sep=$/)
    ARGF.readlines(sep)
  end
  module_function :readlines

  def select(*args)
    IO.select(*args)
  end
  module_function :select

  def srand(seed=undefined)
    if undefined.equal? seed
      seed = Thread.current.randomizer.generate_seed
    end

    seed = Rubinius::Type.coerce_to seed, Integer, :to_int
    Thread.current.randomizer.swap_seed seed
  end
  module_function :srand

  def tap
    yield self
    self
  end

  def test(cmd, file1, file2=nil)
    case cmd
    when ?d
      File.directory? file1
    when ?e
      File.exist? file1
    when ?f
      File.file? file1
    when ?l
      File.symlink? file1
    when ?r
      File.readable? file1
    when ?R
      File.readable_real? file1
    when ?w
      File.writable? file1
    when ?W
      File.writable_real? file1
    when ?A
      File.atime file1
    when ?C
      File.ctime file1
    when ?M
      File.mtime file1
    else
      raise NotImplementedError, "command ?#{cmd.chr} not implemented"
    end
  end
  module_function :test

  def to_enum(method=:each, *args, &block)
    Enumerator.new(self, method, *args).tap do |enum|
      Truffle.privately { enum.size = block } if block_given?
    end
  end
  alias_method :enum_for, :to_enum

  def trap(sig, prc=nil, &block)
    Signal.trap(sig, prc, &block)
  end
  module_function :trap

  def spawn(*args)
    Process.spawn(*args)
  end
  module_function :spawn

  def syscall(*args)
    raise NotImplementedError
  end
  module_function :syscall

  def system(*args)
    begin
      pid = Process.spawn(*args)
    rescue SystemCallError
      return nil
    end

    Process.waitpid pid
    $?.exitstatus == 0
  end
  module_function :system

  def trace_var(name, cmd = nil, &block)
    if !cmd && !block
      raise ArgumentError,
        "The 2nd argument should be a Proc/String, alternatively use a block"
    end

    # Truffle: not yet implemented
  end
  module_function :trace_var

  def untrace_var(name, cmd)
    # Truffle: not yet implemented
  end
  module_function :untrace_var

  def warn(*messages)
    $stderr.puts(*messages) if !$VERBOSE.nil? && !messages.empty?
    nil
  end
  module_function :warn

  def warning(message)
    $stderr.puts message if $VERBOSE
  end
  module_function :warning

  def raise(exc=undefined, msg=undefined, ctx=nil)
    skip = false
    if undefined.equal? exc
      exc = $!
      if exc
        skip = true
      else
        exc = RuntimeError.new("No current exception")
      end
    elsif exc.respond_to? :exception
      if undefined.equal? msg
        exc = exc.exception
      else
        exc = exc.exception msg
      end
      raise ::TypeError, 'exception class/object expected' unless exc.kind_of?(::Exception)
    elsif exc.kind_of? String
      exc = ::RuntimeError.exception exc
    else
      raise ::TypeError, 'exception class/object expected'
    end

    unless skip
      exc.set_context ctx if ctx
      exc.capture_backtrace!(2) unless exc.backtrace?
    end

    if $DEBUG and $VERBOSE != nil
      if bt = exc.backtrace and !bt.empty?
        pos = bt.first
      else
        pos = caller.first
      end

      STDERR.puts "Exception: `#{exc.class}' #{pos} - #{exc.message}"
    end

    Rubinius.raise_exception exc
  end
  module_function :raise

  alias_method :fail, :raise
  module_function :fail

  def __dir__
    path = caller_locations(1, 1).first.absolute_path
    File.dirname(path)
  end
  module_function :__dir__

  def printf(*args)
    print sprintf(*args)
  end
  module_function :printf

  alias_method :trust, :untaint
  alias_method :untrust, :taint
  alias_method :untrusted?, :tainted?

  def caller(start = 1, limit = nil)
    start += 1
    if limit.nil?
      args = [start]
    else
      args = [start, limit]
    end
    Kernel.caller_locations(*args).map(&:inspect)
  end
  module_function :caller

  def at_exit(&block)
    Truffle::Kernel.at_exit false, &block
  end
  module_function :at_exit

  def global_variables
    Truffle.primitive :kernel_global_variables
    raise PrimitiveFailure, "Kernel.global_variables primitive failed"
  end
  module_function :global_variables

  def fork
    raise RubyTruffleError.new('Kernel#fork not implemented')
  end
  module_function :fork

end
