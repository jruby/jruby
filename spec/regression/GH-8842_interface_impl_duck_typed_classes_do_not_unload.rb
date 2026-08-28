# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/8842

# A script to cause exactly one InterfaceImpl class to be loaded after boot.
# NOTE: If future changes in JI cause incidental InterfaceImpl classes, this may start to fail.
src = <<~RUBY
  require 'jruby'

  puts 'started'

  # do this in a block so the object goes fully out of scope
  tap do
    cls = Class.new do
      def accept(*)
      end
    end
    obj = cls.new
    java.util.ArrayList.new([1]).for_each(obj)
  end
  
  # GC a sufficient number of times to clear the impl class
  10.times do
    JRuby.gc
  end
  
  puts 'exiting'
RUBY

def run_script_get_output(class_values, src)
  pout, cout = IO.pipe

  pid = spawn("jruby --dev -J-XX:+TraceClassLoading -J-XX:+TraceClassUnloading -Xji.class.values=#{class_values} -e \"#{src}\"", { out: cout })
  cout.close
  output = pout.read
  Process.wait pid
  output
end

describe 'Duck-typed interface implementation classes' do
  describe 'when using stable JI class values' do
    it 'are unloaded after being dereferenced' do
      output = run_script_get_output("STABLE", src)

      # match only impl classes loaded and unloaded after script starts
      output =~ /started.*Loaded (org.jruby.gen.InterfaceImpl[0-9+]+).*exiting/m

      expect(output).to_not be_nil
      expect(output).to match /Unloading class #{$1}/
    end
  end

  describe 'when using hard mapped JI class values' do
    it 'are not unloaded after being dereferenced' do
      output = run_script_get_output("HARD_MAP", src)

      # match only impl classes loaded and unloaded after script starts
      output =~ /started.*Loaded (org.jruby.gen.InterfaceImpl[0-9+]+).*exiting/m

      expect(output).to_not be_nil
      expect(output).to_not match /Unloading class #{$1}/
    end
  end
end
