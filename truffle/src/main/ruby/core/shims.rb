# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# These are implemented just to get other stuff working - we'll go back and
# implement these properly later.

class IO
  def external_encoding
    @external
  end

  def internal_encoding
    @internal
  end

end

if Truffle::Safe.io_safe?
  STDIN = File.new(0)
  STDOUT = File.new(1)
  STDERR = File.new(2)
else
  STDIN = nil
  STDOUT = nil
  STDERR = nil
end

$stdin = STDIN
$stdout = STDOUT
$stderr = STDERR

class << STDIN
  def external_encoding
    super || Encoding.default_external
  end
end

if Truffle::Safe.io_safe?
  if STDOUT.tty?
    STDOUT.sync = true
  else
    Truffle::Kernel.at_exit true do
      STDOUT.flush
    end
  end

  if STDERR.tty?
    STDERR.sync = true
  else
    Truffle::Kernel.at_exit true do
      STDERR.flush
    end
  end
end

class Regexp
  def self.last_match(n = nil)
    if n
      # TODO (nirvdrum Jan. 8, 2015) Make sure this supports symbol keys for named capture lookup.
      $~.values_at(n).first
    else
      $~
    end
  end
end

module Rubinius
  L64 = true
end

class Rational
  alias :__slash__ :/
end

module Rubinius
  class Mirror
    module Process
      def self.set_status_global(status)
        # Rubinius has: `::Thread.current[:$?] = status`
        $? = status
      end
    end
  end
end

# We use Rubinius's encoding subsystem for the most part, but we need to keep JRuby's up to date in case we
# delegate to any of their methods.  Otherwise, they won't see the updated encoding and return incorrect results.
class Encoding
  class << self
    alias_method :default_external_rubinius=, :default_external=

    def default_external=(enc)
      self.default_external_rubinius = enc
      self.default_external_jruby = enc
    end

    alias_method :default_internal_rubinius=, :default_internal=

    def default_internal=(enc)
      self.default_internal_rubinius = enc
      self.default_internal_jruby = enc
    end
  end
end

# We use Rubinius's encoding class hierarchy, but do the encoding conversion in Java.  In order to properly initialize
# the converter, we need to initialize in both Rubinius and JRuby.
class Encoding::Converter
  alias_method :initialize_rubinius, :initialize

  def initialize(*args)
    initialize_rubinius(*args)
    initialize_jruby(*args)
  end
end

class Rubinius::ByteArray

  alias_method :[], :get_byte
  alias_method :[]=, :set_byte

end

module Rubinius

  def self.synchronize(object, &block)
    Truffle::System.synchronized(object, &block)
  end

  def self.memory_barrier
    Truffle::System.full_memory_barrier
  end

end

module Errno

  # TODO CS 18-Apr-15 this should be a separate class
  DomainError = EDOM

end

module Math
  DomainError = Errno::EDOM
end

$$ = Process.pid

# IO::printf from Rubinius uses Rubinius::Sprinter

class IO

  def printf(fmt, *args)
    fmt = StringValue(fmt)
    write sprintf(fmt, *args)
  end

end

# Windows probably doesn't have a HOME env var, but Rubinius requires it in places, so we need
# to construct the value and place it in the hash.
#unless ENV['HOME']
#  if ENV['HOMEDRIVE']
#    ENV['HOME'] = if ENV['HOMEPATH']
#                    ENV['HOMEDRIVE'] + ENV['HOMEPATH']
#                  else
#                    ENV['USERPROFILE']
#                  end
#  end
#end

class Exception

  def to_s
    if message.nil?
      self.class.to_s
    else
      message.to_s
    end
  end

end

# Hack to let code run that try to invoke RubyGems directly.  We don't yet support RubyGems, but in most cases where
# this call would be made, we've already set up the $LOAD_PATH so the call would no-op anyway.
module Kernel
  def gem(*args)
  end
end

# Find out why Rubinius doesn't implement this
class Rubinius::ARGFClass

  def inplace_mode
    @ext
  end

  def inplace_mode=(ext)
    @ext = ext
  end

end

module Enumerable

  alias_method :min_internal, :min
  alias_method :max_internal, :max

end

# JRuby uses this for example to make proxy settings visible to stdlib/uri/common.rb

ENV_JAVA = {}

# The translator adds a call to Truffle.get_data to set up the DATA constant

module Truffle
  def self.get_data(path, offset)
    file = File.open(path)
    file.seek(offset)

    # I think if the file can't be locked then we just silently ignore
    file.flock(File::LOCK_EX | File::LOCK_NB)

    Truffle::Kernel.at_exit true do
      file.flock(File::LOCK_UN)
    end

    file
  end
end

module Truffle
  def self.load_arguments_from_array_kw_helper(array, kwrest_name, binding)
    array = array.dup

    last_arg = array.pop

    if last_arg.respond_to?(:to_hash)
      kwargs = last_arg.to_hash

      if kwargs.nil?
        array.push last_arg
        return array
      end

      raise TypeError.new("can't convert #{last_arg.class} to Hash (#{last_arg.class}#to_hash gives #{kwargs.class})") unless kwargs.is_a?(Hash)

      return array + [kwargs] unless kwargs.keys.any? { |k| k.is_a? Symbol }

      kwargs.select! do |key, value|
        symbol = key.is_a? Symbol
        array.push({key => value}) unless symbol
        symbol
      end
    else
      kwargs = {}
    end

    binding.local_variable_set(kwrest_name, kwargs) if kwrest_name
    array
  end

  def self.add_rejected_kwargs_to_rest(rest, kwargs)
    return if kwargs.nil?

    rejected = kwargs.select { |key, value|
      not key.is_a?(Symbol)
    }

    unless rejected.empty?
      rest.push rejected
    end
  end
end

def when_splat(cases, expression)
  cases.any? do |c|
    c === expression
  end
end

Truffle::Interop.export(:ruby_cext, Truffle::CExt)
