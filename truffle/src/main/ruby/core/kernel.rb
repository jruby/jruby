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

  def __method__
    scope = Rubinius::VariableScope.of_sender

    name = scope.method.name

    return nil if scope.method.for_module_body?
    # If the name is still __block__, then it's in a script, so return nil
    return nil if name == :__block__ or name == :__script__
    name
  end
  module_function :__method__

  alias_method :__callee__, :__method__
  module_function :__callee__

  def __dir__
    scope = Rubinius::ConstantScope.of_sender
    script = scope.current_script
    basepath = script.file_path
    fullpath = nil

    return nil unless basepath

    fullpath = if script.data_path
      script.data_path
    else
      Truffle.privately do
        File.basic_realpath(basepath)
      end
    end

    File.dirname fullpath
  end
  module_function :__dir__

  def `(str) #`
    str = StringValue(str) unless str.kind_of?(String)
    pid, output = Rubinius::Mirror::Process.backtick str
    Process.waitpid(pid)

    Rubinius::Type.external_string output
  end
  module_function :` # `

  def =~(other)
    nil
  end

  def <=>(other)
    self == other ? 0 : nil
  end

  def ===(other)
    equal?(other) || self == other
  end

  def itself
    self
  end

  def abort(msg=nil)
    Process.abort msg
  end
  module_function :abort

  def at_exit(prc=nil, &block)
    if prc
      unless prc.respond_to?(:call)
        raise "Argument must respond to #call"
      end
    else
      prc = block
    end

    unless prc
      raise "must pass a #call'able or block"
    end

    Rubinius::AtExit.unshift(prc)
  end
  module_function :at_exit

  def autoload(name, file)
    Object.autoload(name, file)
  end
  private :autoload

  def autoload?(name)
    Object.autoload?(name)
  end
  private :autoload?

  def block_given?
    Rubinius::VariableScope.of_sender.block != nil
  end
  module_function :block_given?

  alias_method :iterator?, :block_given?
  module_function :iterator?

  def caller(start = 1, length = nil)
    frames = []

    # The + 1 is to skip this frame
    Rubinius.mri_backtrace(start + 1).map do |tup|
      if length and frames.length == length
        break
      end

      code     = tup[0]
      line     = tup[1]
      is_block = tup[2]
      name     = tup[3]

      frames << "#{code.active_path}:#{line}:in `#{name}'"
    end

    frames
  end
  module_function :caller

  ##
  # Returns the current call stack as an Array of Thread::Backtrace::Location
  # instances. This method is available starting with Ruby 2.0.
  #
  def caller_locations(start = 1, length = nil)
    full_trace = Rubinius.mri_backtrace(start + 1)
    locations  = []

    full_trace.each do |tup|
      if length and locations.length == length
        break
      end

      scope    = tup[0].scope
      abs_path = tup[0].active_path
      path     = scope ? scope.active_path : abs_path
      label    = tup[3].to_s

      locations << Thread::Backtrace::Location.new(label, abs_path, path, tup[1])
    end

    locations
  end
  module_function :caller_locations

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

  def fork(&block)
    Process.fork(&block)
  end
  module_function :fork

  def getc
    $stdin.getc
  end
  module_function :getc

  def gets(sep=$/)
    ARGF.gets(sep)
  end
  module_function :gets

  def global_variables
    Rubinius::Globals.variables
  end
  module_function :global_variables

  def initialize_dup(other)
    initialize_copy(other)
  end
  private :initialize_dup

  def initialize_clone(other)
    initialize_copy(other)
  end
  private :initialize_clone

  def initialize_copy(source)
    unless instance_of?(Rubinius::Type.object_class(source))
      raise TypeError, "initialize_copy should take same class object"
    end
  end
  private :initialize_copy

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

  ##
  # Returns true if this object is an instance of the given class, otherwise
  # false. Raises a TypeError if a non-Class object given.
  #
  # Module objects can also be given for MRI compatibility but the result is
  # always false.

  def instance_of?(cls)
    Truffle.primitive :object_instance_of

    arg_class = Rubinius::Type.object_class(cls)
    if arg_class != Class and arg_class != Module
      # We can obviously compare against Modules but result is always false
      raise TypeError, "instance_of? requires a Class argument"
    end

    Rubinius::Type.object_class(self) == cls
  end

  def instance_variable_get(sym)
    Truffle.primitive :object_get_ivar

    sym = Rubinius::Type.ivar_validate sym
    instance_variable_get sym
  end

  alias_method :__instance_variable_get__, :instance_variable_get

  def instance_variable_set(sym, value)
    Truffle.primitive :object_set_ivar

    sym = Rubinius::Type.ivar_validate sym
    instance_variable_set sym, value
  end

  alias_method :__instance_variable_set__, :instance_variable_set

  def instance_variables
    ary = []
    __all_instance_variables__.each do |sym|
      ary << sym if sym.is_ivar?
    end

    ary
  end

  def instance_variable_defined?(name)
    Truffle.primitive :object_ivar_defined

    instance_variable_defined? Rubinius::Type.ivar_validate(name)
  end

  # Both of these are for defined? when used inside a proxy obj that
  # may undef the regular method. The compiler generates __ calls.
  alias_method :__instance_variable_defined_p__, :instance_variable_defined?
  alias_method :__respond_to_p__, :respond_to?

  alias_method :is_a?, :kind_of?

  def lambda
    env = nil

    Rubinius.asm do
      push_block
      # assign a pushed block to the above local variable "env"
      # Note that "env" is indexed at 0.
      set_local 0
    end

    raise ArgumentError, "block required" unless env

    prc = Rubinius::Mirror::Proc.from_block ::Proc, env

    # Make a proc lambda only when passed an actual block (ie, not using the
    # "&block" notation), otherwise don't modify it at all.
    prc.lambda_style! if env.is_a?(Rubinius::BlockEnvironment)

    return prc
  end
  module_function :lambda

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

    begin
      while true
        yield
      end
    rescue StopIteration
    end
  end
  module_function :loop

  def method(name)
    name = Rubinius::Type.coerce_to_symbol name
    code = Rubinius.find_method(self, name)

    if code
      Method.new(self, code[1], code[0], name)
    elsif respond_to_missing?(name, true)
      Method.new(self, self.class, Rubinius::MissingMethod.new(self,  name), name)
    else
      raise NameError, "undefined method `#{name}' for class #{self.class}"
    end
  end

  def methods(all=true)
    methods = singleton_methods(all)

    if all
      # We have to special case these because unlike true, false, nil,
      # Type.object_singleton_class raises a TypeError.
      case self
      when Fixnum, Symbol
        methods |= Rubinius::Type.object_class(self).instance_methods(true)
      else
        methods |= Rubinius::Type.object_singleton_class(self).instance_methods(true)
      end
    end

    return methods if kind_of?(ImmediateValue)

    undefs = []
    Rubinius::Type.object_singleton_class(self).method_table.filter_entries do |entry|
      undefs << entry.name.to_s if entry.visibility == :undef
    end

    return methods - undefs
  end

  def nil?
    false
  end

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

  def printf(target, *args)
    if target.kind_of?(String)
      output = $stdout
    else
      output = target
      target = args.shift
    end
    output.write Rubinius::Sprinter.get(target).call(*args)
    nil
  end
  module_function :printf

  def private_methods(all=true)
    private_singleton_methods() | Rubinius::Type.object_class(self).private_instance_methods(all)
  end

  def private_singleton_methods
    sc = Rubinius::Type.object_singleton_class self
    methods = sc.method_table.private_names

    m = sc

    while m = m.direct_superclass
      unless Rubinius::Type.object_kind_of?(m, Rubinius::IncludedModule) or
             Rubinius::Type.singleton_class_object(m)
        break
      end

      methods.concat m.method_table.private_names
    end

    methods
  end
  private :private_singleton_methods

  def proc(&prc)
    raise ArgumentError, "block required" unless prc
    return prc
  end
  module_function :proc

  def protected_methods(all=true)
    protected_singleton_methods() | Rubinius::Type.object_class(self).protected_instance_methods(all)
  end

  def protected_singleton_methods
    m = Rubinius::Type.object_singleton_class self
    methods = m.method_table.protected_names

    while m = m.direct_superclass
      unless Rubinius::Type.object_kind_of?(m, Rubinius::IncludedModule) or
             Rubinius::Type.singleton_class_object(m)
        break
      end

      methods.concat m.method_table.protected_names
    end

    methods
  end
  private :protected_singleton_methods

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

  def public_methods(all=true)
    public_singleton_methods | Rubinius::Type.object_class(self).public_instance_methods(all)
  end

  def public_send(message, *args)
    Truffle.primitive :object_public_send
    raise PrimitiveFailure, "Kernel#public_send primitive failed"
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

  def remove_instance_variable(sym)
    Truffle.primitive :object_del_ivar

    # If it's already a symbol, then we're here because it doesn't exist.
    if sym.kind_of? Symbol
      raise NameError, "instance variable '#{sym}' not defined"
    end

    # Otherwise because sym isn't a symbol, coerce it and try again.
    remove_instance_variable Rubinius::Type.ivar_validate(sym)
  end

  def require(name)
    Rubinius::CodeLoader.require name
  end
  module_function :require

  def require_relative(name)
    scope = Rubinius::ConstantScope.of_sender
    Rubinius::CodeLoader.require_relative(name, scope)
  end
  module_function :require_relative

  def select(*args)
    IO.select(*args)
  end
  module_function :select

  def send(message, *args)
    Truffle.primitive :object_send
    raise PrimitiveFailure, "Kernel#send primitive failed"
  end

  def set_trace_func(*args)
    raise NotImplementedError
  end
  module_function :set_trace_func

  def sprintf(str, *args)
    Rubinius::Sprinter.get(str).call(*args)
  end
  module_function :sprintf

  alias_method :format, :sprintf
  module_function :format

  def sleep(duration=undefined)
    Truffle.primitive :vm_sleep

    # The primitive will fail on arg count if sleep is called
    # without an argument, so we call it again passing undefined
    # to mean "sleep forever"
    #
    if undefined.equal? duration
      return sleep(undefined)
    end

    if duration.kind_of? Numeric
      float = Rubinius::Type.coerce_to duration, Float, :to_f
      return sleep(float)
    else
      raise TypeError, 'time interval must be a numeric value'
    end
  end
  module_function :sleep

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

  def singleton_methods(all=true)
    m = Rubinius::Type.object_singleton_class self
    mt = m.method_table
    methods = mt.public_names + mt.protected_names

    if all
      while m = m.direct_superclass
        unless Rubinius::Type.object_kind_of?(m, Rubinius::IncludedModule) or
               Rubinius::Type.singleton_class_object(m)
          break
        end

        mt = m.method_table
        methods.concat mt.public_names
        methods.concat mt.protected_names
      end
    end

    methods.uniq
  end

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

  def to_s
    Rubinius::Type.infect("#<#{self.class}:0x#{self.__id__.to_s(16)}>", self)
  end

  def trace_var(name, cmd = nil, &block)
    if !cmd && !block
      raise(
        ArgumentError,
        'The 2nd argument should be a Proc/String, alternatively use a block'
      )
    end

    # We have to use a custom proc since set_hook passes in both the variable
    # name and value.
    set = proc do |_, value|
      if cmd.is_a?(String)
        eval(cmd)

      # In MRI if one passes both a proc in `cmd` and a block the latter will
      # be ignored.
      elsif cmd.is_a?(Proc)
        cmd.call(value)

      elsif block
        block.call(value)
      end
    end

    Rubinius::Globals.set_hook(name, :[], set)
  end
  module_function :trace_var

  # In MRI one can specify a 2nd argument to remove a specific tracer.
  # Rubinius::Globals however only supports one hook per variable, hence the
  # 2nd dummy argument.
  def untrace_var(name, *args)
    Rubinius::Globals.remove_hook(name)
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
      if bt = exc.backtrace and bt[1]
        pos = bt[1]
      else
        pos = Rubinius::VM.backtrace(1)[0].position
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
end
