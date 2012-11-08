
# Tests to (a) show issues/bugs, and (b) test patches for these, for jira.codehaus.org.
# In the following: IntMin = -9223372036854775808, which is (-2)**(64 - 1).
#   #JRUBY-6612: some problems with JRuby seeming to not detect Java Long arithmetic overflows
#     # IntMin * -1 #=> 9223372036854775808;
#     # -1 * IntMin #=> -9223372036854775808;
#     In the second example JRuby was not detecting the integer overflow.
#     The first patch for this fixed the problem in jruby-1.7.0.preview1,
#     but created a much less serious problem:
#     # IntMin * 1 #=> -9223372036854775808 Fixnum;
#     # 1 * IntMin #=> -9223372036854775808 Bignum;
#     This problem is fixed in jruby-1.7.0.preview2.
#   #JRUBY-6777: RubyFixnum.java - two methods assert false, to detect some long integer overflows
#     # IntMin / -1: -9223372036854775808;
#     # IntMin.divmod(-1): [-9223372036854775808, 0];
#     Fixed in jruby-1.7.0.preview2.
#   #JRUBY-6778: Possible long integer overflow bug in Integer#succ in RubyInteger.java
#     # 9223372036854775807.succ #=> -9223372036854775808;
#     Fixed in jruby-1.7.0.preview2.

# VVT.logfile = "path/smalltest-jruby-.txt"
require 'test/unit'

