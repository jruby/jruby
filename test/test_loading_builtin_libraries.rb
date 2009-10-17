require 'test/unit'

class TestLoadingBuiltinLibraries < Test::Unit::TestCase
  def test_late_bound_libraries
    assert_nothing_raised {
      require 'jruby.rb'
      require 'jruby/ext.rb'
      require 'iconv.so'
      require 'nkf.so'
      require 'stringio.so'
      require 'strscan.so'
      require 'zlib.so'
      require 'enumerator.so'
      require 'readline.so'
      require 'thread.so'
      require 'digest.so'
      require 'digest.rb'
      require 'digest/md5.so'
      # rmd160 requires BouncyCastle
      #require 'digest/rmd160.rb'
      require 'digest/sha1.so'
      require 'digest/sha2.so'
      require 'bigdecimal.so'
      require 'io/wait.so'
      require 'etc.so'
    }
  end
end
