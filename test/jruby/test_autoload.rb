require 'test/unit'

class TestAutoload < Test::Unit::TestCase

  def test_basic_autoload
    assert_nil Object.autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded.rb")
    assert_equal true, Object.const_defined?("Autoloaded")
    assert_nil Object.autoload?("Object::Autoloaded")
    assert_equal "#{File.dirname(__FILE__)}/autoloaded.rb", Object.autoload?(:Autoloaded)
    assert_equal(Class, Object::Autoloaded.class)
    # This should not really autoload since it is set for real
    Object.autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded2.rb")
    assert_equal(Class, Object::Autoloaded.class)
    assert_nil Object.autoload?(:Autoloaded) # already loaded

    # Set versus load (will not perform autoload)
    Object.autoload(:Autoloaded2, "#{File.dirname(__FILE__)}/autoloaded3.rb")
    Object.class_eval "Autoloaded2 = 3"
    assert_equal(3, Object::Autoloaded2)

    Object.autoload("Autoloaded4", "#{File.dirname(__FILE__)}/autoloaded4.rb")
    assert_equal(4, Object::Autoloaded4)

    Object.autoload("Autoloaded6", "#{File.dirname(__FILE__)}/autoloaded6.rb")
    assert_nothing_raised { Object.__send__(:remove_const, :Autoloaded6) }
    assert_raises(NameError) { Object::Autoloaded6 }
  end

  def test_autoload_explicit_require
    TestAutoload.autoload(:Autoloaded5, "#{File.dirname(__FILE__)}/autoloaded5.rb")
    assert TestAutoload.autoload?(:Autoloaded5)
    require "#{File.dirname(__FILE__)}/autoloaded5.rb"
    assert defined?(TestAutoload::Autoloaded5::VAL)
    assert_nil TestAutoload.autoload?(:Autoloaded5)
    assert_equal 5, TestAutoload::Autoloaded5::VAL
  end

  def test_overwrite_autoload
    assert_raise(LoadError) do
      Object.class_eval <<-EOS
        autoload :SomeClass, 'somefile'
        class SomeClass; def foo; true; end; end
      EOS
    end
  end

  # A constant declared with autoload and then defined by a direct require of the same
  # file must end up in the constant table itself; an UNDEF marker left there sends every
  # later lookup from another thread back through the load service.
  AUTOLOAD_REQUIRED = File.expand_path("autoload_required", __dir__)

  module Eager; end
  module Imported; end
  module Siblings; end
  module Circular; end

  def setup
    $autoload_required_loads = []
  end

  # The value in the module's constant table, :undef while it still holds the autoload marker.
  def table_slot(mod, name)
    require 'jruby'
    entry = JRuby.reference(mod).getConstantMap.get(name.to_s)
    return :missing if entry.nil?
    value = entry.value
    value.class # UNDEF has no metaclass and fails here
    value
  rescue java.lang.NullPointerException
    :undef
  end

  def in_thread
    Thread.new do
      Thread.current.report_on_exception = false
      yield
    end.value
  end

  def test_explicit_require_stores_constant_in_table
    path = "#{AUTOLOAD_REQUIRED}/table_slot.rb"
    TestAutoload.autoload(:TableSlot, path)
    require path
    assert_equal [path], $autoload_required_loads
    assert_nil TestAutoload.autoload?(:TableSlot)
    assert_same TestAutoload::TableSlot, table_slot(TestAutoload, :TableSlot)
    assert_same TestAutoload::TableSlot, in_thread { TestAutoload::TableSlot }
    assert_equal [path], $autoload_required_loads
  end

  def test_explicit_require_colon2_definition_stores_constant_in_table
    path = "#{AUTOLOAD_REQUIRED}/colon2.rb"
    TestAutoload.autoload(:Colon2, path)
    require path
    assert_nil TestAutoload.autoload?(:Colon2)
    assert_same TestAutoload::Colon2, table_slot(TestAutoload, :Colon2)
  end

  # Assignment never went through the class-definition lookup; kept as a guard.
  def test_explicit_require_assignment_stores_constant_in_table
    path = "#{AUTOLOAD_REQUIRED}/assigned.rb"
    TestAutoload.autoload(:Assigned, path)
    require path
    assert_nil TestAutoload.autoload?(:Assigned)
    assert_same TestAutoload::Assigned, table_slot(TestAutoload, :Assigned)
  end

  def test_explicit_require_keeps_private_constant
    path = "#{AUTOLOAD_REQUIRED}/private.rb"
    TestAutoload.autoload(:Private, path)
    TestAutoload.send(:private_constant, :Private)
    require path
    assert_nil TestAutoload.autoload?(:Private)
    assert_raise(NameError) { TestAutoload::Private }
    assert_raise(NameError) { in_thread { TestAutoload::Private } }
    assert_same TestAutoload.const_get(:Private), table_slot(TestAutoload, :Private)
  end

  # Kernel#load re-runs the file through the autoload; the slot was already right. Guard.
  def test_explicit_load_finishes_the_autoload
    path = "#{AUTOLOAD_REQUIRED}/loaded.rb"
    TestAutoload.autoload(:Loaded, path)
    load path
    assert_nil TestAutoload.autoload?(:Loaded)
    assert_same TestAutoload::Loaded, table_slot(TestAutoload, :Loaded)
  end

  # java_import defines the constant with const_set; the slot was already right. Guard.
  def test_explicit_require_of_java_import
    path = "#{AUTOLOAD_REQUIRED}/java_import.rb"
    Imported.autoload(:ArrayList, path)
    require path
    assert_nil Imported.autoload?(:ArrayList)
    assert_same java.util.ArrayList, table_slot(Imported, :ArrayList)
    assert_same java.util.ArrayList, in_thread { TestAutoload::Imported::ArrayList }
  end

  def test_explicit_require_then_concurrent_references
    path = "#{AUTOLOAD_REQUIRED}/concurrent.rb"
    TestAutoload.autoload(:Concurrent, path)
    require path
    klass = TestAutoload::Concurrent
    features = $LOADED_FEATURES.size
    threads = 8.times.map do
      Thread.new { 1000.times.all? { TestAutoload::Concurrent.equal?(klass) } }
    end
    assert_equal [true] * 8, threads.map(&:value)
    assert_equal features, $LOADED_FEATURES.size
    assert_equal [path], $autoload_required_loads
    assert_same klass, table_slot(TestAutoload, :Concurrent)
  end

  # Zeitwerk's shape: the loader registers one autoload per file, eager loading requires each file.
  def test_eager_loading_after_autoload_registration
    files = Dir["#{AUTOLOAD_REQUIRED}/eager/*.rb"].sort
    files.each { |f| Eager.autoload(File.basename(f, ".rb").capitalize.to_sym, f) }
    features = $LOADED_FEATURES.dup
    files.each { |f| require f }
    assert_equal files, $autoload_required_loads
    %i[Alpha Beta Gamma].each do |name|
      assert_nil Eager.autoload?(name)
      assert_same Eager.const_get(name), table_slot(Eager, name)
    end
    assert_same Eager::Alpha, Eager::Beta.superclass

    # a regular constant now: no feature bookkeeping is consulted, so nothing is loaded twice
    loaded = $LOADED_FEATURES.dup
    $LOADED_FEATURES.replace(features)
    begin
      assert_same Eager::Gamma, in_thread { TestAutoload::Eager::Gamma }
      assert_same Eager::Beta, in_thread { TestAutoload::Eager::Beta }
    ensure
      $LOADED_FEATURES.replace(loaded)
    end
    assert_equal files, $autoload_required_loads
  end

  # One file defines two autoloaded constants (ActiveSupport's autoload_at shape); no direct require.
  def test_sibling_constant_defined_by_the_same_autoload_stores_constant_in_table
    path = "#{AUTOLOAD_REQUIRED}/pair.rb"
    Siblings.autoload(:PairA, path)
    Siblings.autoload(:PairB, path)
    Siblings::PairA
    assert_equal [path], $autoload_required_loads
    assert_nil Siblings.autoload?(:PairB)
    assert_same Siblings::PairA, table_slot(Siblings, :PairA)
    assert_same Siblings::PairB, table_slot(Siblings, :PairB)
    assert_same Siblings::PairB, in_thread { Marshal.load(Marshal.dump(TestAutoload::Siblings::PairB.new)).class }
    assert_equal [path], $autoload_required_loads
  end

  # The autoloaded file requires another autoloaded file, which requires the first one back.
  def test_circular_require_between_autoloaded_files_stores_both_constants
    Circular.autoload(:First, "#{AUTOLOAD_REQUIRED}/circular_first.rb")
    Circular.autoload(:Second, "#{AUTOLOAD_REQUIRED}/circular_second.rb")
    verbose, $VERBOSE = $VERBOSE, nil
    Circular::First
    $VERBOSE = verbose
    assert_nil Circular.autoload?(:Second)
    assert_same Circular::First, table_slot(Circular, :First)
    assert_same Circular::Second, table_slot(Circular, :Second)
  end

end
