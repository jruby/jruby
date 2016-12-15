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

##
# Interface to process environment variables.

module Rubinius
  class EnvironmentVariables
    include Enumerable

    def initialize
      vars = Truffle::System.initial_environment_variables
      @variables = vars.map { |name| set_encoding(name) }
    end

    def size
      @variables.size
    end

    def [](key)
      value = Truffle::POSIX.getenv(StringValue(key))
      if value
        value = set_encoding(value)
      end
      value
    end

    def []=(key, value)
      key = StringValue(key)
      if value.nil?
        Truffle::POSIX.unsetenv(key)
        @variables.delete(key)
      else
        if Truffle::POSIX.setenv(key, StringValue(value), 1) != 0
          Errno.handle("setenv")
        end
        unless @variables.include?(key)
          @variables << set_encoding(key.dup)
        end
      end
      value
    end
    alias_method :store, :[]=

    def each
      return to_enum(:each) { size } unless block_given?

      @variables.each do |name|
        key = set_encoding(name)
        value = self[name]
        yield key, value
      end

      self
    end
    alias_method :each_pair, :each

    def each_key(&block)
      @variables.each(&block)
    end

    def each_value
      return to_enum(:each_value) { size } unless block_given?

      each { |k, v| yield v }
    end

    def delete(key)
      existing_value = self[key]
      if existing_value
        self[key] = nil
      elsif block_given?
        yield key
      end
      existing_value
    end

    def delete_if(&block)
      return to_enum(:delete_if) { size } unless block_given?
      reject!(&block)
      self
    end

    def shift
      key = @variables.first
      return nil unless key
      value = delete key

      key = set_encoding key
      value = set_encoding value

      return [key, value]
    end

    # More efficient than using the one from Enumerable
    def include?(key)
      !self[key].nil?
    end
    alias_method :has_key?, :include?
    alias_method :key?, :include?
    alias_method :member?, :include?

    def fetch(key, absent=undefined)
      if block_given? and !undefined.equal?(absent)
        warn "block supersedes default value argument"
      end

      if value = self[key]
        return value
      end

      if block_given?
        return yield(key)
      elsif undefined.equal?(absent)
        raise KeyError, "key not found"
      end

      return absent
    end

    def to_s
      "ENV"
    end

    def inspect
      to_hash.inspect
    end

    def reject(&block)
      to_hash.reject(&block)
    end

    def reject!
      return to_enum(:reject!) { size } unless block_given?

      # Avoid deleting from the environment while iterating.
      keys = []
      each { |k, v| keys << k if yield(k, v) }
      keys.each { |k| delete k }

      keys.empty? ? nil : self
    end

    def clear
      # Avoid deleting from the environment while iterating.
      keys = []
      each { |k, v| keys << k }
      keys.each { |k| delete k }

      self
    end

    def has_value?(value)
      each { |k, v| return true if v == value }
      return false
    end

    alias_method :value?, :has_value?

    def values_at(*params)
      params.map{ |k| self[k] }
    end

    def index(value)
      each do |k, v|
        return k if v == value
      end
      nil
    end

    def invert
      to_hash.invert
    end

    def key(value)
      index(value)
    end

    def keys
      keys = []
      each { |k, v| keys << k }
      keys
    end

    def values
      vals = []
      each { |k, v| vals << v }
      vals
    end

    def empty?
      each { return false }
      return true
    end

    def length
      sz = 0
      each { |k, v| sz += 1 }
      sz
    end

    alias_method :size, :length

    def rehash
      # No need to do anything, our keys are always strings
    end

    def replace(other)
      clear
      other.each { |k, v| self[k] = v }
    end

    def select(&blk)
      return to_enum { size } unless block_given?
      to_hash.select(&blk)
    end

    def to_a
      ary = []
      each { |k, v| ary << [k, v] }
      ary
    end

    def to_hash
      hsh = {}
      each { |k, v| hsh[k] = v }
      hsh
    end

    alias_method :to_h, :to_hash

    def update(other)
      if block_given?
        other.each { |k, v| self[k] = yield(k, self[k], v) }
      else
        other.each { |k, v| self[k] = v }
      end
    end

    def keep_if(&block)
      return to_enum(:keep_if) { size } unless block_given?
      select!(&block)
      self
    end

    def select!
      return to_enum(:select!) { size } unless block_given?
      reject! { |k, v| !yield(k, v) }
    end

    def assoc(key)
      key = StringValue(key)
      value = self[key]
      value ? [key, value] : nil
    end

    def rassoc(value)
      value = StringValue(value)
      key = index(value)
      key ? [key, value] : nil
    end

    def set_encoding(value)
      return unless value.kind_of? String
      locale = Encoding.find("locale")
      value = if Encoding.default_internal && value.ascii_only?
        value.encode Encoding.default_internal, locale
      elsif value.encoding != locale
        value.dup.force_encoding(locale)
      else
        value
      end
      value.taint unless value.tainted?
      value.freeze
    end
    private :set_encoding
  end
end

ENV = Rubinius::EnvironmentVariables.new

# JRuby uses this for example to make proxy settings visible to stdlib/uri/common.rb

ENV_JAVA = {}

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
