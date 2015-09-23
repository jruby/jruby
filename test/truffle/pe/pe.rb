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

def example(code, expected_value, expected_constant=true, tagged=false)
  EXAMPLES << [code, expected_value, expected_constant, tagged]
end

def tagged_example(code, expected_value)
  example(code, expected_value, true, true)
end

def counter_example(code)
  example(code, nil, false, false)
end

def tagged_counter_example(code)
  example(code, nil, false, true)
end

example "14", 14
counter_example "rand"

require_relative 'language/controlflow_pe.rb'
require_relative 'language/closures_pe.rb'
require_relative 'language/constant_pe.rb'
require_relative 'language/ivar_pe.rb'
require_relative 'language/metaprogramming_pe.rb'
require_relative 'language/super_pe.rb'
require_relative 'core/truefalse_pe.rb'
require_relative 'core/fixnum_pe.rb'
require_relative 'core/float_pe.rb'
require_relative 'core/symbol_pe.rb'
require_relative 'core/method_pe.rb'
require_relative 'core/array_pe.rb'
require_relative 'core/hash_pe.rb'
require_relative 'core/eval_pe.rb'
require_relative 'core/send_pe.rb'
require_relative 'core/objectid_pe.rb'
require_relative 'core/binding_pe.rb'
require_relative 'macro/pushing_pixels_pe.rb'

tested = 0
failed = 0
errored = 0
timedout = 0

def report(status, code, message = nil)
  format_str = '%14s: %s'
  puts message ? format(format_str + "\n         %s", status, code, message) : format('%14s: %s', status, code)
end

EXAMPLES.each do |code, expected_value, expected_constant, tagged|
  next if tagged

  finished = false

  test_thread = Thread.new do
    begin
      tested += 1
      value = nil
      eval "loop { value = Truffle::Primitive.assert_constant begin; #{code}; end; Truffle::Primitive.assert_not_compiled; Thread.pass }"
    rescue RubyTruffleError => e
      if e.message.include? 'Truffle::Primitive.assert_not_compiled'
        constant = true
      elsif e.message.include? 'Truffle::Primitive.assert_constant'
        constant = false
      else
        constant = nil
      end

      if constant.nil?
        report 'ERROR', code, "errored in some unexpected way: #{e.message}"
        errored += 1
      else
        if expected_constant
          unless constant
            report 'FAILED', code, "wasn't constant"
            failed += 1
          else
            if value == expected_value
              report 'OK', code
            else
              report 'INCORRECT', code, "was: #{value.inspect} and not: #{expected_value.inspect}"
              failed += 1
            end
          end
        else
          if constant
            report 'QUERY', code, "wasn't supposed to be constant but it was (#{value.inspect})"
            failed += 1
          else
            report 'OK (counter)', code
          end
        end
      end
    ensure
      finished = true
    end
  end

  test_thread.join(5)

  unless finished
    report 'TIMEOUT', code, "didn't compile in time so I don't know if it's constant or not"
    timedout += 1
  end
end

puts "Tested #{tested}, #{EXAMPLES.select{|c,e,t| t}.size} tagged, #{failed} failed, #{errored} errored, #{timedout} timed out"

exit 1 unless failed.zero? && errored.zero? && timedout.zero?