class VVT < Test::Unit::TestCase
  BIT_SIZES = [ 30, 31, 32, 33, 62, 63, 64, 65 ]

  def test_integer_overflows
    BIT_SIZES.each do |nbits|
      checks_for_integer_overflow(nbits)
    end

  end

  def test_integer_iteration_for_overflows
    BIT_SIZES.each do |nbits|
      checks_for_integer_iteration_overflows(nbits)
    end

  end

  def checks_for_integer_overflow(nbits)
    reset_subcounts()
    return  unless nbits >= 6
    nbits_int_min = n_bits_integer_min(nbits)
    nbits_int_max = n_bits_integer_max(nbits)

    # (1) Test for whether integer addition and negation seems OK,
    #     because we need these to be reliable for the remaining tests.
    check_integer_add_overflow(nbits_int_max,  1)
    check_integer_add_overflow(nbits_int_min, -1)
    check_integer_negate_overflow(nbits_int_max)
    check_integer_negate_overflow(nbits_int_min)
    return  if sub_notOK?()  # stop here if any of the above tests are failed

    # (2) Test for non-iteration integer overflows.
    #     The problems for JRuby-1.6.7.2, JRuby-1.7.0.preview1
    #     seem to be fixed by patches used in JRuby-1.7.0.preview2.

    # In the following: IntMin = -9223372036854775808, which is -(2**(64 - 1));
    #                   IntMax =  9223372036854775807, which is   2**(64 - 1) - 1.

    check_integer_succ(nbits_int_max - 1)
    #* next line:[JRUBY-6778]: integer overflow: JRuby-1.6.7.2, JRuby-1.7.0.preview1;
    # 9223372036854775807.succ #=> -9223372036854775808;
    #              IntMax.succ #=> IntMin;
    check_integer_succ(nbits_int_max)      # [JRUBY-6778]
    check_integer_succ(nbits_int_max + 1)

    check_integer_multiply_by_0_or_1_or_minus_1(nbits_int_min, 0)   # OK
    check_integer_multiply_by_0_or_1_or_minus_1(0, nbits_int_min)   # OK
    check_integer_multiply_by_0_or_1_or_minus_1(nbits_int_min, -1)  # OK
    #* next line:[JRUBY-6612]: integer overflow: JRuby-1.6.7.2;
    # -1 * -9223372036854775808 #=> -9223372036854775808;
    # -1 *               IntMin #=> IntMin;
    # patch for this corrected this bug but created new bug just below
    check_integer_multiply_by_0_or_1_or_minus_1(-1, nbits_int_min)  # [JRUBY-6612]
    #* next line: incorrect class of result: JRuby-1.7.0.preview1;
    # patch for [JRUBY-6612] corrected that bug but created this new less serious bug:
    # 1 * -9223372036854775808 #=> -9223372036854775808 Bignum;
    # 1 *               IntMin #=> -9223372036854775808 Bignum; should be Fixnum IntMin
    check_integer_multiply_by_0_or_1_or_minus_1( 1, nbits_int_min)  # OK in preview2
    check_integer_multiply_by_0_or_1_or_minus_1(nbits_int_min, 1)   # OK

    check_integer_divide_by_1_or_minus_1(nbits_int_min,  1)
    check_integer_divmod_by_1_or_minus_1(nbits_int_min,  1)
    #* next line:[JRUBY-6777]: integer overflow: JRuby-1.6.7.2, JRuby-1.7.0.preview1;
    # -9223372036854775808 / -1 #=> -9223372036854775808;
    #               IntMin / -1 #=> IntMin;
    check_integer_divide_by_1_or_minus_1(nbits_int_min, -1)  # [JRUBY-6777]
    #* next line:[JRUBY-6777]: integer overflow: JRuby-1.6.7.2, JRuby-1.7.0.preview1;
    # -9223372036854775808.divmod(-1) #=> [-9223372036854775808, 0];
    #               IntMin.divmod(-1) #=> [IntMin, 0];
    check_integer_divmod_by_1_or_minus_1(nbits_int_min, -1)  # [JRUBY-6777]
  end

  def check_integer_add_overflow(av, bv)
    reset_actual_expected_etc()
    return  unless av.kind_of?(Integer) && bv.kind_of?(Integer)
    @expected = vv = vvs = vvv = nil
    if av == 0 && bv == 0 then  @expected = 0
    elsif av ==  0 then  @expected = bv
    elsif bv ==  0 then  @expected = av
    elsif av ==  1 then  vv = vvs =  1; vvv = bv
    elsif av == -1 then  vv = vvs = -1; vvv = bv
    elsif bv ==  1 then  vv = vvs =  1; vvv = av
    elsif bv == -1 then  vv = vvs = -1; vvv = av
    elsif bv > 0 then    vv = bv; vvs =  1; vvv = av
    elsif bv < 0 then    vv = bv; vvs = -1; vvv = av
    else  return
    end
    unless @expected then
      begin
        @expected = vv + vvv
        qok = (if vvs == 1 then @expected > vvv else @expected < vvv end)
        qok &&= (-vv + @expected) == vvv
        @expected = inzpect(@expected) + "_???"  unless qok
      rescue
        @expected = "???_" + inzpect($!) + "_???"
      end
    end
    @text = "#{inzpect(av)} + #{inzpect(bv)}"
    begin
      @actual = av + bv
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_succ(nv)
    reset_actual_expected_etc()
    @text = "#{inzpect(nv)}.succ"
    begin
      @expected = 1 + nv
      qok = @expected > nv
      qok &&= (-1 + @expected) == nv
      @expected = inzpect(@expected) + "_???"  unless qok
    rescue
      @expected = "???_" + inzpect($!) + "_???"
    end
    begin
      @actual = nv.succ
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_negate_overflow(nv)
    reset_actual_expected_etc()
    return  unless nv.kind_of?(Integer)
    @text = "negate #{inzpect(nv)}"
    begin
      # This gets the expected value of negate by using subtract,
      # which may be a problem if "a subtract b" is implemented as "a plus negate b",
      # or if "negate n" is implemented as "0 subtract n".
      @expected = 0 - nv
      negexp = 0 - @expected
      qok = negexp == nv && negexp.class == nv.class &&
          if    nv > 0 then  @expected < 0 && @expected.class == nv.class
          elsif nv < 0 then  @expected > 0 && (-1 + @expected).class == nv.class
          else  @expected.zero? && nv.zero? &&
              @expected == nv && @expected.class == nv.class
          end
      @expected = inzpect(@expected) + "_???"  unless qok
    rescue
      @expected = "???_" + inzpect($!) + "_???"
    end
    begin
      @actual = -nv
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_multiply_by_0_or_1_or_minus_1(av, bv)
    reset_actual_expected_etc()
    return  unless av.kind_of?(Integer) && bv.kind_of?(Integer)
    @expected = if av == 0 || bv == 0 then  0
                elsif av ==  1 then  bv
                elsif bv ==  1 then  av
                elsif av == -1 then
                  if bv < 0 then -(bv + 1) + 1 else -bv end
                elsif bv == -1 then
                  if bv < 0 then -(av + 1) + 1 else -av end
                else nil
                end
    return  unless @expected
    @text = "#{inzpect(av)} * #{inzpect(bv)}"
    begin
      @actual = av * bv
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_divide_by_1_or_minus_1(av, bv)
    return  unless av.kind_of?(Integer) && bv.kind_of?(Integer)
    reset_actual_expected_etc()
    @expected = if    bv ==  1 then  av
                elsif bv == -1 then  -(av + 1) + 1
                else nil
                end
    return  unless @expected
    @text = "#{inzpect(av)} / #{inzpect(bv)}"
    begin
      @actual = av / bv
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_divmod_by_1_or_minus_1(av, bv)
    return  unless av.kind_of?(Integer) && bv.kind_of?(Integer)
    reset_actual_expected_etc()
    @expected = if    bv ==  1 then  [av, 0]
                elsif bv == -1 then  [-(av + 1) + 1, 0]
                else nil
                end
    return  unless @expected
    @text = "#{inzpect(av)}.divmod(#{inzpect(bv)})"
    begin
      @actual = av.divmod(bv)
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def checks_for_integer_iteration_overflows(nbits)
    # This mainly tests for incorrect detection of integer overflow in integer iterations.
    reset_subcounts()
    return  unless nbits >= 6
    nbits_int_min = n_bits_integer_min(nbits)
    nbits_int_max = n_bits_integer_max(nbits)

    # (1) Test if integer iteration seems OK for some "likely" integer overflows.
    #     The idea is to "assert false, quickly" if there are "likely" integer overflows.

    #* next lines seem OK in: JRuby-1.6.7.2, JRuby-1.7.0.preview1;
      # next line was in [JRUBY-6779] as an example of Range#each working as expected
      check_integer_range_each(nbits_int_max - 1...nbits_int_max)  # [JRUBY-6779] OK in JRuby-1.6, etc
      check_range_step(nbits_int_max - 1...nbits_int_max, 1)
      check_range_step(nbits_int_max...nbits_int_max + 1, 1)

    #* next lines: integer overflow: JRuby-1.6.7.2, JRuby-1.7.0.preview1;

      # next three lines are OK in JRuby-1.7.0.preview2 because of [JRUBY-6778] #succ patch
      check_integer_range_each(nbits_int_max.. nbits_int_max + 1)  # [JRUBY-6779] OK in preview2
      check_integer_range_each(nbits_int_max...nbits_int_max + 1)  # [JRUBY-6779] OK in preview2
      check_integer_range_each(nbits_int_max...nbits_int_max + 1)
      # but the next two lines seem still notOK in JRuby-1.7.0.preview2
      check_integer_range_each(nbits_int_max    ..nbits_int_max)   # [JRUBY-6779]
      check_integer_range_each(nbits_int_max - 1..nbits_int_max)   # [JRUBY-6779]

      check_integer_downto(nbits_int_min, nbits_int_min)
      check_integer_upto(nbits_int_max, nbits_int_max)

      check_numeric_step(nbits_int_max - 1, nbits_int_max - 1, 2)      # [JRUBY-6790]
      check_numeric_step(nbits_int_max, nbits_int_max, 1)              # [JRUBY-6790]
      check_numeric_step(nbits_int_min, nbits_int_min, -1)

      check_range_step(nbits_int_max..nbits_int_max, 1)
      check_range_step(nbits_int_max - 1..nbits_int_max, 2)
      check_range_step(nbits_int_max - 1..nbits_int_max - 1, 2)
      check_range_step(nbits_int_max - 1...nbits_int_max, 2)

    #* next lines test for plausible but incorrect detection of iteration integer overflow
    #* next lines: integer overflow: JRuby-1.6.7.2, JRuby-1.7.0.preview1;
      check_numeric_step(nbits_int_min + 1, nbits_int_max - 1, nbits_int_max - 2)
      check_numeric_step(nbits_int_max - 1, nbits_int_min + 1, nbits_int_min + 3)
      check_range_step(nbits_int_min + 1..nbits_int_max - 1, nbits_int_max - 2)
      check_range_step(nbits_int_min + 1...nbits_int_max, nbits_int_max - 2)

    return  if sub_notOK?()  # stop here if any of the above tests are failed

    # If there aren't any "likely" integer overflows then do many more tests,
    # hoping to detect any "unlikely" integer overflows.

    nn0 = nbits_int_max - 7
    nn2 = nbits_int_max + 7
    nn = nn0 - 1
    while (nn += 1) <= nn2 do
      nnn = nn0 - 1
      while (nnn += 1) <= nn2 do
        check_integer_downto(nn, nnn)
        check_integer_upto(nn, nnn)
        check_integer_range_each(nn..nnn)
        check_integer_range_each(nn...nnn)

        check_numeric_step(nn, nnn, 1)
        check_numeric_step(nn, nnn, 2)

        check_range_step(nn..nnn, 1)
        check_range_step(nn...nnn, 1)
        check_range_step(nn..nnn, 2)
        check_range_step(nn...nnn, 2)
      end
    end
  end

  # individual tests

  def check_integer_downto(fromv, tov)
    @text = inzpect(fromv) + ".downto(" + inzpect(tov) + ")"
    set_numeric_expected_iterations(fromv, tov, -1, false, false)
    @expected = fromv
    @iterations = []
    begin
      @actual = fromv.downto(tov) do |iv|
                  @iterations << iv
                  break  if @iterations.size >= @max_num_iterations
                end
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_upto(fromv, tov)
    @text = inzpect(fromv) + ".upto(" + inzpect(tov) + ")"
    set_numeric_expected_iterations(fromv, tov, 1, false, false)
    @expected = fromv
    @iterations = []
    begin
      @actual = fromv.upto(tov) do |iv|
                  @iterations << iv
                  break  if @iterations.size >= @max_num_iterations
                end
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_integer_range_each(rangev)
    @text = "(" + inzpect(rangev) + ").each"
    set_numeric_expected_iterations(rangev.begin, rangev.end, 1, false, rangev.exclude_end?)
    @expected = rangev
    @iterations = []
    begin
      @actual = rangev.each do |iv|
                  @iterations << iv
                  break  if @iterations.size >= @max_num_iterations
                end
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_numeric_step(fromv, tov, stepv)
    @text = inzpect(fromv) + ".step(" + inzpect(tov) + ", " + inzpect(stepv) + ")"
    efromv = fromv; etov = tov; estepv = stepv
    if efromv.kind_of?(Float) || etov.kind_of?(Float) || estepv.kind_of?(Float) then
      efromv = efromv.to_f; etov = etov.to_f; estepv = estepv.to_f
    end
    set_numeric_expected_iterations(efromv, etov, estepv, false, false)
    @expected = fromv
    if @expected_exception then
      @expected_exception = ArgumentError.exception("step can't be 0")
      @expected_iterations = []
      @expected = nil
    end
    @iterations = []
    begin
      @actual = fromv.step(tov, stepv) do |iv|
                  @iterations << iv
                  break  if @iterations.size >= @max_num_iterations
                end
    rescue
      @exception = $!
    end
    process_test_result()
  end

  def check_range_step(rangev, stepv)
    @text = "(" + inzpect(rangev) + ").step(" + inzpect(stepv) + ")"
    efromv = rangev.begin; etov = rangev.end; estepv = stepv
    if efromv.kind_of?(Float) || etov.kind_of?(Float) || estepv.kind_of?(Float) then
      efromv = efromv.to_f; etov = etov.to_f; estepv = estepv.to_f
    end
    set_numeric_expected_iterations(efromv, etov, estepv, false, rangev.exclude_end?)
    @expected = rangev
    if @expected_exception || stepv < 0 then
      @expected_exception = if stepv < 0 then
                              ArgumentError.exception("step can't be negative")
                            else
                              ArgumentError.exception("step can't be 0")
                            end
      @expected_iterations = []
      @expected = nil
    end
    @iterations = []
    begin
      @actual = rangev.step(stepv) do |iv|
                  @iterations << iv
                  break  if @iterations.size >= @max_num_iterations
                end
    rescue
      @exception = $!
    end
    process_test_result()
  end

  #############################
  # Test support methods follow
  #############################
  def ruby_run_info_const(symv, altv = nil)
    if Module.const_defined?(symv) then
      Module.const_get(symv)
    else
      altv
    end
  end

  DEFAULT_RECUR_MAX_DEPTH = 3

  IS_JRUBY = !! RUBY_PLATFORM.to_s.downcase.index("java")
  def JRuby?()
    IS_JRUBY
  end

  Infinity = 1.0 / 0.0

  # Min and max values of N-bit signed integers are
  # used for tests that integer overflow detection is working.
  # 31-bit signed integers: MRI Ruby Fixnum;
  # 32-bit signed integers: Java int, C long, etc;
  # 63-bit signed integers: MRI Ruby Fixnum;
  # 64-bit signed integers: Java long, JRuby Ruby Fixnum;
  def n_bits_integer_min(nbits)
    return nil  if nbits < 1
    # Calculate -(2**(nbits - 1)) using only addition
    # to avoid relying on powers (or multiply) being correctly implemented.
    # This may be slow but that should not matter for the types of tests that
    # this is intended for. (You can "cache" values if speed is a problem.)
    min_intv = -1  # starting min value, for a 1_bit signed integer
    nbitsi = 0
    while (nbitsi += 1) < nbits do
      prev_min_intv = min_intv
      unless (min_intv += min_intv) < prev_min_intv then
        raise "ERROR overflow calculating -(2**(nbits - 1)) for nbits=#{nbits}"
      end
    end
    return min_intv
  end
  #
  def n_bits_integer_max(nbits)
    (minv = n_bits_integer_min(nbits)) && -(minv + 1)
  end

  def Rational_loaded?()
    return defined?(Rational)
  end

  attr_accessor :exclude_tests_level, :normal_tests_level

  def setup()
    # initialize test result counts and test controls
    @numOK = @numNotOK = @numExceptions = 0
    reset_subcounts()
    reset_actual_expected_etc()
    @logfile = nil
    @exclude_tests_level = nil  # controls excluding some tests
    @normal_tests_level = 8
    @recur_max_depth = DEFAULT_RECUR_MAX_DEPTH  # max depth for #not_equal?
    # next instance variable allows you to limit the number of iterations in a test
    # in case the actual number of iterations is much larger than expected (or infinite)
    @max_num_iterations = nil
    # next instance variable is the allowed margin of the actual number of iterations
    # over the expected number of iterations
    @max_num_iterations_margin = 2
  end

  def reset_actual_expected_etc()
    @actual   = @iterations          = @exception          = nil
    @expected = @expected_iterations = @expected_exception = nil
    @max_num_iterations = nil
  end

  def reset_subcounts()
    @sub_numOK = @sub_numNotOK = @sub_numExceptions = 0
  end

  def sub_notOK?()
    (@sub_numNotOK && @sub_numNotOK != 0) ||
      (@sub_numExceptions && @sub_numExceptions != 0)
  end

  def not_equal?(actualv, expectedv, recur_max_depthv = nil)
    return true  if actualv.class != expectedv.class
    recur_max_depthv ||= @recur_max_depth || DEFAULT_RECUR_MAX_DEPTH
    recur_max_depthv -= 1
    case actualv
    when Exception then
      return actualv.to_s != expectedv.to_s  # focus equality on the exception message
    when Array then
      if recur_max_depthv >= 0 then
        ii2 = if actualv.size < expectedv.size then actualv.size else expectedv.size end
        iii = vvv = nil
        ii = -1
        while (ii += 1) < ii2 do
          if (vvv = not_equal?(actualv[ii], expectedv[ii], recur_max_depthv)) then
            iii = ii
            break
          end
        end
        if iii || actualv.size != expectedv.size then
          vvv = []  unless vvv.kind_of?(Array)
          vvv << (iii || ii2)
          return vvv
        end         
      end
    end
    return ! (actualv == expectedv)  # use normal equality checking
  end

  def inzpect(valuev, recur_max_depthv = nil,
              first_diff_indexes = nil, first_diff_indexv = nil)
    # Similar to "inspect" but has different options for showing arrays.
    # And for Bignum values shows class as well as value: for example: -123456789_big;
    # the reason for this is that it is possible that a numeric value is correct
    # but that the expected value is Fixnum and the actual value is Bignum,
    # and we want the output to be clear about the type of value.
    recur_max_depthv ||= @recur_max_depth || DEFAULT_RECUR_MAX_DEPTH
    recur_max_depthv -= 1
    case valuev
    when Array then
      if recur_max_depthv >= 0 then
        if first_diff_indexes.kind_of?(Array) then
          first_diff_indexv ||= first_diff_indexes.size
          first_diff_indexv -= 1
        else
          first_diff_indexv = nil
        end
        resultv = "["
        if valuev.size > 0 then
          iidiff_indexv = if first_diff_indexes.kind_of?(Array) then
                            first_diff_indexes[first_diff_indexv]
                          else
                            -1
                          end
          iidiff_indexv = -1  if iidiff_indexv >= valuev.size
          ii = 0
          if ii == iidiff_indexv then
            iidiff_indexv = -1
            fdixs = first_diff_indexes
            fdixv = first_diff_indexv
          else
            fdixs = fdixv = nil
          end
          resultv << inzpect(valuev[ii], recur_max_depthv, fdixs, fdixv)
          if valuev.size > 1 then
            prev_ii = ii
            if prev_ii < iidiff_indexv then
              if prev_ii < (ii = iidiff_indexv - 1) then
                resultv << (if prev_ii + 1 == ii then ", " else ",...@#{ii}:" end) <<
                           inzpect(valuev[ii], recur_max_depthv, nil, nil)
                prev_ii = ii
              end
              ii = iidiff_indexv
              iidiff_indexv = -1
              resultv << (if prev_ii + 1 == ii then ", " else ",...@#{ii}:" end) <<
                         inzpect(valuev[ii], recur_max_depthv,
                                 first_diff_indexes, first_diff_indexv)
              prev_ii = ii
            end
            if prev_ii < (ii = valuev.size - 1) then
              resultv << (if prev_ii + 1 == ii then ", " else ",...@#{ii}:" end) <<
                         inzpect(valuev[ii], recur_max_depthv, nil, nil)
            end
          end
        end
        resultv << "]"
        return resultv
      end
    when Range then
      # Note recursive calls in next line!!!
      return inzpect(valuev.begin) <<
             (if valuev.exclude_end? then "..." else ".." end) <<
             inzpect(valuev.end)
    when Exception then
      return valuev.class.name + ":" << valuev.to_s.inspect
    when Bignum then
      return valuev.inspect << "_big"
    when Fixnum, Float, NilClass, TrueClass, FalseClass then
      return valuev.inspect
    else
      unless Rational_loaded? && valuev.class == Rational then
        return valuev.class.name + ":v:" << valuev.inspect
      end
    end
    valuev.inspect
  end

  def process_test_result()
    # For readability in making tests, this method relies on instance variables being set.
    # It calls "process_test_result_private" which (mostly) does not use instance variables.
    process_test_result_private(@actual, @expected,
                                @iterations, @expected_iterations,
                                @exception, @expected_exception,
                                @text)
  end

  def process_test_result_private(actual_resultv, expected_resultv,
                                  actual_iterationsv, expected_iterationsv,
                                  actual_exceptionv, expected_exceptionv,
                                  test_textv = nil)
    if expected_resultv.kind_of?(Exception) && expected_exceptionv.nil? then
      expected_exceptionv = expected_resultv
      expected_resultv = nil
    end

    msgv = test_textv << " #=>"
    msgv << " != " << inzpect(expected_resultv, nil, not_equal?(actual_iterationsv, expected_iterationsv), nil)
    assert_equal(actual_resultv, expected_resultv, msgv)

    msgv = test_textv << " #=>"
    msgv << " != " << inzpect(expected_iterationsv, nil, not_equal?(actual_resultv, expected_resultv), nil)
    assert_equal(actual_iterationsv, expected_iterationsv, msgv)

    msgv = test_textv << " #=>"
    msgv << " != " << inzpect(expected_exceptionv, nil, not_equal?(actual_exceptionv, expected_exceptionv), nil)
    assert_equal(actual_exceptionv, expected_exceptionv, msgv)
  end

  def set_numeric_expected_iterations(fromv, tov, stepv, exclude_fromv, exclude_tov)
    # set up expected numeric iterated values
    reset_actual_expected_etc()
    if stepv == 0 then
      @expected_exception = true
      return
    end
    @expected_iterations = []  # store expected iterated values in an array
    # next line sets the appropriate check for the end of the iteration
    symv = if stepv > 0 then
             if exclude_tov then :< else :<= end
           else
             if exclude_tov then :> else :>= end
           end
    itvalue = fromv
    num_steps = 0  # count the steps from fromv: needed for Float (etc) iterations
    if exclude_fromv then
      # Depending on the type of iteration, this might need to be changed
      # if stepv is a very small Float or BigDecimal, etc.
      itvalue += stepv
      num_steps += 1
    end
    while itvalue.__send__(symv, tov) do
      @expected_iterations << itvalue
      num_steps += 1  # needed for Float (etc) iterations
      if itvalue.kind_of?(Float) ||
          (defined?(BigDecimal) && itvalue.kind_of?(BigDecimal)) then
        itvalue = fromv + stepv * num_steps
      else
        itvalue += stepv
      end
    end
    # next line anticipates unexpected infinite or very large loops in tests of iterators
    @max_num_iterations = @expected_iterations.size + (@max_num_iterations_margin || 2)
    return
  end

end
