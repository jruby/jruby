require 'test/unit'
require 'test/jruby/test_helper'

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
  end unless TestHelper::WINDOWS

  def test_jruby_libraries
    assert_nothing_raised {
      require 'jruby'
      require 'jruby/ext'
      require 'java'
    }
  end if defined? JRUBY_VERSION

  def test_does_not_load_ji_on_boot
    pend "TODO: JRuby (still) self-reflects on Windows" if TestHelper::WINDOWS

    code  = "all = []; ObjectSpace.each_object(Module) { |mod| all << mod }; "
    code += "packages = all.select { |m| m.is_a?(Java::JavaPackage) }; "
    code += "p packages.size; packages.each { |m| p m }; "
    # TODO due ENV_JAVA we (still) load 3 Java packages
    # org.jruby.java.util (SystemPropertiesMap) and dependencies :
    # java.util (Map) implemented interface
    # java.lang (Object) super-class
    # TODO since 9.3 also java.lang.Class and dependencies :
    # java.lang.reflect (interfaces)
    # java.io (Class implements java.io.Serializable)
    #code += "all.select { |m| m.inspect.start_with? \"Java::\" }.each { |m| puts m.inspect };"
    # self-reflecting this would fail (on RubyBasicObject.UNDEF)

    expected_count = 7 # 5 on JDK 11 TODO on Java 14 loads 2 more (Java::JavaLangInvoke, Java::JavaLangConstant)

    out = `#{RbConfig.ruby} -e '#{code}'`
    assert $?.success?, "JRuby self-reflected (JI) during boot!"
    pkg_count = out.strip.to_i
    assert pkg_count <= expected_count, "expected less than #{expected_count} packages but loaded: #{out}"

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
    lib_out = `#{RbConfig.ruby} #{requires} -e '#{code}'`
    assert $?.success?, "a library self-reflected (JI) during boot!"
    assert_same_output_lines out, lib_out
  end if defined? JRUBY_VERSION

  private

  def assert_same_output_lines(expected, actual)
    p expected.split("\n").sort
    assert_equal expected.split("\n").sort, actual.split("\n").sort
  end

end
