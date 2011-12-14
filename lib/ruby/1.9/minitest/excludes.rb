require 'minitest/unit'

##
# minitest/excludes.rb extends MiniTest::Unit::TestCase to provide a
# clean API for excluding certain tests you don't want to run under
# certain conditions.
#
# For example, in test/test_xyz.rb you have:
#
#   class TestXYZ < MiniTest::Unit::TestCase
#     def test_good
#       # test that passes
#     end
#
#     def test_bad
#       # test that fails only on jruby
#     end
#   end
#
# For jruby runs, you can add test/excludes/TestXYZ.rb with:
#
#   exclude :test_bad, "Uses ObjectSpace" if jruby?
#
# The file is instance_eval'd on TestXYZ so you can call the exclude
# class method directly. Since it is ruby you can provide any sort
# of conditions you want to figure out if your tests should be
# excluded.
#
# TestCase.exclude causes test methods to call skip with the reason
# you provide. If you run your tests in verbose mode, you'll see a
# full report of the tests you've excluded.
#
# If you want to change where the exclude files are located, you can
# set the EXCLUDE_DIR environment variable.

class MiniTest::Unit::TestCase
  EXCLUDE_DIR = ENV['EXCLUDE_DIR'] || "test/excludes"

  ##
  # Exclude a test from a testcase. This is intended to be used by
  # exclusion files.

  def self.exclude name, reason = nil
    return warn "Method #{self}##{name} is not defined" unless
      method_defined? name
      
    __excludes__ << name

    alias_method :"old_#{name}", name

    define_method name do
      skip reason
    end
  end
  
  ##
  # Excluded methods for this class
  
  def self.__excludes__
    @__excludes__ ||= []
  end

  ##
  # Loads the exclusion file for the class, if any.

  def self.load_excludes
    @__load_excludes__ ||=
      begin
        if name and not name.empty? then
          file = File.join EXCLUDE_DIR, "#{name}.rb"
          if File.exist? file
            instance_eval File.read(file), file, 1
            
            # redefine setup and teardown to skip for excluded tests
            alias_method :old_setup, :setup
            define_method :setup do
              return if self.class.__excludes__.include? :"#{__name__}"
              old_setup
            end
            
            alias_method :old_teardown, :teardown
            define_method :teardown do
              return if self.class.__excludes__.include? :"#{__name__}"
              old_teardown
            end
          end
        end
        true
      end
  end

  class << self
    alias :old_test_methods :test_methods # :nodoc:

    def test_methods # :nodoc:
      load_excludes
      old_test_methods
    end
  end
end
