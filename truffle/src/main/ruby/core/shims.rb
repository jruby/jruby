# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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

STDIN = File.new(0)
STDOUT = File.new(1)
STDERR = File.new(2)

$stdin = STDIN
$stdout = STDOUT
$stderr = STDERR

class << STDIN
  def external_encoding
    super || Encoding.default_external
  end
end

if STDOUT.tty?
  STDOUT.sync = true
else
  at_exit do
    STDOUT.flush
  end
end

if STDERR.tty?
  STDERR.sync = true
else
  at_exit do
    STDERR.flush
  end
end

ARGF = Object.new

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

  def extended_modules(object)
    []
  end
end

class Module
  def extended_modules(object)
    []
  end
end

class String
  def append(other)
    self << other
  end

  # The version in Rubinius 2.4.1 is broken, but has since been fixed.  We'll monkey-patch here until we update to
  # a newer Rubinius in order to keep the number of direct source modifications low.
  def include?(needle)
    !!find_string(StringValue(needle), 0)
  end
end

class Rational
  alias :__slash__ :/
end

ENV['TZ'] = 'UTC'

class MatchData
  def full
    @cached_full ||= begin
      tuple = Rubinius::Tuple.new
      tuple << self.begin(0)
      tuple << self.end(0)
      tuple
    end
  end
end

# Wrapper class for Rubinius's exposure of @data within String.
#
# We can't use Array directly because we don't currently guarantee that we'll always return the same
# exact underlying byte array.  Rubinius calls #equal? rather than #== throughout its code, making a tighter
# assumption than we provide.  This wrapper provides the semantics we need in the interim.
module Rubinius
  class StringData
    attr_accessor :array

    def initialize(array)
      @array = array
    end

    def equal?(other)
      @array == other.array
    end

    alias_method :==, :equal?

    def size
      @array.size
    end

    def [](index)
      @array[index]
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

# Don't apply any synchronization at the moment

module Rubinius

  def self.synchronize(object)
    yield
  end

end

module Errno

  # TODO CS 18-Apr-15 this should be a separate class
  DomainError = EDOM

end

module Math
  DomainError = Errno::EDOM
end

$PROGRAM_NAME = $0
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
unless ENV['HOME']
  if ENV['HOMEDRIVE']
    ENV['HOME'] = if ENV['HOMEPATH']
                    ENV['HOMEDRIVE'] + ENV['HOMEPATH']
                  else
                    ENV['USERPROFILE']
                  end
  end
end