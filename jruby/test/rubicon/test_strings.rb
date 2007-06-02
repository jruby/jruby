require 'test/unit'

# Tests of functions in the String class are mostly in ../builtins/TestString.rb

class TestStrings < Test::Unit::TestCase

  def testCompileTimeStringConcatenation
    assert_equal("abcd", "ab" "cd")
    assert_equal("abcd", 'ab' "cd")
    assert_equal("abcd", 'ab' 'cd')
    assert_equal("abcd", "ab" 'cd')
    assert_equal("22aacd44", "#{22}aa" "cd#{44}")
    assert_equal("22aacd445566", "#{22}aa" "cd#{44}" "55" "#{66}")
  end

  # ------------------------------------------------------------

  def testInterpolationOfGlobal
    $foo = "abc"
    assert_equal("abc = abc", "#$foo = abc")
    assert_equal("abc = abc", "#{$foo} = abc")
    
    foo = "abc"
    assert_equal("abc = abc", "#{foo} = abc")
  end

  # ------------------------------------------------------------

  def testSingleQuoteStringLiterals
    assert_equal("abc\"def\#{abc}", 'abc"def#{abc}')
    assert_equal('abc"def#{abc}', %q/abc"def#{abc}/)
    assert_equal('abc"def#{abc}', %q{abc"def#{abc}})
    assert_equal('abc"def#{abc}', %q(abc"def#{abc}))
    assert_equal('abc"def#{abc}', %q
abc"def#{abc}
)
  end

  # ------------------------------------------------------------
  
  def testDoubleQuoteStringLiterals
    foo = "abc"
    assert_equal('"abc#{foo}"', "\"#{foo}\#{foo}\"")
    assert_equal('"abc#{foo}"', "\"#{foo}\#{foo}\"")
    assert_equal('/"abc/#{foo}"/', %/\/"#{foo}\/\#{foo}"\//)
    assert_equal('/"abc/#{foo}"/', %Q/\/"#{foo}\/\#{foo}"\//)
    assert_equal('/"abc/#{foo}"/', %Q{/"abc/\#{foo}"/})
    assert_equal("abc\ndef", %Q{abc
def})
  end

  # ------------------------------------------------------------

  def testHereDocuments
    assert_equal("abc\n", <<EOF)
abc
EOF

    foo = "abc"
    assert_equal("abc\n", <<EOF)
#{foo}
EOF
    assert_equal("abc\n", <<"EOF")
#{foo}
EOF
    assert_equal("\#{foo}\n", <<'EOF')
#{foo}
EOF

    assert_equal(<<EOF1, <<EOF2)
a
EOF1
a
EOF2
    
    assert_equal("foo\n", <<"")
foo

    # Test that <<-EOF does not strip leading whitespace
    assert_equal("    foo\n    bar\n", <<-EOF)
    foo
    bar
    EOF
  end

  # ------------------------------------------------------------

end