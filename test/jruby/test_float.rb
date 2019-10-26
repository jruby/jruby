require 'test/unit'

class TestGH5939 < Test::Unit::TestCase

  class Primitive

    def _value
      @__value ||= 0.0
    end

    def +(other)
      _value + other
    end

    def ==(other)
      other == _value
    end

    def to_f
      _value.to_f
    end

  end

  def test_op_indy_regression
    threads = []
    30.times do
      threads << Thread.start do
        begin
          i = Object.new

          def i.version; @version ||= Primitive.new end

          assert_equal 11.0, i.version + 11.0

          assert_false i.version == 5.0
          assert_true 0.0 == i.version

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

end
