# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/9319

require 'tmpdir'

# An AOT-restored boot layer skips Module.defineModules, dropping org.jruby.dist's read
# edge to the unnamed module JRubyClassLoader loads gem classes into.
SRC = <<~RUBY
  $CLASSPATH << 'test/target/test-classes'
  obj = Java::Java_integrationFixtures::ClassWithSimpleMethod.new
  puts obj.foo('bar')
RUBY

def aot_cache_supported?
  ENV_JAVA['java.specification.version'].to_i >= 25
end

describe 'a Java class loaded from the JRuby class loader' do
  before { skip 'requires a JVM with AOT cache support' unless aot_cache_supported? }

  it 'keeps its bound methods when the boot layer comes from an AOT cache' do
    # --nocache avoids -XX:SharedArchiveFile, which the JVM rejects alongside AOT cache flags.
    jruby = "#{ENV_JAVA['jruby.home']}/bin/jruby --nocache"

    Dir.mktmpdir('jruby-9319') do |dir|
      cache = File.join(dir, 'test.aot')
      script = File.join(dir, 'probe.rb')
      File.write(script, SRC)

      expect(`#{jruby} -J-XX:AOTCacheOutput=#{cache} #{script} 2>&1`).to include('foobar')
      expect(File).to exist(cache)

      expect(`#{jruby} -J-XX:AOTCache=#{cache} #{script} 2>&1`).to include('foobar')
    end
  end
end
