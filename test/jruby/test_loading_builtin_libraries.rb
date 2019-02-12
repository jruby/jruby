require 'test/unit'

class TestLoadingBuiltinLibraries < Test::Unit::TestCase
  def test_late_bound_libraries
    assert_nothing_raised {
      require 'jruby'
      require 'jruby/ext'
      require 'nkf'
      require 'stringio'
      require 'strscan'
      require 'zlib'
      require 'enumerator'
      require 'digest'
      require 'digest'
      require 'digest/md5'
      # rmd160 requires BouncyCastle
      #require 'digest/rmd160'
      require 'digest/sha1'
      require 'digest/sha2'
      require 'bigdecimal'
      require 'io/wait'
      require 'etc'
    }
  end
end
