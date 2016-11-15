# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
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
#     Truffle::Graal.assert_constant expression
#   end
#
# Run with:
#
#   jt run --graal -J-Dgraal.TraceTruffleCompilation=true -J-Dgraal.TruffleCompilationExceptionsAreFatal=true -J-Dgraal.TruffleIterativePartialEscape=true test.rb

unless Truffle::Graal.graal?
  puts 'not running Graal'
  exit 1
end

TIMEOUT = 10

EXAMPLES = []

Example = Struct.new(:code, :expected_value, :expected_constant, :tagged, :main_thread)

def example(code, expected_value=nil)
  example = Example.new(code, expected_value, true, false, false)
  EXAMPLES << example
  example
end

def main_thread(example)
  example.main_thread = true
  example
end

def tagged(example)
  example.tagged = true
  example
end

def counter(example)
  example.expected_constant = false
  example
end

if ARGV.first
  require File.expand_path(ARGV.first)
else
  example '14', 14
  counter example 'rand'

  require_relative 'language/controlflow_pe.rb'
  require_relative 'language/closures_pe.rb'
  require_relative 'language/constant_pe.rb'
  require_relative 'language/ivar_pe.rb'
  require_relative 'language/metaprogramming_pe.rb'
  require_relative 'language/super_pe.rb'
  require_relative 'language/defined_pe.rb'
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
  require_relative 'core/frozen_pe.rb'
  require_relative 'core/block_given_pe.rb'
  require_relative 'core/string_pe.rb'
  require_relative 'core/class_pe'
  require_relative 'core/encoding_pe'
  require_relative 'interop/interop_pe'
  require_relative 'macro/pushing_pixels_pe.rb'
  
  if Truffle::Interop.mime_type_supported?('application/javascript')
    require_relative 'interop/js.rb'
  end
  
  if Truffle::Interop.mime_type_supported?('application/x-r')
    require_relative 'interop/r.rb'
  end

end

tested = 0
failed = 0
errored = 0
timedout = 0

def report(status, code, message = nil)
  format_str = '%14s: %s'
  puts message ? format(format_str + "\n         %s", status, code, message) : format('%14s: %s', status, code)
end

EXAMPLES.each do |example|
  next if example.tagged

  finished = false
  
  runner = proc do
    begin
      tested += 1
      eval "
      def test_pe_code
        value = Truffle::Graal.assert_constant(begin; #{example.code}; end)
        Truffle::Graal.assert_not_compiled
        value
      end"
      while true
        value = test_pe_code
      end
    rescue RubyTruffleError => e
      if e.message.include? 'Truffle::Graal.assert_not_compiled'
        constant = true
      elsif e.message.include? 'Truffle::Graal.assert_constant'
        constant = false
      else
        constant = nil
      end

      if constant.nil?
        report 'ERROR', example.code, "errored in some unexpected way: #{e.message}"
        errored += 1
      else
        if example.expected_constant
          unless constant
            report 'FAILED', example.code, "wasn't constant"
            failed += 1
          else
            if value == example.expected_value
              report 'OK', example.code
            else
              report 'INCORRECT', example.code, "was: #{$value.inspect} and not: #{example.expected_value.inspect}"
              failed += 1
            end
          end
        else
          if constant
            report 'QUERY', example.code, "wasn't supposed to be constant but it was (#{$value.inspect})"
            failed += 1
          else
            report 'OK (counter)', example.code
          end
        end
      end
    ensure
      finished = true
    end
  end

  if example.main_thread
    runner.call
  else
    test_thread = Thread.new do
      runner.call
    end

    test_thread.join(TIMEOUT)

    unless finished
      report 'TIMEOUT', example.code, "didn't compile in time so I don't know if it's constant or not"
      timedout += 1
    end
  end
end

puts "Tested #{tested}, #{EXAMPLES.select{|example| example.tagged}.size} tagged, #{failed} failed, #{errored} errored, #{timedout} timed out"

exit 1 unless failed.zero? && errored.zero? && timedout.zero?
