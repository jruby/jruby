require 'test/unit'

class TestGH5939 < Test::Unit::TestCase

  class Primitive

    def _value
      @__value ||= 0
    end

    def +(other)
      _value + other
    end

    def ==(other)
      other == _value
    end

    def method_missing(name, *args)
      return _value - args.first if name == :-
      super
    end

  end

  class Klass; end

  def test_op_indy_regression
    threads = []
    30.times do
      threads << Thread.start do
        begin
          i = Klass.new

          def i.version; Primitive.new end

          assert_equal 10, plus_10(i.version)
          assert_false i.version == 5
          assert_true 0 == i.version

          true
        rescue java.lang.Exception => ex
          warn("#{__method__} FAILED WITH: #{ex}")
          raise ex
        end
      end
    end

    threads.each &:join
    threads.each do |thread|
      assert thread.value # should not raise (if thread raised an exception)
    end

  end

  def test_op_fallback_on_mm_npe
    prim = Primitive.new
    assert_equal -1, prim - 1

    assert_raises(NoMethodError) { Primitive.new * 1 }
  end

  def plus_10(version)
    version + 10
  end
  private :plus_10

end
