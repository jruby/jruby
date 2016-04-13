# frozen_string_literal: false
require 'test/unit'
require 'forwardable'

class TestForwardable < Test::Unit::TestCase
  RECEIVER = BasicObject.new
  RETURNED1 = BasicObject.new
  RETURNED2 = BasicObject.new

  class << RECEIVER
    def delegated1
      RETURNED1
    end

    def delegated2
      RETURNED2
    end
  end

  def test_def_instance_delegator
    %i[def_delegator def_instance_delegator].each do |m|
      cls = forwardable_class do
        __send__ m, :@receiver, :delegated1
      end

      assert_same RETURNED1, cls.new.delegated1
    end
  end

  def test_def_instance_delegators
    %i[def_delegators def_instance_delegators].each do |m|
      cls = forwardable_class do
        __send__ m, :@receiver, :delegated1, :delegated2
      end

      assert_same RETURNED1, cls.new.delegated1
      assert_same RETURNED2, cls.new.delegated2
    end
  end

  def test_instance_delegate
    %i[delegate instance_delegate].each do |m|
      cls = forwardable_class do
        __send__ m, delegated1: :@receiver, delegated2: :@receiver
      end

      assert_same RETURNED1, cls.new.delegated1
      assert_same RETURNED2, cls.new.delegated2

      cls = forwardable_class do
        __send__ m, %i[delegated1 delegated2] => :@receiver
      end

      assert_same RETURNED1, cls.new.delegated1
      assert_same RETURNED2, cls.new.delegated2
    end
  end

  def test_def_single_delegator
    %i[def_delegator def_single_delegator].each do |m|
      cls = single_forwardable_class do
        __send__ m, :@receiver, :delegated1
      end

      assert_same RETURNED1, cls.delegated1
    end
  end

  def test_def_single_delegators
    %i[def_delegators def_single_delegators].each do |m|
      cls = single_forwardable_class do
        __send__ m, :@receiver, :delegated1, :delegated2
      end

      assert_same RETURNED1, cls.delegated1
      assert_same RETURNED2, cls.delegated2
    end
  end

  def test_single_delegate
    %i[delegate single_delegate].each do |m|
      cls = single_forwardable_class do
        __send__ m, delegated1: :@receiver, delegated2: :@receiver
      end

      assert_same RETURNED1, cls.delegated1
      assert_same RETURNED2, cls.delegated2

      cls = single_forwardable_class do
        __send__ m, %i[delegated1 delegated2] => :@receiver
      end

      assert_same RETURNED1, cls.delegated1
      assert_same RETURNED2, cls.delegated2
    end
  end

  class Foo
    extend Forwardable

    def_delegator :bar, :baz

    class Exception
    end
  end

  def test_backtrace_adjustment
    e = assert_raise(NameError) {
      Foo.new.baz
    }
    assert_not_match(/\/forwardable\.rb/, e.backtrace[0])
  end

  class Foo2 < BasicObject
    extend ::Forwardable

    def_delegator :bar, :baz
  end

  def test_basicobject_subclass
    bug11616 = '[ruby-core:71176] [Bug #11616]'
    assert_raise_with_message(NameError, /`bar'/, bug11616) {
      Foo2.new.baz
    }
  end

  private

  def forwardable_class(&block)
    Class.new do
      extend Forwardable

      def initialize
        @receiver = RECEIVER
      end

      class_exec(&block)
    end
  end

  def single_forwardable_class(&block)
    Class.new do
      extend SingleForwardable

      @receiver = RECEIVER

      class_exec(&block)
    end
  end
end
