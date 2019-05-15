require 'test/unit'

class TestLoadingBuiltinLibraries < Test::Unit::TestCase
  def test_late_bound_libraries
    assert_nothing_raised {
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
      require 'rbconfig'
      require 'tempfile'
    }
  end

  # JRUBY-4962
  def test_require_pty
    assert_nothing_raised { require 'pty' }
  end

  def test_jruby_libraries
    assert_nothing_raised {
      require 'jruby'
      require 'jruby/ext'
      require 'java'
    }
  end if defined? JRUBY_VERSION

  def test_does_not_load_ji_on_boot
    code  = "all = []; ObjectSpace.each_object(Module) { |mod| all << mod }; "
    code += "p all.count { |m| m.is_a?(Java::JavaPackage) }; " # <= 3
    # org.jruby.java.util (SystemPropertiesMap) and dependencies :
    # java.util (Map) implemented interface
    # java.lang (Object) super-class
    code += "all.each { |m| m.inspect }; " # if self-reflecting this would fail (on RubyBasicObject.UNDEF)

    out = `#{RbConfig.ruby} -e '#{code}'`
    assert $?.success?, "JRuby self-reflected (JI) during boot!"
    pkg_count = out.strip.to_i
    assert pkg_count <= 3 # due ENV_JAVA we (still) load 3 Java packages - see ^^^

    requires = [ 'stringio',
                 'rbconfig',
                 'enumerator',
                 'set',
                 'time',
                 'cgi',
                 'date',
                 'digest',
                 'digest/md5',
                 'digest/sha1',
                 'digest/sha2',
                 'drb',
                 'etc',
                 'ffi',
                 'fiber',
                 'irb',
                 'io/wait',
                 'bigdecimal',
                 'ostruct',
                 'open3',
                 'pathname',
                 'pp',
                 'pty',
                 'rational',
                 'securerandom',
                 'socket',
                 'strscan',
                 'thread',
                 'tempfile',
                 'tmpdir',
                 'timeout',
                 'zlib',

                 'jar-dependencies',
                 'psych',
                 'openssl',
                 'yaml',
    ]
    requires = requires.map { |lib| "-r#{lib}" }.join(' ')
    out = `#{RbConfig.ruby} #{requires} -e '#{code}'`
    assert $?.success?, "a library self-reflected (JI) during boot!"
    assert_equal pkg_count.to_s, out.strip
  end if defined? JRUBY_VERSION

end
