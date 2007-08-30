require 'test/unit'
require 'date'

class TestDateTime < Test::Unit::TestCase
  
  def setup
    @t = DateTime.now
  end

  def test_t_should_be_an_instance_of_date_time
    assert_kind_of(DateTime, @t)
    assert_instance_of(DateTime, @t, "T should be instance of DateTime")
  end
  
  ##### to_s ######
  def test_s_should_be_an_instance_of_date_time
    # JRUBY-16
    s = @t.to_s
    assert_kind_of(String, s)
    assert_instance_of(String, s, "S should be instance of String")
  end
  
  ##### <, <=, >, >=, <=> ######
  def test_date_time_comparisons_should_work_logically
    early = DateTime.now
    sleep(1)
    late = DateTime.now
    assert(early < late, "Early should be less than late")
    assert(early <= late, "Early should be less than or equal to late")
    assert(late > early, "Late should be greater than early")
    assert(late >= early, "Late should be greater than or equal to early")
    assert(late != early, "Early and late should not be equal")
    assert_equal(-1, early <=> late)
    assert_equal(0, early <=> early)
    assert_equal(1, late <=> early)
  end

  def test_create_various_number_of_args
    t = [2007, 8, 28, 0, 37, 29]
    answers = ["Tue Aug 28 00:37:29 UTC 2007",
               "Tue Aug 28 00:37:00 UTC 2007",
               "Tue Aug 28 00:00:00 UTC 2007",
               "Tue Aug 28 00:00:00 UTC 2007",
               "Wed Aug 01 00:00:00 UTC 2007",
               "Mon Jan 01 00:00:00 UTC 2007",
               "Mon Jan 01 00:00:00 UTC 2007"]

    0.upto(5) do |i|
      assert(answers[i] == Time.gm(*t[0..-(i+1)]).to_s)
    end
  end
end
