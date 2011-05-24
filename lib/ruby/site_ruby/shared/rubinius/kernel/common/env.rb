##
# Interface to process environment variables.

module Rubinius
  class EnvironmentVariables
    include Enumerable
    include Rubinius::EnvironmentAccess

    def [](key)
      getenv(StringValue(key)).freeze
    end

    def []=(key, value)
      key = StringValue(key)
      if value.nil?
        unsetenv(key)
      else
        setenv key, StringValue(value), 1
      end
      value
    end

    alias_method :store, :[]=

    def each_key
      return to_enum(:each_key) unless block_given?

      each { |k, v| yield k }
    end

    def each_value
      return to_enum(:each_value) unless block_given?

      each { |k, v| yield v }
    end

    def each
      return to_enum(:each) unless block_given?

      env = environ()
      ptr_size = FFI.type_size FFI.find_type(:pointer)

      i = 0

      offset = 0
      cur = env + offset

      until cur.read_pointer.null?
        entry = cur.read_pointer.read_string
        key, value = entry.split '=', 2
        value.taint if value
        key.taint if key

        yield key, value

        offset += ptr_size
        cur = env + offset
      end

      self
    end

    alias_method :each_pair, :each

    def delete(key)
      existing_value = self[key]
      self[key] = nil if existing_value
      existing_value
    end

    def delete_if(&block)
      return to_enum(:delete_it) unless block_given?
      reject!(&block)
      self
    end

    def fetch(key, absent=undefined)
      if block_given? and !absent.equal?(undefined)
        warn "block supersedes default value argument"
      end

      if value = self[key]
        return value
      end

      if block_given?
        return yield(key)
      elsif absent.equal?(undefined)
        raise IndexError, "key not found"
      end

      return absent
    end

    def include?(key)
      !self[key].nil?
    end

    alias_method :has_key?, :include?
    alias_method :key?, :include?
    # More efficient than using the one from Enumerable
    alias_method :member?, :include?

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
      return to_enum(:reject!) unless block_given?

      rejected = false
      each do |k, v|
        if yield(k, v)
          delete k
          rejected = true
        end
      end

      rejected ? self : nil
    end

    def clear
      # Avoid deleting from environ while iterating because the
      # OS can handle that in a million different bad ways.

      keys = []
      each { |k,v| keys << k }
      keys.each { |k| delete k }

      self
    end

    def has_value?(value)
      each { |k,v| return true if v == value }
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

    def keys
      keys = []
      each { |k,v| keys << k }
      keys
    end

    def values
      vals = []
      each { |k,v| vals << v }
      vals
    end

    def empty?
      each { return false }
      return true
    end

    def length
      sz = 0
      each { |k,v| sz += 1 }
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

    def shift
      env = environ()
      ptr_size = FFI.type_size FFI.find_type(:pointer)

      offset = 0
      cur = env + offset

      ptr = cur.read_pointer
      return nil unless ptr

      key, value = ptr.read_string.split "=", 2

      return nil unless key

      key.taint if key
      value.taint if value

      delete key

      return [key, value]
    end

    def to_a
      ary = []
      each { |k,v| ary << [k,v] }
      ary
    end

    def to_hash
      return environ_as_hash()
    end

    def update(other, &block)
      if block_given?
        other.each { |k, v| self[k] = yield(k, self[k], v) }
      else
        other.each { |k, v| self[k] = v }
      end
    end

    # Missing and deprecated: indexes, indices
  end
end
