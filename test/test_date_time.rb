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

    # Test the fifth beatle of gm/utc
    t = Time.gm(2005,1,1,0,1,0,10)
    class << t
      def seconds_since_midnight
        self.to_i - self.change(:hour => 0).to_i + (self.usec/1.0e+6)
      end

      def change(options)
        ::Time.send(self.utc? ? :utc : :local, 
            options[:year]  || self.year, 
            options[:month] || self.month, 
            options[:mday]  || self.mday, 
            options[:hour]  || self.hour, 
            options[:min]   || (options[:hour] ? 0 : self.min),
            options[:sec]   || ((options[:hour] || options[:min]) ? 0 : self.sec),
            options[:usec]  || ((options[:hour] || options[:min] || options[:sec]) ? 0 : self.usec))
      end
    end
    assert(60.00001, t.seconds_since_midnight)
  end
end
