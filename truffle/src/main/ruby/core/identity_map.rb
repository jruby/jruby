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

module Rubinius

  # IdentityMap is customized for uniquely storing elements from an Array to
  # implement the following Array methods: #&, #|, #-, #uniq, and #uniq!
  #
  # The algorithm used is double hashing with an overflow array. For each
  # array element, the element, its hash value, and its ordinal in the
  # sequence of elements added to the map are stored.
  #
  # Methods are provided to test an element for inclusion in the map and to
  # delete an entry from the map. The contents of a map can be returned as an
  # array in the order the elements were added to the map.

  class IdentityMap
    attr_reader :size

    Row = Table = Array
    MIN_CAPACITY = 64
    MIN_ROW = 10
    ROW_GROWTH = 9

    # Converts one or more Enumerable instances to a single IdentityMap
    def self.from(*arrays, &block)
      im = allocate
      Truffle.privately { im.load(arrays, &block) }
      im
    end

    def initialize
      capacity = MIN_CAPACITY
      @table = Table.new capacity
      @mask = capacity - 4
      @max = capacity
      @size = 0
    end

    # Adds +item+ to the IdentityMap if it does not already exist. May cause
    # a row to be added or enlarged. Returns +self+.
    def insert(item, &block)
      redistribute if @size > @max

      if block_given?
        item_hash = yield(item).hash
      else
        item_hash = item.hash
      end

      index = item_hash & @mask
      table = @table

      if num_entries = table[index]
        index += 1

        if num_entries == 1
          return self if match?(table, index, item, item_hash, &block)

          table[index-1] = 2
          table[index] = promote_row table, index, item, item_hash, @size
        else
          i = 1
          row = table[index]
          total = row[0]

          while i < total
            return self if match?(row, i, item, item_hash, &block)
            i += 3
          end

          if total == row.size
            table[index] = enlarge_row row, item, item_hash, @size
          else
            i = row[0]
            set_item row, i, item, item_hash, @size
            row[0] = i + 3
          end
        end
      else
        table[index] = 1
        set_item table, index+1, item, item_hash, @size
      end
      @size += 1

      self
    end

    # Returns +true+ if +item+ is in the IdentityMap, +false+ otherwise.
    def include?(item)
      item_hash = item.hash

      index = item_hash & @mask
      table = @table
      if num_entries = table[index]
        index += 1

        if num_entries == 1
          return true if match? table, index, item, item_hash
        else
          row = table[index]
          i = 1
          total = row[0]
          while i < total
            return true if match? row, i, item, item_hash
            i += 3
          end
        end
      end

      false
    end

    # If +item+ is in the IdentityMap, removes it and returns +true+.
    # Otherwise, returns +false+.
    def delete(item)
      item_hash = item.hash

      index = item_hash & @mask
      table = @table

      if num_entries = table[index]
        index += 1
        if num_entries == 1
          if match? table, index, item, item_hash
            table[index] = nil
            @size -= 1
            return true
          end
        else
          row = table[index]
          i = 1
          total = row[0]
          while i < total
            if match? row, i, item, item_hash
              row[i] = nil
              @size -= 1
              return true
            end
            i += 3
          end
        end
      end

      false
    end

    # Returns an Array containing all items in the IdentityMap in the order
    # in which they were added to the IdentityMap.
    def to_array
      array = Array.new @size

      i = 0
      table = @table
      total = table.size

      while i < total
        if num_entries = table[i]
          if num_entries == 1
            array[table[i+3]] = table[i+2] if table[i+1]
          else
            row = table[i+1]
            k = row[0]
            j = 1
            while j < k
              array[row[j+2]] = row[j+1] if row[j]
              j += 3
            end
          end
        end

        i += 4
      end

      array
    end

    # Private implementation methods

    def resize(total)
      capacity = MIN_CAPACITY
      while capacity < total
        capacity <<= 2
      end

      @table = Table.new capacity
      @mask = capacity - 4
      @max = capacity
    end
    private :resize

    def redistribute
      table = @table
      resize @size

      i = 0
      total = table.size

      while i < total
        if num_entries = table[i]
          if num_entries == 1
            if item_hash = table[i+1]
              add_item table[i+2], item_hash, table[i+3]
            end
          else
            row = table[i+1]
            k = row[0]
            j = 1
            while j < k
              if item_hash = row[j]
                add_item row[j+1], item_hash, row[j+2]
              end
              j += 3
            end
          end
        end

        i += 4
      end
    end
    private :redistribute

    def add_item(item, item_hash, ordinal)
      index = item_hash & @mask
      table = @table

      if num_entries = table[index]
        index += 1

        if num_entries == 1
          table[index-1] = 2
          table[index] = promote_row table, index, item, item_hash, ordinal
        else
          row = table[index]
          i = row[0]

          if i == row.size
            table[index] = enlarge_row row, item, item_hash, ordinal
          else
            set_item row, i, item, item_hash, ordinal
            row[0] = i + 3
          end
        end
      else
        table[index] = 1
        set_item table, index+1, item, item_hash, ordinal
      end
    end
    private :add_item

    # Given an Array of Enumerable instances, computes a bounding set
    # to contain them and then adds each item to the IdentityMap.
    def load(arrays, &block)
      resize(arrays.inject(0) { |sum, array| sum + array.size })
      @size = 0

      arrays.each do |array|
        array.each { |item| insert(item, &block) }
      end
    end
    private :load

    def match?(table, index, item, item_hash)
      return false unless table[index] == item_hash
      other = table[index+1]
      if block_given?
        item = yield item
        other = yield other
      end
      Rubinius::Type.object_equal(item, other) or item.eql?(other)
    end
    private :match?

    def set_item(table, index, item, item_hash, ordinal)
      table[index]   = item_hash
      table[index+1] = item
      table[index+2] = ordinal
    end
    private :set_item

    def promote_row(row, index, item, item_hash, ordinal)
      new_row = Row.new MIN_ROW

      new_row[0] = 7
      new_row[1] = row[index]
      new_row[2] = row[index+1]
      new_row[3] = row[index+2]
      new_row[4] = item_hash
      new_row[5] = item
      new_row[6] = ordinal

      new_row
    end
    private :promote_row

    def enlarge_row(row, item, item_hash, ordinal)
      new_row = Row.new row.size + ROW_GROWTH
      new_row[0, row.size] = row

      index = row[0]
      new_row[0] = index + 3
      set_item new_row, index, item, item_hash, ordinal

      new_row
    end
    private :enlarge_row
  end
end
