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

module Rubinius
  class LRUCache
    class Entry
      attr_reader :hits
      attr_reader :key
      attr_accessor :value
      attr_accessor :next_entry
      attr_accessor :prev_entry

      def initialize(key, value)
        @key = key
        @value = value
        @hits = 0
        @next_entry = nil
        @prev_entry = nil
      end

      def insert_after(entry)
        nxt = entry.next_entry

        @prev_entry = entry
        @next_entry = nxt

        entry.next_entry = self
        nxt.prev_entry = self if nxt
      end

      def insert_before(entry)
        prev = entry.prev_entry

        @prev_entry = prev
        @next_entry = entry

        entry.prev_entry = self
        prev.next_entry = self if prev
      end

      def detach!
        @next_entry.prev_entry = @prev_entry if @next_entry
        @prev_entry.next_entry = @next_entry if @prev_entry

        @next_entry = nil
        @prev_entry = nil
      end

      def become_first!
        @prev_entry = nil
      end

      def inc!
        @hits += 1
      end
    end

    def initialize(total)
      @cache = {}
      @total = total
      @current = 0

      @head = Entry.new(nil, nil)
      @tail = Entry.new(nil, nil)

      @tail.insert_after(@head)

      @misses = 0
    end

    attr_reader :current
    attr_reader :misses

    def clear!
      Rubinius.synchronize(self) do
        @cache = {}
        @current = 0

        @head = Entry.new(nil, nil, -1)
        @tail = Entry.new(nil, nil, -2)

        @tail.insert_after(@head)
      end
    end

    def explain
      entry = @head.next_entry
      while entry != @tail
        str, layout = entry.key
        puts "hits: #{entry.hits}"
        puts "layout: #{layout.inspect}"
        puts "<STRING>"
        puts str
        puts "</STRING>"

        entry = entry.next_entry
      end
    end

    def retrieve(key)
      Rubinius.synchronize(self) do
        if entry = @cache[key]
          entry.inc!

          entry.detach!
          entry.insert_before @tail

          return entry.value
        end

        @misses += 1

        nil
      end
    end

    def set(key, value)
      Rubinius.synchronize(self) do
        if entry = @cache[key]
          entry.value = value

          entry.detach!
          entry.insert_before @tail

          return value
        end

        if @current == @total
          entry = @head.next_entry

          entry.detach!

          @cache.delete entry.key
        else
          @current += 1
        end

        entry = Entry.new(key, value)

        entry.insert_before @tail

        @cache[key] = entry
      end
    end
  end
end
