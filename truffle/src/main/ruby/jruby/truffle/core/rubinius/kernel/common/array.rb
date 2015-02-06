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

# Only part of Rubinius' array.rb

class Array

  def self.[](*args)
    ary = allocate
    ary.replace args
    ary
  end

  # Modified implementation until we support Rubinius::IdentityMap.
  def &(other)
    other = Rubinius::Type.coerce_to other, Array, :to_ary

    array = []

    each { |x| array << x if other.include? x }

    array
  end


  def values_at(*args)
    out = []

    args.each do |elem|
      # Cannot use #[] because of subtly different errors
      if elem.kind_of? Range
        finish = Rubinius::Type.coerce_to_collection_index elem.last
        start = Rubinius::Type.coerce_to_collection_index elem.first

        start += @total if start < 0
        next if start < 0

        finish += @total if finish < 0
        finish -= 1 if elem.exclude_end?

        next if finish < start

        start.upto(finish) { |i| out << at(i) }

      else
        i = Rubinius::Type.coerce_to_collection_index elem
        out << at(i)
      end
    end

    out
  end

  def first(n = undefined)
    return at(0) if undefined.equal?(n)

    n = Rubinius::Type.coerce_to_collection_index n
    raise ArgumentError, "Size must be positive" if n < 0

    Array.new self[0, n]
  end

  def hash
    hash_val = size
    mask = Fixnum::MAX >> 1

    # This is duplicated and manually inlined code from Thread for performance
    # reasons. Before refactoring it, please benchmark it and compare your
    # refactoring against the original.

    id = object_id
    objects = Thread.current.recursive_objects

    # If there is already an our version running...
    if objects.key? :__detect_outermost_recursion__

      # If we've seen self, unwind back to the outer version
      if objects.key? id
        raise Thread::InnerRecursionDetected
      end

      # .. or compute the hash value like normal
      begin
        objects[id] = true

        each { |x| hash_val = ((hash_val & mask) << 1) ^ x.hash }
      ensure
        objects.delete id
      end

      return hash_val
    else
      # Otherwise, we're the outermost version of this code..
      begin
        objects[:__detect_outermost_recursion__] = true
        objects[id] = true

        each { |x| hash_val = ((hash_val & mask) << 1) ^ x.hash }

        # An inner version will raise to return back here, indicating that
        # the whole structure is recursive. In which case, abondon most of
        # the work and return a simple hash value.
      rescue Thread::InnerRecursionDetected
        return size
      ensure
        objects.delete :__detect_outermost_recursion__
        objects.delete id
      end
    end

    return hash_val
  end

end