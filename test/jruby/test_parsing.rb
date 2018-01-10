require 'test/unit'

class TestParsing < Test::Unit::TestCase
  # JRUBY-376
  def test_parse_empty_parens
    assert_raises(TypeError) { n = 3 * () }
   
    # JRUBY-755
    assert(nil == ())
    assert(().nil?)
  end
  
  def test_parse_literal_char
    assert_equal('a', ?a)
  end

#  FIXME: does not pass in 2.0 mode
#  def test_bogus_char
#    begin
#      eval "\277"
#    rescue SyntaxError
#      assert($!.message =~ /277/)
#    end
#  end

  def test_parse_empty_proc_with_explicit_line_number
    s = "proc{\n}"
    eval s, binding, "file.rb", 0
  end
  
  # JRUBY-2499
  def test_parse_of_do_symbol
    foo :do
  end
  
  def foo(*args)
  end

  def test_parse_invalid_gvar
    assert_equal '# comment', eval('"# comment"')
    assert_equal '#$', eval('%{#$}')
    assert_equal '##$', eval('%{##$}')
    assert_equal '#${', eval('"#${"')
    assert_equal ' #${', eval('" #${"')
    assert_equal ' # ##${', eval('" # ##${"')
    assert_equal " \#${\n", eval("<<E\n \#$\{\nE\n")
    assert_equal " # #\#${\n", eval("<<E\n \# \#\#$\{\nE\n")
  end
end
