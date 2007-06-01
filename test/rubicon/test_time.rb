require 'test/unit'

#
# NOTICE: These tests assume that your local time zone is *not* GMT.
#

class T # ZenTest SKIP
  attr :orig
  attr :amt
  attr :result
  def initialize(a1, anAmt, a2)
    @orig = a1
    @amt = anAmt
    @result = a2
  end
  def to_s
    @orig.join("-")
  end
end

class TestTime < Test::Unit::TestCase

  ONEDAYSEC = 60 * 60 * 24

  #
  # Test month name to month number
  #
  @@months = { 
    'Jan' => 1,
    'Feb' => 2,
    'Mar' => 3,
    'Apr' => 4,
    'May' => 5,
    'Jun' => 6,
    'Jul' => 7,
    'Aug' => 8,
    'Sep' => 9,
    'Oct' => 10,
    'Nov' => 11,
    'Dec' => 12
  }

  #
  # A random selection of interesting dates
  #
  @@dates = [ 
    #                   Source  +   amt         ==   dest
    T.new([1999, 12, 31, 23,59,59], 1,               [2000,  1,  1,  0,0,0]),
    T.new([2036, 12, 31, 23,59,59], 1,               [2037,  1,  1,  0,0,0]),
    T.new([2000,  2, 28, 23,59,59], 1,               [2000,  2, 29, 0,0,0]),
    T.new([1970,  2, 1,   0, 0, 0], ONEDAYSEC,       [1970,  2,  2,  0,0,0]),
    T.new([2000,  7, 1,   0, 0, 0], 32 * ONEDAYSEC,  [2000,  8,  2,  0,0,0]),
    T.new([2000,  1, 1,   0, 0, 0], 366 * ONEDAYSEC, [2001,  1,  1,  0,0,0]),
    T.new([2001,  1, 1,   0, 0, 0], 365 * ONEDAYSEC, [2002,  1,  1,  0,0,0]),

    T.new([2000,  1, 1,   0, 0, 0], 0,               [2000,  1,  1,  0,0,0]),
    T.new([2000,  2, 1,   0, 0, 0], 0,               [2000,  2,  1,  0,0,0]),
    T.new([2000,  3, 1,   0, 0, 0], 0,               [2000,  3,  1,  0,0,0]),
    T.new([2000,  4, 1,   0, 0, 0], 0,               [2000,  4,  1,  0,0,0]),
    T.new([2000,  5, 1,   0, 0, 0], 0,               [2000,  5,  1,  0,0,0]),
    T.new([2000,  6, 1,   0, 0, 0], 0,               [2000,  6,  1,  0,0,0]),
    T.new([2000,  7, 1,   0, 0, 0], 0,               [2000,  7,  1,  0,0,0]),
    T.new([2000,  8, 1,   0, 0, 0], 0,               [2000,  8,  1,  0,0,0]),
    T.new([2000,  9, 1,   0, 0, 0], 0,               [2000,  9,  1,  0,0,0]),
    T.new([2000, 10, 1,   0, 0, 0], 0,               [2000, 10,  1,  0,0,0]),
    T.new([2000, 11, 1,   0, 0, 0], 0,               [2000, 11,  1,  0,0,0]),
    T.new([2000, 12, 1,   0, 0, 0], 0,               [2000, 12,  1,  0,0,0]), 

    T.new([2001,  1, 1,   0, 0, 0], 0,               [2001,  1,  1,  0,0,0]),
    T.new([2001,  2, 1,   0, 0, 0], 0,               [2001,  2,  1,  0,0,0]),
    T.new([2001,  3, 1,   0, 0, 0], 0,               [2001,  3,  1,  0,0,0]),
    T.new([2001,  4, 1,   0, 0, 0], 0,               [2001,  4,  1,  0,0,0]),
    T.new([2001,  5, 1,   0, 0, 0], 0,               [2001,  5,  1,  0,0,0]),
    T.new([2001,  6, 1,   0, 0, 0], 0,               [2001,  6,  1,  0,0,0]),
    T.new([2001,  7, 1,   0, 0, 0], 0,               [2001,  7,  1,  0,0,0]),
    T.new([2001,  8, 1,   0, 0, 0], 0,               [2001,  8,  1,  0,0,0]),
    T.new([2001,  9, 1,   0, 0, 0], 0,               [2001,  9,  1,  0,0,0]),
    T.new([2001, 10, 1,   0, 0, 0], 0,               [2001, 10,  1,  0,0,0]),
    T.new([2001, 11, 1,   0, 0, 0], 0,               [2001, 11,  1,  0,0,0]),
    T.new([2001, 12, 1,   0, 0, 0], 0,               [2001, 12,  1,  0,0,0]),
  ]

  def setup
    @orig_zone = ENV['TZ']
    ENV['TZ'] = 'PST8PDT'
    @utc = Time.utc(2001, 2, 3, 4, 5, 6)
    @loc = Time.local(2001, 2, 3, 4, 5, 6)
    @zone = @loc.zone
  end

  def teardown
    ENV['TZ'] = @orig_zone
  end

  ##
  # Check a particular date component -- m is the method (day, month, etc)
  # and i is the index in the date specifications above.

  def util_check_component(m, i)
    @@dates.each do |x|
      assert_equal(x.orig[i],   Time.local(*x.orig).send(m))
      assert_equal(x.result[i], Time.local(*x.result).send(m))
      assert_equal(x.orig[i],   Time.gm(*x.orig).send(m))
      assert_equal(x.result[i], Time.gm(*x.result).send(m))
    end
  end

  def util_class_now(method)
    min = 0.1
    max = min * 3.0 # some ruby impls will be SLOOOW
    t1 = Time.send(method)
    sleep min
    t2 = Time.send(method)
    delta = t2.to_f - t1.to_f
    assert(delta >= min, "time difference must be at least #{min}")
    assert(max >= delta, "time difference should not be more than #{max}")
  end

  def util_os_specific_epoch
    "Thu Jan  1 00:00:00 1970"
  end

  ##
  # If this test is failing, you've got big problems.  Start with Time::at,
  # Time::utc and Time::local before looking at bugs in any of your other
  # code.

  def test_00sanity # ZenTest SKIP
    assert_equal(Time.at(981173106), Time.utc(2001, 2, 3, 4, 5, 6),
                 "If this test fails, don't bother debugging anything else.")
    assert_equal(Time.at(981201906), Time.local(2001, 2, 3, 4, 5, 6),
                 "If this test fails, don't bother debugging anything else.")
  end

  # Class methods:

  def test_class__load
    # TODO: raise NotImplementedError, 'Need to write test_class__load'
  end

  def test_class_at
    sec = @loc.to_i
    assert_equal(0, Time.at(0).to_i)
    assert_equal(@loc, Time.at(@loc))
    assert_in_delta(Time.at(sec,1_000_000).to_f, Time.at(sec).to_f, 1.0)

    # no arguments ==> error
    assert_raise(ArgumentError) do
      Time.at
    end

    # one integer argument ==> seconds
    t = Time.at(1_234_567)
    assert_equal(1_234_567, t.tv_sec)
    assert_equal(        0, t.tv_usec)

    # two integer arguments ==> seconds & microseconds
    t = Time.at(1_234_567, 888_999)
    assert_equal(1_234_567, t.tv_sec)
    assert_equal(  888_999, t.tv_usec)

    # float argument ==> second & rounded microseconds
    t = Time.at(1_234_567.5)
    assert_equal(1_234_567, t.tv_sec)
    assert_equal(  500_000, t.tv_usec)

    # float + integer arguments ==> rounded seconds & microseconds
    t = Time.at(1_234_567.5, 300_000)
    assert_equal(1_234_567, t.tv_sec)
    assert_equal(  300_000, t.tv_usec)

    # Time argument
    t1 = Time.at(1_234_567,  888_999)
    t2 = Time.at(t1)
    assert_equal(1_234_567, t2.tv_sec)
    assert_equal(  888_999, t2.tv_usec)
  end

  def test_class_at_utc
    utc1 = @utc
    utc2 = Time.at(@utc)
    assert(utc1.utc?)
    assert(utc2.utc?)
    assert_equal(utc1.to_i, utc2.to_i)
  end

  def test_class_gm
    assert_raise(ArgumentError) { Time.gm }
    assert_not_equal(Time.gm(2000), Time.local(2000))
    assert_equal(Time.gm(2000), Time.gm(2000,1,1,0,0,0))
    assert_equal(Time.gm(2000,nil,nil,nil,nil,nil), Time.gm(2000,1,1,0,0,0))
    assert_raise(ArgumentError) { Time.gm(2000,0) }
    assert_raise(ArgumentError) { Time.gm(2000,13) }
    assert_raise(ArgumentError) { Time.gm(2000,1,1,24) }
    Time.gm(2000,1,1,23)
    @@months.each do |month, num| 
      assert_equal(Time.gm(2000,month), Time.gm(2000,num,1,0,0,0))
      assert_equal(Time.gm(1970,month), Time.gm(1970,num,1,0,0,0))
      assert_equal(Time.gm(2037,month), Time.gm(2037,num,1,0,0,0))
    end
    t = Time.gm(2000,1,1)
    a = t.to_a
    assert_equal(Time.gm(*a),t)
  end

  def test_class_local
    assert_raise(ArgumentError) { Time.local }
    assert_not_equal(Time.gm(2000), Time.local(2000))
    assert_equal(Time.local(2000), Time.local(2000,1,1,0,0,0))
    assert_equal(Time.local(2000,nil,nil,nil,nil,nil), Time.local(2000,1,1,0,0,0))
    assert_raise(ArgumentError) { Time.local(2000,0) }
    assert_raise(ArgumentError) { Time.local(2000,13) }
    assert_raise(ArgumentError) { Time.local(2000,1,1,24) }
    Time.local(2000,1,1,23)
    @@months.each do |month, num| 
      assert_equal(Time.local(2000,month), Time.local(2000,num,1,0,0,0))
      assert_equal(Time.local(1971,month), Time.local(1971,num,1,0,0,0))
      assert_equal(Time.local(2037,month), Time.local(2037,num,1,0,0,0))
    end
    t = Time.local(2000,1,1)
    a = t.to_a
    assert_equal(Time.local(*a),t)
  end

  def test_class_mktime
    #
    # Test insufficient arguments
    #
    assert_raise(ArgumentError) { Time.mktime }
    assert_not_equal(Time.gm(2000), Time.mktime(2000))
    assert_equal(Time.mktime(2000), Time.mktime(2000,1,1,0,0,0))
    assert_equal(Time.mktime(2000,nil,nil,nil,nil,nil), Time.mktime(2000,1,1,0,0,0))
    assert_raise(ArgumentError) { Time.mktime(2000,0) }
    assert_raise(ArgumentError) { Time.mktime(2000,13) }
    assert_raise(ArgumentError) { Time.mktime(2000,1,1,24) }
    Time.mktime(2000,1,1,23)

    #
    # Make sure spelled-out month names work
    #
    @@months.each do |month, num| 
      assert_equal(Time.mktime(2000,month), Time.mktime(2000,num,1,0,0,0))
      assert_equal(Time.mktime(1971,month), Time.mktime(1971,num,1,0,0,0))
      assert_equal(Time.mktime(2037,month), Time.mktime(2037,num,1,0,0,0))
    end
    t = Time.mktime(2000,1,1)
    a = t.to_a
    assert_equal(Time.mktime(*a),t)
  end

  def test_class_now
    util_class_now(:now) # Time.now
  end

  def test_class_times
    assert_instance_of(Struct::Tms, Process.times)
  end

  def test_class_utc
    test_class_gm # TODO: refactor to ensure they really are synonyms
  end

  # Instance Methods:

  def test__dump
    # TODO: raise NotImplementedError, 'Need to write test__dump'
  end

  def test_asctime
    expected = util_os_specific_epoch
    assert_equal(expected, Time.at(0).gmtime.asctime)
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = @loc.dup
        a.taint  if taint
        a.freeze if frozen
        b = a.clone

        assert_equal(a, b)
        assert_not_equal(a.__id__, b.__id__)
        assert_equal(a.frozen?, b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end
  end

  def test_ctime
    expected = util_os_specific_epoch
    assert_equal(expected, Time.at(0).gmtime.ctime)
  end

  def test_day
    util_check_component(:day, 2)
  end

  def test_dst_eh
    test_isdst # TODO: refactor to test that they really are the same
  end

  def test_eql_eh
    t1 = @loc
    t2 = Time.at(t1)
    t3 = t1 + 2e-6
    t4 = t1 + 1
    assert(t1.eql?(t1))
    assert(t1.eql?(t2))
    assert(!t1.eql?(t3))
    assert(!t1.eql?(t4))
    assert(t1.eql?(t1.getutc))
  end

  def test_getgm
    # TODO: this only tests local -> gm
    t1 = @loc
    loc = Time.at(t1)
    assert(!t1.gmt?)
    t2 = t1.getgm
    assert(!t1.gmt?)
    assert(t2.gmt?)
    assert_equal(t1, loc)
    assert_equal(t1.asctime, loc.asctime)
    assert_not_equal(t2.asctime, loc.asctime)
    assert_not_equal(t1.asctime, t2.asctime)
    assert_equal(t1, t2)
  end

  def test_getlocal
    # TODO: this only tests gm -> local
    t1 = @utc
    utc = Time.at(t1)
    assert(t1.gmt?)
    t2 = t1.getlocal
    assert(t1.gmt?)
    assert(!t2.gmt?)
    assert_equal(t1, utc)
    assert_equal(t1.asctime, utc.asctime)
    assert_not_equal(t2.asctime, utc.asctime)
    assert_not_equal(t1.asctime, t2.asctime)
    assert_equal(t1, t2)
  end

  def test_getutc
    test_getgm # REFACTOR to test both calls
  end

  def test_gmt_eh
    assert(!@loc.gmt?)
    assert(@utc.gmt?)
    assert(!Time.local(2000).gmt?)
    assert(Time.gm(2000).gmt?)
  end

  def test_gmt_offset
    test_utc_offset # REFACTOR to test both methods
  end

  def test_gmtime
    # TODO: this only tests local -> gm
    t = @loc
    loc = Time.at(t)
    assert(!t.gmt?)
    t.gmtime
    assert(t.gmt?)
    assert_not_equal(t.asctime, loc.asctime)
  end

  def test_gmtoff
    test_utc_offset # REFACTOR to test both methods
  end

  def test_hash
    t1 = @utc
    t2 = Time.at(t1)
    t3 = @utc + 1
    assert_equal(t1.hash, t2.hash)
    assert_not_equal(t1.hash, t3.hash)
  end

  def test_hour
    util_check_component(:hour, 3)
  end

  def test_initialize
    util_class_now(:new) # Time.new
  end

  def test_inspect
    assert_equal("Sat Feb 03 04:05:06 UTC 2001", @utc.inspect)
    assert_equal("Sat Feb 03 04:05:06 -0800 2001", @loc.inspect)
  end

  def test_isdst
    # This code is problematic: how do I find out the exact
    # date and time of the dst switch for all the possible
    # timezones in which this code runs? For now, I'll just check
    # midvalues, and add boundary checks for the US. I know this won't 
    # work in some parts of the US, even, so I'm looking for
    # better ideas

    # Are we in the US?
    if ["EST", "EDT",
        "CST", "CDT",
        "MST", "MDT",
        "PST", "PDT"].include? @zone

      dtest = [ 
        [false, 2000, 1, 1],
        [true,  2000, 7, 1],
        [true,  2000, 4, 2, 4],
        [false, 2000, 10, 29, 4],
        [false, 2000, 4,2,1,59],   # Spring forward
        [true,  2000, 4,2,3,0],
        [true,  2000, 10,29,0,59], # Fall back
        [false, 2000, 10,29,2,0]
      ]

      dtest.each do |x|
        result = x.shift
        assert_equal(result, Time.local(*x).isdst,
                     "\nExpected Time.local(#{x.join(',')}).isdst == #{result}")
      end
    else
      skipping("Don't know how to do timezones");
    end
  end

  def test_localtime
    # TODO: this only tests gm -> local
    t = @utc
    utc = Time.at(t)
    assert(t.gmt?)
    t.localtime
    assert(!t.gmt?)
    assert_not_equal(t.asctime, utc.asctime)
  end

  def test_mday
    util_check_component(:mday, 2)
  end

  def test_min
    util_check_component(:min, 4)
  end

  def test_minus # '-'
    @@dates.each do |x|
      # Check subtracting an amount in seconds
      assert_equal(Time.local(*x.result) - x.amt, Time.local(*x.orig))
      assert_equal(Time.gm(*x.result) - x.amt, Time.gm(*x.orig))
      # Check subtracting two times
      assert_equal(Time.local(*x.result) - Time.local(*x.orig), x.amt)
      assert_equal(Time.gm(*x.result) - Time.gm(*x.orig), x.amt)
    end

    # integer argument
    t1 = Time.at(1_234_567, 500_000)
    t2 = t1 - 567
    assert_equal( 1_234_000, t2.tv_sec)
    assert_equal(   500_000, t2.tv_usec)

    # float argument with fractional part
    t1 = Time.at(1_234_567, 500_000)
    t2 = t1 - 566.75
    assert_equal( 1_234_000, t2.tv_sec)
    assert_equal(   750_000, t2.tv_usec)

    # Time argument
    t1 = Time.at(1_234_000, 750_000)
    t2 = Time.at(1_234_567, 500_000)
    diff = t2 - t1
    assert_equal( 566.75, diff)
  end

  def test_mon
    util_check_component(:mon, 1)
  end

  def test_month
    util_check_component(:month, 1)
  end

  def test_plus # '+'
    @@dates.each do |x|
      assert_equal(Time.local(*x.orig) + x.amt, Time.local(*x.result))
      assert_equal(Time.gm(*x.orig) + x.amt, Time.gm(*x.result))
    end

    # integer argument
    t1 = Time.at(1_234_567, 500_000)
    t2 = t1 + 433
    assert_equal( 1_235_000, t2.tv_sec)
    assert_equal(   500_000, t2.tv_usec)

    # float argument with fractional part
    t1 = Time.at(1_234_567, 500_000)
    t2 = t1 + 433.25
    assert_equal( 1_235_000, t2.tv_sec)
    assert_equal(   750_000, t2.tv_usec)
  end

  def test_sec
    util_check_component(:sec, 5)
  end

  def test_spaceship # '<=>'
    @@dates.each do |x|
      if (x.amt != 0)
        assert_equal(1, Time.local(*x.result) <=> Time.local(*x.orig),
                     "#{x.result} should be > #{x.orig}")

        assert_equal(-1, Time.local(*x.orig) <=> Time.local(*x.result))
        assert_equal(0, Time.local(*x.orig) <=> Time.local(*x.orig))
        assert_equal(0, Time.local(*x.result) <=> Time.local(*x.result))
        
        assert_equal(1,Time.gm(*x.result) <=> Time.gm(*x.orig))
        assert_equal(-1,Time.gm(*x.orig) <=> Time.gm(*x.result))
        assert_equal(0,Time.gm(*x.orig) <=> Time.gm(*x.orig))
        assert_equal(0,Time.gm(*x.result) <=> Time.gm(*x.result))
      end
    end

    # microsecond diffs
    assert_equal( 1, Time.at(10_000, 500_000) <=> Time.at(10_000, 499_999))
    assert_equal( 0, Time.at(10_000, 500_000) <=> Time.at(10_000, 500_000))
    assert_equal(-1, Time.at(10_000, 500_000) <=> Time.at(10_000, 500_001))

    # second diff & microsecond diffs
    assert_equal(-1, Time.at(10_000, 500_000) <=> Time.at(10_001, 499_999))
    assert_equal(-1, Time.at(10_000, 500_000) <=> Time.at(10_001, 500_000))
    assert_equal(-1, Time.at(10_000, 500_000) <=> Time.at(10_001, 500_001))

    # non-Time object gives nil
    assert_nil(Time.at(10_000) <=> Object.new)
  end

  def test_strftime
    # Sat Jan  1 14:58:42 2000
    t = Time.local(2000,1,1,14,58,42)

    stest = {
       '%a' => 'Sat',
       '%A' => 'Saturday',
       '%b' => 'Jan',
       '%B' => 'January',
       #'%c',  The preferred local date and time representation,
       '%d' => '01',
       '%H' => '14',
       '%I' => '02',
       '%j' => '001',
       '%m' => '01',
       '%M' => '58',
       '%p' => 'PM',
       '%S' => '42',
       '%U' => '00',
       '%W' => '00',
       '%w' => '6',
       #'%x',  Preferred representation for the date alone, no time\\
       #'%X',  Preferred representation for the time alone, no date\\
       '%y' =>  '00',
       '%Y' =>  '2000',
       #'%Z',  Time zone name\\
       '%%' =>  '%',
      }

    stest.each {|flag,val|
      assert_equal("Got "+val,t.strftime("Got " + flag))
    }

  end

  def test_succ
    t1 = @loc
    t2 = t1 + 1
    t3 = t1.succ
    assert_equal(t2, t3)
  end

  def test_to_a
    t = @loc
    a = t.to_a
    assert_equal(t.sec,  a[0])
    assert_equal(t.min,  a[1])
    assert_equal(t.hour, a[2])
    assert_equal(t.day,  a[3])
    assert_equal(t.month,a[4])
    assert_equal(t.year, a[5])
    assert_equal(t.wday, a[6])
    assert_equal(t.yday, a[7])
    assert_equal(t.isdst,a[8])
    assert_equal(t.zone, a[9])
  end

  def test_to_f
    t = Time.at(10000,1066)
    assert_in_delta(10000.001066, t.to_f, 1e-7)
  end

  def test_to_i
    t = Time.at(0)
    assert_equal(0, t.to_i)
    t = Time.at(10000)
    assert_equal(10000, t.to_i)
  end

  def test_to_s
    assert_equal("Sat Feb 03 04:05:06 UTC 2001", @utc.to_s)
    assert_equal("Sat Feb 03 04:05:06 -0800 2001", @loc.to_s)
  end

  def test_tv_sec
    t = Time.at(0)
    assert_equal(0,t.tv_sec)
    t = Time.at(10000)
    assert_equal(10000,t.tv_sec)
  end

  def util_usec(s, u, method)
    t = Time.at(s,u)
    assert_equal(u,t.send(method))
  end

  def test_tv_usec
    util_usec(10000, 1066, :tv_usec)
    util_usec(10000, 0, :tv_usec)
  end

  def test_usec
    util_usec(10000, 1066, :usec)
    util_usec(10000, 0, :usec)
  end

  def test_utc
    test_gmtime # REFACTOR to test both methods
  end

  def test_utc_eh
    test_gmt_eh # REFACTOR to test both methods
  end

  def test_utc_offset
    # TODO: figure out the year, month, & day edgecase setups
    off = @utc - @loc
    assert_equal(0, @utc.utc_offset)
    assert_equal(off, @loc.utc_offset)
  end

  def test_wday
    t = Time.local(2001, 4, 1)

    7.times { |i|
      assert_equal(i,t.wday)
      t += ONEDAYSEC
    }
  end

  def test_yday
    # non-leap 1/1, 2/28,       3/1, 12/31
    #     leap 1/1, 2/28, 2/29, 3/1, 12/31
    # leap century (2000)
    # want to do a non-leap century, but they are out of range.
    # any others?

    # non-leap year:
    assert_equal(  1, Time.local(1999,  1,  1).yday)
    assert_equal( 59, Time.local(1999,  2, 28).yday)
    assert_equal( 60, Time.local(1999,  3,  1).yday)
    assert_equal(365, Time.local(1999, 12, 31).yday)

    # leap century:
    assert_equal(  1, Time.local(2000,  1,  1).yday)
    assert_equal( 59, Time.local(2000,  2, 28).yday)
    assert_equal( 60, Time.local(2000,  2, 29).yday)
    assert_equal( 61, Time.local(2000,  3,  1).yday)
    assert_equal(366, Time.local(2000, 12, 31).yday)

    # leap year:
    assert_equal(  1, Time.local(2004,  1,  1).yday)
    assert_equal( 59, Time.local(2004,  2, 28).yday)
    assert_equal( 60, Time.local(2004,  2, 29).yday)
    assert_equal( 61, Time.local(2004,  3,  1).yday)
    assert_equal(366, Time.local(2004, 12, 31).yday)
  end

  def test_year
    util_check_component(:year, 0)
  end

  def test_zone
    gmt = "UTC"
    t = @utc
    assert_equal(gmt, t.zone)
    t = @loc
    assert_not_equal(gmt, t.zone)
  end

end