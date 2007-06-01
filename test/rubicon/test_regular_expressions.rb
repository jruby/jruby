require 'test/unit'


# The regexp class outputs warnings to $stderr
# We don't want users of rubicon to see these
require 'stringio'
def IO.capture_stderr(&block)
        e = StringIO.new
        $stderr = e
        block.call
        return e.string
ensure
        $stderr = STDERR
end 

class TestRegularExpressions < Test::Unit::TestCase


  def testBasics
    assert("abc" !~ /^$/)
    assert("abc\n" !~ /^$/)
    assert("abc" !~ /^d*$/)
    assert(("abc" =~ /d*$/) == 3)
    assert_equal(0, "" =~ /^$/)
    assert_equal(0, "\n" =~ /^$/)
    assert_equal(2, "a\n\n" =~ /^$/)
    assert("abcabc" =~ /.*a/ && $& == "abca")
    assert("abcabc" =~ /.*c/ && $& == "abcabc")
    assert("abcabc" =~ /.*?a/ && $& == "a")
    assert("abcabc" =~ /.*?c/ && $& == "abc")
    assert(/(.|\n)*?\n(b|\n)/ =~ "a\nb\n\n" && $& == "a\nb")
    
    assert(/^(ab+)+b/ =~ "ababb" && $& == "ababb")
    assert(/^(?:ab+)+b/ =~ "ababb" && $& == "ababb")
    assert(/^(ab+)+/ =~ "ababb" && $& == "ababb")
    assert(/^(?:ab+)+/ =~ "ababb" && $& == "ababb")
    
    assert(/(\s+\d+){2}/ =~ " 1 2" && $& == " 1 2")
    assert(/(?:\s+\d+){2}/ =~ " 1 2" && $& == " 1 2")

    x = "ABCD\nABCD\n"
    x.gsub!(/((.|\n)*?)B((.|\n)*?)D/) {$1+$3}
    assert_equal("AC\nAC\n", x)

    assert_equal(0, "foobar" =~ /foo(?=(bar)|(baz))/)
    assert_equal(0, "foobaz" =~ /foo(?=(bar)|(baz))/)
  end

  def testReferences
    x = "a.gif"
    assert_equal("gif",     x.sub(/.*\.([^\.]+)/, '\1'))
    assert_equal("b.gif",   x.sub(/.*\.([^\.]+)/, 'b.\1'))
    assert_equal("",        x.sub(/.*\.([^\.]+)/, '\2'))
    assert_equal("ab",      x.sub(/.*\.([^\.]+)/, 'a\2b'))
    assert_equal("<a.gif>", x.sub(/.*\.([^\.]+)/, '<\&>'))
  end

  def testGlobal
    file_name = nil
    start = File.dirname($0)
    for base in [".", "language"]
      file_name = File.join(start, base, 'regexp.test')
      break if File.exist? file_name
      file_name = nil
    end

    fail("Could not find file containing regular expression tests") unless file_name

    lineno =  0
    IO.foreach(file_name) do |line|
      lineno += 1
      line.sub!(/\r?\n\z/, '')
      next if /^#/ =~ line || /^$/ =~ line
      pat, subject, result, repl, expect = line.split(/\t/, 6)
      begin
	for mes in [subject, expect]
	  if mes
	    mes.gsub!(/\\n/, "\n")
	    mes.gsub!(/\\000/, "\0")
	    mes.gsub!(/\\255/, "\255") #"
	  end
	end

	regexp = nil
	ignore = IO.capture_stderr do
		regexp = Regexp.new(pat, false)
	end
	reg = regexp.match subject
        
	case result
	when 'y'
          assert_not_nil(reg, "Expected a match: #{lineno}: '#{line}'")
          if repl != '-'
            eu = eval('"' + repl + '"')
            assert(expect == eu, "Expected '#{expect.inspect}, " +
		   "got '#{eu.inspect}'\n" +
		   "#{lineno}: '#{line.inspect}'")
          end
	when 'n'
	  assert(!reg, "Did not expect a match: #{lineno} '#{line}'")
	when 'c'
          assert_fail("'#{line}' should not have compiled")
	end
      rescue RegexpError
        assert_equal('c', result, 
                     "Regular expression did not compile: #{lineno} '#{line}'")
	fail_msg = $!.to_s
        assert_equal(expect, fail_msg, "Expected error: '#{expect}'")
#      rescue
#	assert_fail("#$!: #{lineno}: '#{line}'")
      end
    end
  end

end