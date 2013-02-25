require 'test/unit'
require 'test/test_helper'
require 'java'

# This tests JRuby-specific environment stuff, like JRUBY_VERSION
class TestEnv < Test::Unit::TestCase
  include TestHelper

  if ENV.respond_to? :key
    def key(value)
      ENV.key(value)
    end
  else
    def key(value)
      ENV.index(value)
    end
  end

  def test_jruby_version
    assert defined? JRUBY_VERSION
    assert String === JRUBY_VERSION
    assert_equal(org.jruby.runtime.Constants::VERSION, JRUBY_VERSION)
  end

  if WINDOWS
    def test_case_insensitive_on_windows
      path = ENV['PATH']
      cased_path = ENV['path']

      assert_equal(path, cased_path)
    end

    def test_env_java_case_sensitivity_on_windows
      bit = ENV_JAVA['java.home']
      cased_bit = ENV_JAVA['Java.Home']

      assert_not_equal(bit, cased_bit)
    end
  end

  IS_WINDOWS = RUBY_PLATFORM =~ /mswin/i || (RUBY_PLATFORM =~ /java/i && ENV_JAVA['os.name'] =~ /windows/i)

  def test_bracket
    assert_equal(nil, ENV['test'])
    assert_equal(nil, ENV['TEST'])

    ENV['test'] = 'foo'

    assert_equal('foo', ENV['test'])
    assert_equal(IS_WINDOWS ? 'foo' : nil, ENV['TEST'])

    ENV['TEST'] = 'bar'
    assert_equal('bar', ENV['TEST'])
    assert_equal(IS_WINDOWS ? 'bar' : 'foo', ENV['test'])

    assert_raises(TypeError) {
      tmp = ENV[1]
    }

    assert_raises(TypeError) {
      ENV[1] = 'foo'
    }

    assert_raises(TypeError) {
      ENV['test'] = 0
    }
  end

  def test_has_value
    val = 'a'
    val.succ! while ENV.has_value?(val) && ENV.has_value?(val.upcase)
    ENV['test'] = val[0...-1]

    assert_equal(false, ENV.has_value?(val))
    assert_equal(false, ENV.has_value?(val.upcase))

    ENV['test'] = val

    assert_equal(true, ENV.has_value?(val))
    assert_equal(false, ENV.has_value?(val.upcase))

    ENV['test'] = val.upcase
    assert_equal(false, ENV.has_value?(val))
    assert_equal(true, ENV.has_value?(val.upcase))
  end

  def test_index
    val = 'a'
    val.succ! while ENV.has_value?(val) && ENV.has_value?(val.upcase)
    ENV['test'] = val[0...-1]

    assert_equal(nil, key(val))
    assert_equal(nil, key(val.upcase))
    ENV['test'] = val
    assert_equal('test', key(val))

    assert_equal(nil, key(val.upcase))
    ENV['test'] = val.upcase
    assert_equal(nil, key(val))
    assert_equal('test', key(val.upcase))

    #nil values are ok (corresponding key will be deleted)
    #nil keys are not ok
    assert_raises(TypeError) { ENV[nil] }
    assert_raises(TypeError) { ENV[nil] = "foo" }

    ENV['test'] = nil
    assert_equal(nil, ENV['test'])
    assert_equal(false, ENV.has_key?('test'))
  end

  def test_jruby_3097
    name = (ENV['OS'] =~ /\AWin/i ? '%__JRUBY_T1%' : '$__JRUBY_T1')
    expected = (ENV['OS'] =~ /\AWin/i ? '%__JRUBY_T1%' : '')
    v = `echo #{name}`.chomp
    assert_equal expected, v
    ENV['__JRUBY_T1'] = "abc"
    v = `echo #{name}`.chomp
    assert_equal "abc", v

    # See JRUBY-3097
    # try setting up PATH in such a way that doesn't find 'jruby'
    # but 'java' is available
    jruby_exe = File.join(File.dirname(__FILE__), '..', 'bin', 'jruby')
    old_path = ENV['PATH']
    our_path = []
    old_path.split(File::PATH_SEPARATOR).each do |dir|
      our_path << dir unless File.exist? File.join(dir, 'jruby')
    end
    unless our_path.select { |dir| File.exist? File.join(dir, 'java') }.empty?
      assert_equal "abc", `PATH=#{our_path.join(File::PATH_SEPARATOR)} #{jruby_exe} -e "puts ENV[%{__JRUBY_T1}]"`.chomp
    end
    ENV['PATH'] = old_path
  end

  def test_jruby_2393
    # JRUBY-2393
    assert(ENV.object_id != ENV.to_hash.object_id)
  end
end
