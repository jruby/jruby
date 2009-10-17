#!/usr/bin/env ruby
#--
# $Idaemons: /home/cvs/rb/generator.rb,v 1.8 2001/10/03 08:54:32 knu Exp $
# $RoughId: generator.rb,v 1.10 2003/10/14 19:36:58 knu Exp $
# $Id$
#++
#
# = generator.rb: convert an internal iterator to an external one
#
# Copyright (c) 2001,2003 Akinori MUSHA <knu@iDaemons.org>
# Copyright (c) 2009 MenTaLguY <mental@rydia.net>
#
# All rights reserved.  You can redistribute and/or modify it under
# the same terms as Ruby.
#
# == Overview
#
# This library provides the Generator class, which converts an
# internal iterator (i.e. an Enumerable object) to an external
# iterator.  In that form, you can roll many iterators independently.
#
# The SyncEnumerator class, which is implemented using Generator,
# makes it easy to roll many Enumerable objects synchronously.
#
# See the respective classes for examples of usage.


#
# Generator converts an internal iterator (i.e. an Enumerable object)
# to an external iterator.
#
# == Example
#
#   require 'generator'
#
#   # Generator from an Enumerable object
#   g = Generator.new(['A', 'B', 'C', 'Z'])
#
#   while g.next?
#     puts g.next
#   end
#
#   # Generator from a block
#   g = Generator.new { |g|
#     for i in 'A'..'C'
#       g.yield i
#     end
#
#     g.yield 'Z'
#   }
#
#   # The same result as above
#   while g.next?
#     puts g.next
#   end
#
require 'thread'

class Generator
  include Enumerable

  # marks the end of enumeration
  END_MARKER = Object.new
  def END_MARKER.inspect ; "END_MARKER" ; end

  # a queue which emulates the producer-side interface of a generator
  class ProducerQueue < SizedQueue
    def initialize
      super(1)
    end

    alias yield push

    def _run_enum(enum)
      _run { enum.each { |x| self.yield(x) } }
    end

    def _run
      # caller manages thread, to avoid circularity
      Thread.new do
        begin
          yield self
        ensure
          self.yield(END_MARKER)
        end
      end
    end
  end

  # Creates a new generator either from an Enumerable object or from a
  # block.
  #
  # In the former, block is ignored even if given.
  #
  # In the latter, the given block is called with the generator
  # itself, and expected to call the +yield+ method for each element.
  def initialize(enum = nil, &block)
    @thread.kill if @thread

    @queue = ProducerQueue.new
    @got_next_element = false
    @next_element = nil
    @index = 0
    
    @enum = enum
    @block = block
    if enum
      @thread = @queue._run_enum(enum)
    else
      @thread = @queue._run(&block)
    end

    self
  end

  # Yields an element to the generator.
  def yield(value)
    @queue.yield(value)
    self
  end

  # gets the next element; may block
  def _next_element
    unless @got_next_element
      @next_element = @queue.pop
      @got_next_element = true
    end
    @next_element
  end

  # Returns true if the generator has reached the end.
  def end?()
    END_MARKER.equal? _next_element
  end

  # Returns true if the generator has not reached the end yet.
  def next?()
    !end?
  end

  # Returns the current index (position) counting from zero.
  def index()
    @index
  end
  alias pos index

  # Returns the element at the current position and moves forward.
  def next()
    result = current
    @index += 1
    @got_next_element = false
    @next_element = nil
    result
  end

  # Returns the element at the current position.
  def current()
    raise EOFError, "no more elements available" if end?
    _next_element
  end

  # Rewinds the generator.
  def rewind()
    initialize(@enum, &@block) if @index.nonzero?
    self
  end

  # Rewinds the generator and enumerates the elements.
  def each
    rewind

    until end?
      yield self.next
    end

    self
  end

  if RUBY_VERSION =~ /^1\.9/
    Enumerator = ::Enumerator
  else
    Enumerator = Enumerable::Enumerator
  end

  class Enumerator
    def __generator
      @generator ||= Generator.new(self)
    end
    private :__generator

    # call-seq:
    #   e.next   => object
    #
    # Returns the next object in the enumerator, and move the internal
    # position forward.  When the position reached at the end,
    # StopIteration is raised until the enumerator is rewound.
    #
    # Note that enumeration sequence by next method does not affect other
    # non-external enumeration methods, unless underlying iteration
    # methods itself has side-effect, e.g. IO#each_line.
    def next
      g = __generator
      begin
        g.next
      rescue EOFError
        raise StopIteration, 'iteration reached at end'
      end
    end

    # call-seq:
    #   e.rewind   => e
    #
    # Rewinds the enumeration sequence by the next method.
    def rewind
      __generator.rewind
      self
    end
  end

  #
  # SyncEnumerator creates an Enumerable object from multiple Enumerable
  # objects and enumerates them synchronously.
  #
  # == Example
  #
  #   require 'generator'
  #
  #   s = SyncEnumerator.new([1,2,3], ['a', 'b', 'c'])
  #
  #   # Yields [1, 'a'], [2, 'b'], and [3,'c']
  #   s.each { |row| puts row.join(', ') }
  #
  class SyncEnumerator
    include Enumerable

    # Creates a new SyncEnumerator which enumerates rows of given
    # Enumerable objects.
    def initialize(*enums)
      @gens = enums.map { |e| Generator.new(e) }
    end

    # Returns the number of enumerated Enumerable objects, i.e. the size
    # of each row.
    def size
      @gens.size
    end

    # Returns the number of enumerated Enumerable objects, i.e. the size
    # of each row.
    def length
      @gens.length
    end

    # Returns true if the given nth Enumerable object has reached the
    # end.  If no argument is given, returns true if any of the
    # Enumerable objects has reached the end.
    def end?(i = nil)
      if i.nil?
        @gens.detect { |g| g.end? } ? true : false
      else
        @gens[i].end?
      end
    end

    # Enumerates rows of the Enumerable objects.
    def each
      @gens.each { |g| g.rewind }

      loop do
        count = 0

        ret = @gens.map { |g|
    if g.end?
      count += 1
      nil
    else
      g.next
    end
        }

        if count == @gens.size
    break
        end

        yield ret
      end

      self
    end
  end
end
