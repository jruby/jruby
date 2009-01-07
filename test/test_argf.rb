require 'test/unit'
require 'test/test_helper'
require 'rbconfig'

# Since we haven't found a good way to instantiate multiple ARGF instances for testing,
# the approach here is to fork to another process, run few things, and be done with it.
# Given JRuby startup time, ARGF is not a terribly valuable feature, so this level of testing is enough.

SCRIPT = <<END_OF_SCRIPT

def test_equal(expected, actual)
  raise "Expected: \#{expected.inspect}, got: \#{actual.inspect}" unless expected == actual
end

def test_to_io
  raise "Could not coerce ARGF to IO" unless ARGF.to_io.is_a? IO
end

test_to_io

# should not raise anything
test_equal "1:1\\n", ARGF.gets

test_to_io

ARGF.each_with_index do |line, index|
  case index
  when 0 then test_equal "1:2", line
  when 1 then test_equal "2:1\\n", line
  when 2
    test_equal "2:2\\n", line
    break
  else raise 'Should never get here'
  end
end

test_equal nil, ARGF.gets

END_OF_SCRIPT

class TestArgf < Test::Unit::TestCase
  include TestHelper

  def test_argf_sanity
    begin
      File.open('__argf_script.rb', 'w') { |f| f.write SCRIPT }
      File.open('__argf_input_1', 'w') { |f| f.write "1:1\n1:2" }
      File.open('__argf_input_2', 'w') { |f| f.write "2:1\n2:2\n" }

      assert jruby("__argf_script.rb", "__argf_input_1", "__argf_input_2"),
             "Smoke test script for ARGF failed"
    ensure
      File.unlink '__argf_script.rb' rescue nil
      File.unlink '__argf_input_1' rescue nil
      File.unlink '__argf_input_2' rescue nil
      File.unlink '__argf_input_3' rescue nil
    end
  end

  def test_argf_cloning
    assert_equal "ARGF", ARGF.clone.to_s
  end
end
