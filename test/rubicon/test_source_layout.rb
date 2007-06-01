require 'test/unit'

class TestSourceLayout < Test::Unit::TestCase

  # ------------------------------------------------------------

  def test_semicolon_separated_expressions
    assert_equal(2, eval("1; 2"))
    assert_equal('1; 2', "1; 2")
    assert_equal(99, eval("def fred() 99; end; fred"))
  end

  

  # ------------------------------------------------------------

  def lcbb_test(*args)
    return args.length
  end

  def test_line_continuation_by_backslash
    assert_equal(3, 1 \
                    + \
                    2)
    assert_equal("hello", "h\
e\
l\
l\
o")

    assert_equal("h e l l o", "h\
 e\
 l\
 l\
 o")

    assert_equal("h\ne\nl\nl\no", "h
e
l
l
o")

    a = lcbb_test \
"arg?"
    assert_equal(1, a)          # would be '0' if arg not passed

  end

  # ------------------------------------------------------------

  def test_line_continuation_in_mid_expression
    assert_equal(3, 1 +
                 2)

    assert_equal(3, 1 + # spurious comment
                 2)

    assert_equal(3, (1 +
                     2))

    assert_equal(2, lcbb_test(1,
                              2))

    assert_equal(2, lcbb_test(
                              1,
                              2
                              ))
  end

  # ------------------------------------------------------------

  def test_comments
    assert_equal 4, 1 + # 2
                    3

    # check that comment isn't continued by '\'
    assert_equal 4, 1 + # 2\
                    3
  end

  # ------------------------------------------------------------

  SAMPLE1 = <<'SAMPLE1'
a\
=begin
skip this
=end
= 99
puts(a + 1)
SAMPLE1

  SAMPLE2 = <<'SAMPLE2'
a\
=begin label
=begin
skip this
=end
= 99
puts(a + 1)
SAMPLE2

  SAMPLE3 = <<'SAMPLE3'
 =begin
skip this
=end
SAMPLE3

  def rubyRun(str)
    result = nil
    code = nil

    File.open("_prog.rb", "w") { |f| f.puts str }

    begin
      result = nil
      IO.popen("#$interpreter _prog.rb 2>_err_tmp") do |p|
	result = p.read
      end
      code   = $?
    ensure
      File.delete("_prog.rb")
      File.delete("_err_tmp")
    end
    [ code, result ]
  end

  def test_equals_begin
    assert_equal([0, "100\n"], rubyRun(SAMPLE1))
    assert_equal([0, "100\n"], rubyRun(SAMPLE2))
    assert(rubyRun(SAMPLE3)[0] != 0)
  end

  # ------------------------------------------------------------

  SAMPLE4 = <<-SAMPLE4
  END   { puts 1 }
  BEGIN { puts 2 }
  END   { puts 3 }
  BEGIN { puts 4 }
  SAMPLE4

  SAMPLE5 = <<-SAMPLE5
  END   { puts 5 }
  require "_prog1.rb"
  BEGIN { puts 8 }
  SAMPLE5

  
  def test_BEGIN_END
    assert_equal([0, "2\n4\n3\n1\n"], rubyRun(SAMPLE4))

    # confirm that require brings things in at runtime
    File.open("_prog1.rb", "w") do |f|
      f.puts "BEGIN { puts 6 }"
      f.puts "END { puts 7 }"
    end
    begin
      assert_equal([0, "8\n6\n7\n5\n"], rubyRun(SAMPLE5))
    ensure
      File.delete("_prog1.rb")
    end
  end

  # ------------------------------------------------------------

  SAMPLE6 = <<-SAMPLE6
puts DATA.gets
__END__
wombat
SAMPLE6

  SAMPLE7 = <<-SAMPLE7
puts DATA.gets
 __END__
wombat
SAMPLE7

  SAMPLE8 = <<-SAMPLE8
puts DATA.gets
__END__ of data
wombat
SAMPLE8

  def test___END__
    assert_equal([ 0, "wombat\n"], rubyRun(SAMPLE6))
    assert_equal(256, rubyRun(SAMPLE7)[0])
    assert_equal(256, rubyRun(SAMPLE8)[0])
  end

end