# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# These are implemented just to get other stuff working - we'll go back and
# implement these properly later.

# Here otherwise it causes problems for RubySpec
class Channel
end

module STDIN
  def self.external_encoding
    @external || Encoding.default_external
  end

  def self.internal_encoding
    @internal
  end

  def self.set_encoding(external, internal)
    @external = external
    @internal = internal
  end
end

module STDOUT
  def self.puts(*values)
    Kernel.send(:puts, *values)
  end

  def self.print(*values)
    Kernel.send(:print, *values)
  end

  def self.printf(*values)
    Kernel.send(:printf, *values)
  end

  def self.write(value)
    print value
  end

  def self.flush
    Truffle::Debug.flush_stdout
  end

  def self.sync
    false
  end

  def self.sync=(value)
  end

  def self.external_encoding
    @external
  end

  def self.internal_encoding
    @internal
  end

  def self.set_encoding(external, internal)
    @external = external
    @internal = internal
  end
end

$stdout = STDOUT

module STDERR
  def self.puts(*values)
    Kernel.send(:puts, *values)
  end

  def self.external_encoding
    @external
  end

  def self.internal_encoding
    @internal
  end

  def self.set_encoding(external, internal)
    @external = external
    @internal = internal
  end
end

ARGF = Object.new

class Hash

  def fetch(key, default=nil)
    if key?(key)
      self[key]
    elsif block_given?
      yield(key)
    elsif default
      default
    else
      raise(KeyError, "key not found: #{key}")
    end
  end

  def each_key
    each do |key, value|
      yield key
    end
  end

  def each_value
    each do |key, value|
      yield value
    end
  end

  def value?(value)
    values.any? { |v| v == value }
  end

  alias_method :has_value?, :value?

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
  def gsub(*args)
    dup.gsub!(*args)
  end
end