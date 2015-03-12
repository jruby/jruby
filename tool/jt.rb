#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# A workflow tool for JRuby+Truffle development

# Recommended: function jt { ruby PATH/TO/jruby/tool/jt.rb $@; }

require 'fileutils'
require 'digest/sha1'

JRUBY_DIR = File.expand_path('../..', __FILE__)

module Utilities

  GRAAL_LOCATIONS = [
    ENV['GRAAL_BIN'],
    'graalvm-jdk1.8.0/bin/java',
    '../graalvm-jdk1.8.0/bin/java',
    '../../graal/graalvm-jdk1.8.0/bin/java'
  ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

  BENCH_LOCATIONS = [
    ENV['BENCH_DIR'],
    'bench9000',
    '../bench9000'
  ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

  def self.find_graal
    not_found = -> {
      raise "couldn't find graal - download it from http://lafo.ssw.uni-linz.ac.at/graalvm/ and extract it into the JRuby repository or parent directory"
    }
    GRAAL_LOCATIONS.find(not_found) do |location|
      File.executable?(location)
    end
  end

  def self.find_bench
    not_found = -> {
      raise "couldn't find bench9000 - clone it from https://github.com/jruby/bench9000.git into the JRuby repository or parent directory"
    }
    BENCH_LOCATIONS.find(not_found) do |location|
      Dir.exist?(location)
    end
  end

  def self.jruby_version
    File.read("#{JRUBY_DIR}/VERSION").strip
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
    puts 'jt build truffle                             build only the Truffle part, assumes the rest is up-to-date'
    puts 'jt clean                                     clean'
    puts 'jt rebuild                                   clean and build'
    puts 'jt run [options] args...                     run JRuby with -X+T and args'
    puts '    --graal        use Graal (set GRAAL_BIN or it will try to automagically find it)'
    puts '    --asm          show assembly (implies --graal)'
    puts '    --server       run an instrumentation server on port 8080'
    puts 'jt test                                      run all specs'
    puts 'jt test fast                                 run all specs except sub-processes, GC, sleep, ...'
    puts 'jt test spec/ruby/language                   run specs in this directory'
    puts 'jt test spec/ruby/language/while_spec.rb     run specs in this file'
    puts 'jt test pe                                   run partial evaluation tests'
    puts 'jt tag spec/ruby/language                    tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb      tag failing specs in this file'
    puts 'jt tag all spec/ruby/language                tag all specs in this file, without running them'
    puts 'jt untag spec/ruby/language                  untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb    untag passing specs in this file'
    puts 'jt bench debug benchmark                     run a single benchmark with options for compiler debugging'
    puts 'jt bench reference [benchmarks]              run a set of benchmarks and record a reference point'
    puts 'jt bench compare [benchmarks]                run a set of benchmarks and compare against a reference point'
    puts '    benchmarks can be any benchmarks of group of benchmarks supported'
    puts '    by bench9000, eg all, classic, chunky, 3, 5, 10, 15 - default is 5'
    puts 'jt findbugs                                  run findbugs'
    puts 'jt findbugs report                           run findbugs and generate an HTML report'
    puts 'jt install ..../graal/mx/suite.py            install a JRuby distribution into an mx suite'
    puts
    puts 'you can also put build or rebuild in front of any command'
  end

  def build(project = nil)
    case project
    when 'truffle'
      mvn '-pl', 'truffle', 'package'
    when nil
      mvn 'package'
    else
      raise ArgumentError, project
    end
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

    if args.delete('--jdebug')
      jruby_args += %w[-J-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y]
    end

    if args.delete('--server')
      jruby_args += %w[-Xtruffle.instrumentation_server_port=8080 -Xtruffle.passalot=1]
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
    run(*%w[--graal test/truffle/pe/pe.rb])
  end
  private :test_pe

  def tag(path, *args)
    return tag_all(*args) if path == 'all'
    mspec 'tag', '--add', 'fails', '--fail', path, *args
  end

  # Add tags to all given examples without running them. Useful to avoid file exclusions.
  def tag_all(*args)
    mspec('tag', *%w[--unguarded --all --dry-run --add fails], *args)
  end
  private :tag_all

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
    bench_args = ["-I#{bench_dir}/lib", "#{bench_dir}/bin/bench"]
    case command
    when 'debug'
      env_vars = env_vars.merge({'JRUBY_OPTS' => '-J-G:+TraceTruffleCompilation -J-G:+DumpOnError'})
      bench_args += ['score', 'jruby-9000-dev-truffle-graal', '--show-commands', '--show-samples']
      raise 'specify a single benchmark for run - eg classic-fannkuch-redux' if args.size != 1
    when 'reference'
      bench_args += ['reference', 'jruby-9000-dev-truffle-graal', '--show-commands']
      args << "5" if args.empty?
    when 'compare'
      bench_args += ['compare-reference', 'jruby-9000-dev-truffle-graal']
      args << "5" if args.empty?
    else
      raise ArgumentError, command
    end
    raw_sh env_vars, "ruby", *bench_args, *args
  end

  def findbugs(report=nil)
    case report
    when 'report'
      sh 'tool/truffle-findbugs.sh', '--report'
      sh 'open', 'truffle-findbugs-report.html'
    when nil
      sh 'tool/truffle-findbugs.sh'
    else
      raise ArgumentError, report
    end
  end

  def install(arg)
    case arg
    when /.*suite.*\.py$/
      suite_file = arg
      mvn '-Pcomplete'
      sh 'tool/remove-bundled-truffle.sh'
      jar_name = "jruby-complete-no-truffle-#{Utilities.jruby_version}.jar"
      source_jar_name = "maven/jruby-complete/target/#{jar_name}"
      shasum = Digest::SHA1.hexdigest File.read(source_jar_name)
      shasum_jar_name = "jruby-complete-no-truffle-#{Utilities.jruby_version}-#{shasum}.jar"
      FileUtils.cp source_jar_name, "#{File.expand_path('../..', suite_file)}/lib/#{shasum_jar_name}"
      suite_lines = File.readlines(suite_file)
      line_index = suite_lines.find_index { |line| line.start_with? '      "path" : "lib/jruby-complete-no-truffle' }
      suite_lines[line_index] = "      \"path\" : \"lib/#{shasum_jar_name}\",\n"
      suite_lines[line_index + 1] = "      \#\"urls\" : [\"http://lafo.ssw.uni-linz.ac.at/truffle/ruby/#{shasum_jar_name}\"],\n"
      suite_lines[line_index + 2] = "      \"sha1\" : \"#{shasum}\"\n"
      File.write(suite_file, suite_lines.join())
    else
      raise ArgumentError, kind
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

    case args.first
    when "rebuild"
      send(args.shift)
    when "build"
      command = [args.shift]
      command << args.shift if args.first == "truffle"
      send(*command)
    end

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
