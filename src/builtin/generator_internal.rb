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
  
  # Returns true if the generator has not reached the end yet.
  def next?()
    !end?
  end

  # Returns the current index (position) counting from zero.
  def index()
    @index
  end
  alias pos index

  # Construct a new generator; defaults to the threaded impl
  def self.new(*args, &block)
    generator = (self == Generator) ? Threaded : self

    gen = generator.allocate
    gen.send :initialize, *args, &block
    gen
  end

  class Indexed < Generator
    SIMPLE_INDEXER = proc {|ary, i| ary[i]}
    def initialize(ary, &indexer)
      @ary = ary
      @index = 0
      @indexer = indexer || SIMPLE_INDEXER
    end

    def end?
      @index >= @ary.size
    end

    def next
      obj, @index = current, @index + 1
      obj
    end

    def current
      raise EOFError, "no more elements available" if end?
      @indexer.call(@ary, @index)
    end

    def rewind
      @index = 0
    end

    def each
      return enum_for(:each) unless block_given?
      
      for i in 0...@ary.size
        yield @indexer.call(i)
      end
    end
  end
  
  class Cursor < Generator
    ENDED = Object.new
    def initialize(start, succ_method, _end)
      @start = start
      @cur = start
      @end = _end
      @succ_method = succ_method
    end
    
    def end?
      @cur == ENDED
    end
    
    def next
      if @cur == ENDED
        raise StopIteration.new
      elsif @cur == @end
        obj, @cur = @cur, ENDED
      else
        obj, @cur = @cur, @cur.succ
      end
      obj
    end
    
    def current
      raise EOFError, "no more elements available" if end?
      @cur
    end
    
    def rewind
      @cur = @start
    end

    def each
      return enum_for(:each) unless block_given?

      until end?
        yield self.next
      end
    end
  end

  class Threaded < Generator
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
          rescue StopIteration
            self.clear
          ensure
            self.yield(END_MARKER)
          end
        end
      end
    end

    class QueueFinalizer
      attr_accessor :queue

      def initialize
        @queue = nil
      end

      def to_proc
        proc do
          @queue.shutdown! if @queue
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
      warning "Using inefficient threaded enumerator for #{enum.inspect}, consider writing a iterator" if $DEBUG
      @queue_finalizer = QueueFinalizer.new
      ObjectSpace.define_finalizer self, &@queue_finalizer
      _setup(enum, block)
    end

    def _setup(enum, block)
      @queue = ProducerQueue.new
      @queue_finalizer.queue = @queue

      @got_next_element = false
      @next_element = nil
      @index = 0

      @enum = enum
      @block = block
      @thread = nil

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
        unless @thread
          if @enum
            @thread = @queue._run_enum(@enum.each)
          else
            @thread = @queue._run(&@block)
          end
        end
        @next_element = @queue.pop
        if Exception === @next_element
          raise @next_element
        end
        @got_next_element = true
      end
      @next_element
    end

    # Returns true if the generator has reached the end.
    def end?()
      END_MARKER.equal? _next_element
    end

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
      if @index.nonzero?
        @queue.shutdown!
        begin
          @thread.join if @thread
        rescue Exception
        end
        _setup(@enum, @block)
      end
      self
    end

    # Rewinds the generator and enumerates the elements.
    def each(&block)
      return enum_for(:each) unless block_given?
      
      # if using the block form only, don't "next" for internal iteration
      if @block && !@enum
        @block.call Enumerator::Yielder.new(&block)
      else
        rewind

        until end?
          yield self.next
        end
      end
      
      self
    end
  end

  IS_RUBY_19 = RUBY_VERSION =~ /^1\.9/

  if IS_RUBY_19
    Enumerator = ::Enumerator
  else
    Enumerator = Enumerable::Enumerator
  end

  module Iterators
    module ClassMethods
      def indexed_iter(method, &block)
        ext_iter_method = :"iter_for_#{method}"

        define_method ext_iter_method do
          Generator::Indexed.new(self, &block)
        end
      end
      
      def cursor_iter(start_method, succ_method, end_method, method, &block)
        ext_iter_method = :"iter_for_#{method}"
        
        define_method ext_iter_method do
          Generator::Cursor.new(self.__send__(start_method), succ_method, self.__send__(end_method), &block)
        end
      end
    end
    def self.included(cls)
      cls.extend ClassMethods
    end
  end

  class ::Array
    include Iterators
    indexed_iter(:each)
    indexed_iter(:each_with_index) {|a,i| [ a[i], i ]}
    indexed_iter(:each_index) {|a,i| i}
    indexed_iter(:reverse_each) {|a,i| a[a.size - i - 1]}
  end

  class ::Hash
    include Iterators
    # these are inefficient and need a place to store 'keys'
    indexed_iter(:each) {|h,i| keys = h.keys; [ keys[i], h[keys[i]] ]}
    indexed_iter(:each_with_index) {|h,i| keys = h.keys; [ [ keys[i], h[keys[i]] ], i]}
  end

  class ::Range
    include Iterators
    cursor_iter(:first, :succ, :last, :each)
  end

  class Enumerator
    def __generator
      @generator ||= __choose_generator
    end

    def __choose_generator
      iter_for_method = :"iter_for_#{@__method__}"
      if ENV_JAVA['jruby.enumerator.lightweight'] != 'false' &&
          @__object__.respond_to?(iter_for_method)
        @__object__.send iter_for_method
      else
        Generator::Threaded.new(self)
      end
    end
    private :__generator, :__choose_generator

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
    if IS_RUBY_19
      def rewind
        @__object__.rewind if @__object__.respond_to? :rewind
        __generator.rewind
        self
      end

      def peek
        begin
          __generator.current
        rescue EOFError
          raise StopIteration, 'iteration reached at end'
        end
      end
    else
      def rewind
        __generator.rewind
        self
      end
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

  if IS_RUBY_19
    module ::Enumerable
      def slice_before(filter = (no_filter = true; nil), &block)
        if no_filter && !block
          raise ArgumentError.new("wrong number of arguments (0 for 1)")
        end

        if block
          if no_filter
            state = nil
          else
            initial_state = filter.dup
            state = initial_state
          end
        else
          state = nil
        end

        Enumerator.new do |yielder|
          ary = nil
          self.each do |elt|
            if block
              if no_filter
                state = block.call elt
              else
                state = block.call elt, initial_state
              end
            else
              state = (filter === elt)
            end

            if ary
              if state
                yielder.yield ary
                ary = [elt]
              else
                ary << elt
              end
            else
              ary = [elt]
            end
          end
          yielder.yield ary
        end
      end
    end
  end
end
