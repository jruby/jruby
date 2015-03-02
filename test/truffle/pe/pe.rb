# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This file relies on some implementation details of JRuby+Truffle and Truffle,
# so be careful as you edit. Every block that you pass to example must be
# unique - so you can't always build up examples by running in a loop or using
# helper method. truffle_assert_constant looks like a method but is replaced
# in the parser with a specific node.

# Definition of the DSL

module PETests

  def self.start
    @description_stack = []
    @failures = []
    @successes = []
    @dots = 0
  end

  def self.tests(&block)
    instance_eval &block
  end

  def self.describe(description)
    @description_stack.push description
    
    begin
      yield
    ensure
      @description_stack.pop
    end
  end

  def self.example(description)
    describe "#{description} is constant" do
      begin
        1_000_000.times do
          yield
        end

        @successes.push @description_stack.join(" ")
        print "."
      rescue RubyTruffleError
        @failures.push @description_stack.join(" ")
        print "E"
      ensure
        @dots += 1
        puts if @dots == 80
      end
    end
  end

  def self.counter_example(description)
    describe "#{description} is constant" do
      begin
        1_000_000.times do
          yield
        end

        @failures.push @description_stack.join(" ")
        print "E"
      rescue RubyTruffleError
        @successes.push @description_stack.join(" ")
        print "."
      ensure
        @dots += 1
        puts if @dots == 80
      end
    end
  end

  def self.broken_example(description)
    puts "warning: broken examples not run"
    #describe "#{description} is not constant" do
    #  inner_example do
    #    yield
    #  end
    #end
  end

  def self.finish
    print "\n"

    if @failures.empty?
      puts "success - #{@successes.length} passed"
      true
    else
      puts "failure - #{@failures.length} failed, #{@successes.length} passed"
      
      @failures.each do |message|
        puts "failed: #{message}"
      end

      false
    end
  end

end

PETests.start

# Test we're working

PETests.tests do

  describe "For example" do

    example "a fixnum literal" do
      truffle_assert_constant 14
    end

    counter_example "a call to #rand" do
      truffle_assert_constant rand
    end

  end

end

# Tests organised by class

$: << File.expand_path('..', __FILE__)

require "language/metaprogramming_pe.rb"
require "core/truefalse_pe.rb"
require "core/fixnum_pe.rb"
require "core/float_pe.rb"
require "core/symbol_pe.rb"
require "core/array_pe.rb"
require "core/hash_pe.rb"
require "core/kernel/set_trace_func_pe.rb"
require "macro/pushing_pixels_pe.rb"

# Finished

exit 1 unless PETests.finish
