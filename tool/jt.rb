# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# A workflow tool for JRuby+Truffle development

# Recommended: function jt { ruby tool/jt.rb $@; }

module ShellUtils
  private

  def sh(*args)
    system args.join(' ')
    raise "failed" unless $? == 0
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
    puts 'jt test                                      run all specs'
    puts 'jt test spec/ruby/language                   run specs in this directory'
    puts 'jt test spec/ruby/language/while_spec.rb     run specs in this file'
    puts 'jt tag spec/ruby/language                    tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb      tag failing specs in this file'
    puts 'jt untag spec/ruby/language                  untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb    untag passing specs in this file'
    puts 'jt findbugs                                  run findbugs'
    puts 'jt findbugs report                           run findbugs and generate an HTML report'
    puts
    puts 'you can also put build or redbuild in front of any command'
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

  def test(path=nil)
    if path == nil
      mspec 'run', '--excl-tag', 'fails', ':language', ':core'
    elsif path.start_with? 'spec/ruby'
      mspec 'run', '--excl-tag', 'fails', path
    else
      raise ArgumentError, "don't know how to test #{path}"
    end
  end

  def tag(path)
    mspec 'tag', '--add', 'fails', '--fail', path
  end

  def untag(path)
    puts
    puts "WARNING: untag is currently not very reliable - run test #{path} after and manually annotate any new failures"
    puts
    mspec 'tag', '--del', 'fails', '--pass', path
  end

  def findbugs(report=nil)
    case report
    when "report"
      sh 'tool/truffle-findbugs.sh --report' rescue nil
      sh 'open truffle-findbugs-report.html' rescue nil
    when nil
      sh 'tool/truffle-findbugs.sh'
    else
      raise ArgumentError, report
    end
  end
end

class JT
  include Commands

  def run(args)
    args = args.dup

    if args.empty? or !(args & %w[-h -help --help]).empty?
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

JT.new.run(ARGV)
