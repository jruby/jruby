# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'optparse'
require 'pp'
require 'yaml'
require 'fileutils'
require 'shellwords'
require 'pathname'

# TODO (pitr 01-Sep-2015): add pre stored run options combinations like -S irb, -I test

class JRubyTruffleRunner
  attr_reader :options

  EXECUTABLE        = 'jruby+truffle'
  LOCAL_CONFIG_FILE = '.jruby+truffle.yaml'

  begin
    assign_new_value   = -> (new, old) { new }
    add_to_array       = -> (new, old) { old << new }
    merge_hash         = -> ((k, v), old) { old.merge k => v }

    # Format:
    #   subcommand_name: {
    #     :'option_name (also used as a key in yaml)' => [option_parser_on_method_args,
    #                                                     -> (new_value, old_value) { result_of_this_block_is_stored },
    #                                                     default_value]
    #   }
    OPTION_DEFINITIONS = {
        global: {
            verbose:             ['-v', '--verbose', 'Run verbosely (prints options)', assign_new_value, false],
            help:                ['-h', '--help', 'Show this message', assign_new_value, false],
            debug_port:          ['--debug-port PORT', 'Debug port', assign_new_value, '51819'],
            debug_option:        ['--debug-option OPTION', 'Debug JVM option', assign_new_value,
                                  '-J-agentlib:jdwp=transport=dt_socket,server=y,address=%d,suspend=y'],
            truffle_bundle_path: ['--truffle-bundle-path NAME', 'Bundle path', assign_new_value, '.jruby+truffle_bundle'],
            jruby_truffle_path:  ['--jruby-truffle-path PATH', 'Path to JRuby+Truffle bin/jruby', assign_new_value,
                                  '../jruby/bin/jruby'],
            graal_path:          ['--graal-path PATH', 'Path to Graal', assign_new_value, '../graalvm-jdk1.8.0/bin/java'],
            mock_load_path:      ['--mock-load-path PATH', 'Path of mocks & monkey-patches (prepended in $:, relative to --truffle_bundle_path)',
                                  assign_new_value, 'mocks']
        },
        setup:  {
            help:    ['-h', '--help', 'Show this message', assign_new_value, false],
            after:   ['--after SH_CMD', 'Commands to execute after setup', add_to_array, []],
            file:    ['--file NAME,CONTENT', Array, 'Create file in truffle_bundle_path', merge_hash, {}],
            without: ['--without GROUP', 'Do not install listed gem group by bundler', add_to_array, []]
        },
        run:    {
            help:       ['-h', '--help', 'Show this message', assign_new_value, false],
            test:       ['-t', '--test', 'Do not use Truffle use plain JRuby', assign_new_value, false],
            graal:      ['-g', '--graal', 'Run on graal', assign_new_value, false],
            debug:      ['-d', '--debug', 'JVM remote debugging', assign_new_value, false],
            require:    ['-r', '--require FILE', 'Files to require, same as Ruby\'s -r', add_to_array, []],
            load_path:  ['-I', '--load-path LOAD_PATH', 'Paths to add to load path, same as Ruby\'s -I', add_to_array, []],
            jexception: ['--jexception', 'print Java exceptions', assign_new_value, false]
        },
        clean:  {
            help: ['-h', '--help', 'Show this message', assign_new_value, false]
        }
    }
  end

  begin
    global_help = <<-TXT.gsub(/^ {6}/, '')
      Usage: #{EXECUTABLE} [options] [subcommand [subcommand-options]]
      Subcommands are: #{(OPTION_DEFINITIONS.keys - [:global]).map(&:to_s).join(', ')}

      Allows to execute gem/app on JRuby+Truffle until it's more complete. Environment
      has to be set up first with setup subcommand then run subcommand can be used.

      Options can be set on commandline or in local directory in #{LOCAL_CONFIG_FILE} file.
      Tha data in yaml file follow same structure as OPTION_DEFINITIONS what, its values
      are deep-merged with default values, then command-line options are applied.

      This tool contains default configurations for gems in gem_configurations directory.
      They are copied when there is no configuration present.

    TXT

    setup_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] setup [subcommand-options]

      Creates environment for running gem/app on JRuby+Truflle.

    TXT

    run_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] run [subcommand-options] -- [ruby-options]

      Runs file, -e expr, etc in setup environment on JRuby+Truffle
      Examples: #{EXECUTABLE} run -- a_file.rb
                #{EXECUTABLE} run -- -S irb
                #{EXECUTABLE} run -- -e 'puts :v'
                #{EXECUTABLE} --verbose run -- -Itest test/a_test_file_test.rb

    TXT

    clean_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] clean [subcommand-options]

      Deletes all files created by setup subcommand.

    TXT

    HELP = { global: global_help, setup: setup_help, run: run_help, clean: clean_help }
  end


  def initialize(argv = ARGV)
    construct_default_options
    load_local_yaml_configuration
    build_option_parsers

    subcommand, *argv_after_global = @option_parsers[:global].order argv

    if subcommand.nil?
      print_options
      help
    end

    subcommand = subcommand.to_sym

    subcommand_option_parser = @option_parsers[subcommand] || raise("unknown subcommand: #{subcommand}")
    argv_after_subcommand    = subcommand_option_parser.order argv_after_global

    print_options
    help subcommand if @options[subcommand][:help]

    send "subcommand_#{subcommand}", argv_after_subcommand
  end

  def print_options
    if verbose?
      puts 'Options:'
      pp @options
    end
  end

  private

  def verbose?
    @options[:global][:verbose]
  end

  def build_option_parsers
    @option_parsers = OPTION_DEFINITIONS.each_with_object({}) do |(group, group_options), parsers|
      parsers[group] = OptionParser.new do |option_parser|
        group_options.each do |option, data|
          *args, description, block, default = data

          option_parser.on(*args, description + " (default: #{default.inspect})") do |new_value|
            old_value               = @options[group][option]
            @options[group][option] = block.call new_value, old_value
          end
        end
      end
    end

    @option_parsers.each { |key, option_parser| option_parser.banner = HELP[key] }
  end

  def load_local_yaml_configuration
    yaml_path = File.join Dir.pwd, LOCAL_CONFIG_FILE

    unless File.exist? yaml_path
      candidates = Dir['*.gemspec']
      if candidates.size == 1
        gem_name, _ = candidates.first.split('.')

        default_configuration_file_path = File.dirname(__FILE__) + "/../gem_configurations/#{gem_name}.yaml"
        if File.exist?(default_configuration_file_path)
          puts "Copying default #{LOCAL_CONFIG_FILE} for #{gem_name}."
          FileUtils.cp default_configuration_file_path, LOCAL_CONFIG_FILE
        end
      end
    end

    yaml_data = YAML.load_file(yaml_path) if File.exist?(yaml_path)
    @options  = deep_merge @options, yaml_data
  end

  def construct_default_options
    @options = OPTION_DEFINITIONS.each_with_object({}) do |(group, group_options), options|
      group_options.each_with_object(options[group] = {}) do |(option, data), group_option_defaults|
        *args, block, default         = data
        group_option_defaults[option] = default
      end
    end
  end

  def help(key = nil)
    parsers = if key
                [@option_parsers[key]]
              else
                @option_parsers.values
              end
    puts *parsers
    exit
  end

  def deep_merge(a, b)
    if Hash === a
      if Hash === b
        return a.merge(b) { |k, ov, nv| deep_merge ov, nv }
      else
        return a
      end
    end

    if Array === a
      if Array === b
        return a + b.map { |v| eval_yaml_strings v }
      else
        return a
      end
    end

    eval_yaml_strings b
  end

  def eval_yaml_strings(value)
    if String === value
      begin
        eval('"' + value.gsub(/\\#|"|\\/, '\#' => '\#', '"' => '\"', '\\' => '\\\\') + '"')
      rescue => e
        p value
        raise e
      end
    else
      value
    end
  end

  def subcommand_setup(rest)
    bundle_path      = File.expand_path @options[:global][:truffle_bundle_path]
    bundle_installed = execute_cmd 'command -v bundle 2>/dev/null 1>&2', fail: false

    execute_cmd 'gem install bundler' unless bundle_installed

    execute_cmd ['bundle',
                 'install',
                 '--standalone',
                 '--path', bundle_path,
                 *(['--without', @options[:setup][:without].join(' ')] unless @options[:setup][:without].empty?)]

    link_path = "#{bundle_path}/jruby+truffle"
    FileUtils.rm link_path if File.exist? link_path
    execute_cmd "ln -s #{bundle_path}/#{RUBY_ENGINE} #{link_path}"

    mock_path = "#{bundle_path}/#{@options[:global][:mock_load_path]}"
    FileUtils.mkpath mock_path

    @options[:setup][:file].each do |name, content|
      puts "creating file: #{mock_path}/#{name}" if verbose?
      File.write "#{mock_path}/#{name}", content
    end

    File.open("#{bundle_path}/bundler/setup.rb", 'a') do |f|
      f.write %[$:.unshift "\#{path}/../#{@options[:global][:mock_load_path]}"]
    end

    @options[:setup][:after].each do |cmd|
      execute_cmd cmd
    end
  end

  def subcommand_run(rest)
    core_load_path = Pathname("#{@options[:global][:jruby_truffle_path]}/../../truffle/src/main/ruby").
        relative_path_from(Pathname('.')).to_s

    cmd_options = [
        *(['-X+T', "-Xtruffle.core.load_path=#{core_load_path}"] unless @options[:run][:test]),
        (format(@options[:global][:debug_option], @options[:global][:debug_port]) if @options[:run][:debug]),
        ('-Xtruffle.exceptions.print_java=true' if @options[:run][:jexception]),
        '-r', "./#{@options[:global][:truffle_bundle_path]}/bundler/setup.rb",
        *@options[:run][:load_path].flat_map { |v| ['-I', v] },
        *@options[:run][:require].flat_map { |v| ['-r', v] }
    ].compact

    env            = {}
    env['JAVACMD'] = @options[:global][:graal_path] if @options[:run][:graal]

    cmd = [(env unless env.empty?),
           @options[:global][:jruby_truffle_path],
           *cmd_options,
           *rest
    ].compact

    execute_cmd(cmd, fail: false, print_always: true)
    exit $?.exitstatus
  end

  def subcommand_clean(rest)
    FileUtils.rm_rf @options[:global][:truffle_bundle_path]
  end

  def print_cmd(cmd, print_always)
    unless verbose? || print_always
      return cmd
    end

    print = if String === cmd
              cmd
            else
              cmd.map do |v|
                if Hash === v
                  v.map { |k, v| "#{k}=#{v}" }.join(' ')
                else
                  Shellwords.escape v
                end
              end.join(' ')
            end
    puts '$ ' + print
    return cmd
  end

  def execute_cmd(cmd, fail: true, print_always: false)
    result = system(*print_cmd(cmd, print_always))
  ensure
    raise 'command failed' if fail && !result
  end
end
