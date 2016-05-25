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

module ObjectSpace
  def self.find_object(query, callable)
    Truffle.primitive :vm_find_object
    raise PrimitiveFailure, "ObjectSpace#find_object primitive failed"
  end

  def self._id2ref(id)
    ary = []
    if find_object([:object_id, Integer(id)], ary) > 0
      return ary.first
    end

    return nil
  end

  def self.find_references(obj)
    ary = []
    find_object([:references, obj], ary)
    return ary
  end

  # @todo rewrite each_object

  # Tries to handle as much as it can.
  def self.each_object(what=nil, &block)
    return to_enum :each_object, what unless block_given?

    what ||= Object

    unless what.kind_of? Object
      raise TypeError, "class or module required"
    end

    case what
    when Fixnum, Symbol
      return 0
    when TrueClass
      yield true
      return 1
    when FalseClass
      yield false
      return 1
    when NilClass
      yield nil
      return 1
    else
      return find_object([:kind_of, what], block)
    end
  end

  def self.define_finalizer(obj, prc=nil, &block)
    prc ||= block

    if obj.equal? prc
      # This is allowed. This is the Rubinius specific API that calls
      # __finalize__ when the object is finalized.
    elsif !prc and obj.respond_to?(:__finalize__)
      # Allowed. Same thing as above
      prc = obj
    elsif !prc or !prc.respond_to?(:call)
      raise ArgumentError, "action must respond to call"
    end

    unless Truffle.invoke_primitive(:vm_set_finalizer, obj, prc)
      raise RuntimeError, "cannot define a finalizer for a #{obj.class}"
    end

    [0, prc]
  end

  def self.undefine_finalizer(obj)
    Truffle.invoke_primitive :vm_set_finalizer, obj, nil
    return obj
  end

  def self.run_finalizers
  end

  def self.garbage_collect
    GC.start
  end
end
