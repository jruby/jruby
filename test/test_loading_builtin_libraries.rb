require 'test/unit'

class TestLoadingBuiltinLibraries < Test::Unit::TestCase
  def test_late_bound_libraries
    assert_nothing_raised {
      require 'jruby.rb'
      require 'jruby/ext.rb'
      require 'iconv.rb'
      require 'nkf.rb'
      require 'stringio.rb'
      require 'strscan.rb'
      require 'zlib.rb'
      require 'yaml_internal.rb'
      require 'enumerator.rb'
      require 'generator_internal.rb'
      require 'readline.rb'
      require 'thread.so'
      require 'digest.so'
      require 'digest.rb'
      require 'digest/md5.rb'
      # rmd160 requires BouncyCastle
      #require 'digest/rmd160.rb'
      require 'digest/sha1.rb'
      require 'digest/sha2.rb'
      require 'bigdecimal.rb'
      require 'io/wait.so'
      require 'etc.so'
      require 'fiber.so'
    }
  end
end
