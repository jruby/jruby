# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# To diagnose a failing example
#
# Take the expression that failed and put it in test.rb with this code around
# it:
#
#   loop do
#     expression
#   end
#
# To actually see the failure (rather than just the non-constant code running)
# do:
#
#   loop do
#     Truffle::Primitive.assert_constant expression
#   end
#
# Run with:
#
#   jt run --graal -J-G:+TraceTruffleCompilation -J-G:+TruffleCompilationExceptionsAreFatal test.rb

EXAMPLES = []

def example(code, expected_constant=true, tagged=false)
  EXAMPLES << [code, expected_constant, tagged]
end

def counter_example(code)
  example(code, false, false)
end

def tagged_example(code)
  example(code, false, true)
end

example "14"
counter_example "rand"

require_relative 'language/closures_pe.rb'
require_relative 'language/metaprogramming_pe.rb'
require_relative 'core/truefalse_pe.rb'
require_relative 'core/fixnum_pe.rb'
require_relative 'core/float_pe.rb'
require_relative 'core/symbol_pe.rb'
require_relative 'core/array_pe.rb'
require_relative 'core/hash_pe.rb'
require_relative 'macro/pushing_pixels_pe.rb'

tested = 0
failed = 0
tagged = 0

EXAMPLES.each do |code, expected_constant, tagged|
  finished = false

  test_thread = Thread.new do
    begin
      tested += 1
      eval "loop { Truffle::Primitive.assert_constant #{code}; Truffle::Primitive.assert_not_compiled; Thread.pass }"
    rescue RubyTruffleError => e
      constant = e.message.include? 'Truffle::Primitive.assert_not_compiled'
      if expected_constant
        unless constant
          puts "FAILURE: #{code} wasn't constant"
          failed += 1
        end
      else
        if constant
          if tagged
            puts "QUERY: #{code} was tagged but it still passed"
          else
            puts "QUERY: #{code} wasn't supposed to be constant but it was"
            failed += 1
          end
        end
      end
    ensure
      finished = true
    end
  end

  test_thread.join(5)

  unless finished
    puts "TIMEOUT: #{code} didn't compile in time so I don't know if it's constant or not"
  end
end

puts "Tested #{tested}, #{EXAMPLES.select{|c,e,t| t}.size} tagged, #{failed} failed"

exit 1 unless failed.zero?
