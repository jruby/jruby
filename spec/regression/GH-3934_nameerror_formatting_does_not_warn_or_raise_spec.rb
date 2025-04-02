require 'stringio'
require 'tempfile'

describe "NameError created internally using a format string" do
  it "does not warn in verbose mode" do
    begin
      old_verbose, $VERBOSE = $VERBOSE, true
      old_stderr = $stderr.dup
      io = Tempfile.new('gh-3934')
      $stderr.reopen(io)

      expect(->{DoesNotExist}).to raise_error(NameError)
      expect(->{String::DoesNotExist}).to raise_error(NameError)
      expect(->{String.remove_instance_variable(:@foo)}).to raise_error(NameError)
      expect(->{String.instance_variable_get(:foo)}).to raise_error(NameError)
      expect(->{String.remove_class_variable(:@@foo)}).to raise_error(NameError)
      expect(->{String.class_variable_get(:foo)}).to raise_error(NameError)
      expect(->{method_that_does_not_exist}).to raise_error(NameError)
      expect(->{method_that_does_not_exist(1,2,3)}).to raise_error(NoMethodError)
      io.rewind
      expect(io.read).to eq ""
    ensure
      io.close rescue nil
      $VERBOSE = old_verbose
      $stderr.reopen(old_stderr)
    end
  end

  it "does not raise formatting errors in debug mode" do
    begin
      old_debug, $DEBUG = $DEBUG, true
      old_stderr = $stderr.dup
      io = Tempfile.new('gh-3934')
      $stderr.reopen(io)

      expect(->{DoesNotExist}).to raise_error(NameError)
      expect(->{String::DoesNotExist}).to raise_error(NameError)
      expect(->{String.remove_instance_variable(:@foo)}).to raise_error(NameError)
      expect(->{String.instance_variable_get(:foo)}).to raise_error(NameError)
      expect(->{String.remove_class_variable(:@@foo)}).to raise_error(NameError)
      expect(->{String.class_variable_get(:foo)}).to raise_error(NameError)
      expect(->{method_that_does_not_exist}).to raise_error(NameError)
      expect(->{method_that_does_not_exist(1,2,3)}).to raise_error(NoMethodError)
      io.rewind
      expect(io.read).to eq ""
    ensure
      io.close rescue nil
      $DEBUG = old_debug
      $stderr.reopen(old_stderr)
    end
  end
end