# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# A workflow tool for JRuby+Truffle development

# Recommended: function jt { ruby PATH/TO/jruby/tool/jt.rb $@; }

JRUBY_DIR = File.expand_path('../..', __FILE__)

module Utilities

  GRAAL_LOCATIONS = [
    ENV['GRAAL_BIN'],
    '../graalvm-jdk1.8.0/bin/java',         # This also seems like a sensible place to keep it
    '../../graal/graalvm-jdk1.8.0/bin/java' # This is where I (CS) keep it
  ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

  BENCH_LOCATIONS = [
    ENV['BENCH_DIR'],
    '../bench9000'
  ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

  def self.find_graal
    not_found = -> {
      raise "couldn't find graal - download it from http://lafo.ssw.uni-linz.ac.at/graalvm/ and extract it into the parent directory"
    }
    GRAAL_LOCATIONS.find(not_found) do |location|
      File.executable?(location)
    end
  end

  def self.find_bench
    not_found = -> {
      raise "couldn't find bench9000 - clone it from https://github.com/jruby/bench9000.git into the parent directory"
    }
    BENCH_LOCATIONS.find(not_found) do |location|
      Dir.exist?(location)
    end
  end

end

module ShellUtils
  private

  def raw_sh(*args)
    begin
      result = system(*args)
    rescue Interrupt
      abort # Ignore Ctrl+C
    else
      unless result
        $stderr.puts "FAILED (#{$?}): #{args * ' '}"
        exit $?.exitstatus
      end
    end
  end

  def sh(*args)
    Dir.chdir(JRUBY_DIR) do
      raw_sh(*args)
    end
  end

  def mvn(*args)
    sh 'mvn', *args
  end

  def mspec(command, *args)
    sh 'ruby', 'spec/mspec/bin/mspec', command, '--config', 'spec/truffle/truffle.mspec', *args
  end
end

module Commands
  include ShellUtils

  def help
    puts 'jt build                                     build'
    puts 'jt clean                                     clean'
    puts 'jt rebuild                                   clean and build'
    puts 'jt run [options] args...                     run JRuby with -X+T and args'
    puts '    --graal        use Graal (set GRAAL_BIN or it will try to automagically find it)'
    puts '    --asm          show assembly (implies --graal)'
    puts 'jt test                                      run all specs'
    puts 'jt test fast                                 run all specs except sub-processes, GC, sleep, ...'
    puts 'jt test spec/ruby/language                   run specs in this directory'
    puts 'jt test spec/ruby/language/while_spec.rb     run specs in this file'
    puts 'jt test pe                                   run partial evaluation tests'
    puts 'jt tag spec/ruby/language                    tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb      tag failing specs in this file'
    puts 'jt untag spec/ruby/language                  untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb    untag passing specs in this file'
    puts 'jt bench reference [benchmarks]              run a set of benchmarks and record a reference point'
    puts 'jt bench compare [benchmarks]                run a set of benchmarks and compare against a reference point'
    puts '    benchmarks can be any benchmarks of group of benchmarks supported'
    puts '    by bench9000, eg all, classic, chunky, 3, 5, 10, 15 - default is 5'
    puts 'jt findbugs                                  run findbugs'
    puts 'jt findbugs report                           run findbugs and generate an HTML report'
    puts
    puts 'you can also put build or rebuild in front of any command'
  end

  def build
    mvn 'package'
  end

  def clean
    mvn 'clean'
  end

  def rebuild
    clean
    build
  end

  def run(*args)
    env_vars = {}
    jruby_args = %w[-X+T]

    { '--asm' => '--graal' }.each_pair do |arg, dep|
      args.unshift dep if args.include?(arg)
    end

    if args.delete('--graal')
      env_vars["JAVACMD"] = Utilities.find_graal
      jruby_args << '-J-server'
    end

    if args.delete('--asm')
      jruby_args += %w[-J-XX:+UnlockDiagnosticVMOptions -J-XX:CompileCommand=print,*::callRoot]
    end

    raw_sh(env_vars, "#{JRUBY_DIR}/bin/jruby", *jruby_args, *args)
  end

  def test(*args)
    return test_pe if args == ['pe']

    options = %w[--excl-tag fails]
    if args.first == 'fast'
      args.shift
      options += %w[--excl-tag slow]
    end
    args = [':language', ':core'] if args.empty?
    mspec 'run', *options, *args
  end

  def test_pe
    run(*%w[
            --graal
            -J-G:+TruffleCompilationExceptionsAreThrown
            -Xtruffle.debug.enable_assert_constant=true
            test/truffle/pe/pe.rb])
  end
  private :test_pe

  def tag(path, *args)
    mspec 'tag', '--add', 'fails', '--fail', path, *args
  end

  def untag(path, *args)
    puts
    puts "WARNING: untag is currently not very reliable - run `jt test #{path} #{args * ' '}` after and manually annotate any new failures"
    puts
    mspec 'tag', '--del', 'fails', '--pass', path, *args
  end

  def bench(command, *args)
    bench_dir = Utilities.find_bench
    env_vars = {
      "JRUBY_9000_DEV_DIR" => JRUBY_DIR,
      "GRAAL_BIN" => Utilities.find_graal,
    }
    args << "5" if args.empty?
    bench_args = ["-I#{bench_dir}/lib", "#{bench_dir}/bin/bench"]
    case command
    when 'reference'
      bench_args += ['reference', 'jruby-9000-dev-truffle-graal', '--show-commands']
    when 'compare'
      bench_args += ['compare-reference', 'jruby-9000-dev-truffle-graal']
    else
      raise ArgumentError, command
    end
    raw_sh env_vars, "ruby", *bench_args, *args
  end

  def findbugs(report=nil)
    case report
    when 'report'
      sh 'tool/truffle-findbugs.sh', '--report' rescue nil
      sh 'open', 'truffle-findbugs-report.html' rescue nil
    when nil
      sh 'tool/truffle-findbugs.sh'
    else
      raise ArgumentError, report
    end
  end
end

class JT
  include Commands

  def main(args)
    args = args.dup

    if args.empty? or %w[-h -help --help].include? args.first
      help
      exit
    end

    send args.shift if %w[build rebuild].include? args.first

    return if args.empty?

    commands = Commands.public_instance_methods(false).map(&:to_s)

    abort "no command matched #{args.first.inspect}" unless commands.include?(args.first)

    begin
      send(*args)
    rescue
      puts "Error during command: #{args*' '}"
      raise $!
    end
  end
end

JT.new.main(ARGV)
