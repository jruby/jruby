require 'test/unit'

class TestInvocation < Test::Unit::TestCase

  TMP = "script_tmp"
  
  $interpreter = 'ruby'

  def teardown
    begin
      File.unlink TMP or `/bin/rm -f "#{TMP}"`
      File.unlink TMP + ".bak" or `/bin/rm -f "#{TMP}.bak"`
    rescue
    end
  end

  def tmp_write
    File.open(TMP, "w") do |f|
      yield f
    end
  end

  # --------------------------------------------------------

  def test_00_Basic
    assert_equal("foobar", `#$interpreter -e 'print "foobar"'`)
  end

  def testUnadornedScript
    tmp_write { |f| f.puts "print $zzz" }

    assert_equal('true', `#$interpreter -s script_tmp -zzz`)
    assert_equal('555', `#$interpreter -s script_tmp -zzz=555`)
  end

  def testScriptWithShebang
    tmp_write do |f|
      f.puts "#! /usr/local/bin/ruby -s"
      f.puts "print $zzz"
    end

    assert_equal('678', `#$interpreter script_tmp -zzz=678`)
  end

  def testScriptWithLeadingJunk
    tmp_write do |f|
      f.puts "this is a leading junk"
      f.puts "#! /usr/local/bin/ruby -s"
      f.puts "print $zzz"
      f.puts "__END__"
      f.puts "this is a trailing junk"
    end

    assert_equal('nil', `#$interpreter -x script_tmp`)
    assert_equal('555', `#$interpreter -x script_tmp -zzz=555`)
  end


  def testSciptWith_pe
    tmp_write do |f|
      for i in 1..5
        f.puts i
      end
    end

    `#$interpreter -i.bak -pe 'sub(/^[0-9]+$/){$&.to_i * 5}' script_tmp`

    File.open(TMP) do |f|
      while line = f.gets
        assert_equal(0, line.to_i % 5)
      end
    end
  end

end