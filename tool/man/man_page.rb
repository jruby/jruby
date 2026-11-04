# frozen_string_literal: true

# Generates the `jruby.1` man page
# that backs `jruby --help` (see `core/src/main/java/org/jruby/util/cli/OutputStrings.java`).
#
# Usage: bin/jruby tool/man/man_page.rb <output-file> [--date YYYY-MM-DD]

require 'date'
require 'erb'
require 'fileutils'

begin
  require 'ronn'
rescue LoadError => e
  abort "man: Ronn-NG is required; install it with `bin/jruby -S gem install ronn-ng -v 0.10.1` (#{e.message})"
end

module ManPage
  TEMPLATE = File.join(File.dirname(__FILE__), 'jruby.1.md.erb')
  MANUAL = 'JRuby Programmer\'s Reference Guide'
  ORGANIZATION = 'JRuby'

  # OutputStrings.CliOption on the Java side.
  CliOption = Struct.new(:flag, :description)

  # The documented switches, read live from the running JRuby runtime.
  def self.cli_options
    cli_runtime::BASIC_USAGE_OPTIONS.map { |o| CliOption.new(o.flag, o.description) }
  end

  # The command synopsis, read live from the running JRuby runtime.
  def self.cli_synopsis
    cli_runtime::BASIC_USAGE_SYNOPSIS
  end

  def self.cli_runtime
    begin
      require 'java'
      java_import 'org.jruby.util.cli.OutputStrings'
    rescue LoadError
      abort 'man: must run on JRuby (`bin/jruby tool/man/man_page.rb ...`) so the CLI strings can be read from the runtime'
    end
    unless OutputStrings.const_defined?(:BASIC_USAGE_OPTIONS)
      abort 'man: OutputStrings.BASIC_USAGE_OPTIONS not found; rebuild JRuby first (`./mvnw -ntp -pl core process-classes`)'
    end
    unless OutputStrings.const_defined?(:BASIC_USAGE_SYNOPSIS)
      abort 'man: OutputStrings.BASIC_USAGE_SYNOPSIS not found; rebuild JRuby first (`./mvnw -ntp -pl core process-classes`)'
    end
    OutputStrings
  end
  private_class_method :cli_runtime

  def self.resolve_date(date_arg)
    return Date.strptime(date_arg, '%Y-%m-%d') if date_arg

    epoch = ENV['SOURCE_DATE_EPOCH']
    if epoch && !epoch.strip.empty?
      begin
        return Time.at(Integer(epoch.strip)).utc.to_date
      rescue ArgumentError
        warn "WARNING: ignoring malformed SOURCE_DATE_EPOCH: #{epoch}"
      end
    end
    Time.now.utc.to_date
  end

  class Renderer
    def initialize(options, synopsis)
      @options = options
      @synopsis = synopsis
    end

    def render
      ERB.new(File.read(TEMPLATE, encoding: 'UTF-8'), trim_mode: '-').result(binding)
    end

    private

    attr_reader :options, :synopsis
  end

  def self.render(date, options, synopsis)
    markdown = Renderer.new(options, synopsis).render
    document = Ronn::Document.new(
      'jruby.1.ronn',
      date: date,
      manual: MANUAL,
      organization: ORGANIZATION
    ) { markdown }
    document.convert(:roff)
  end

  def self.write(output, date_arg: nil)
    text = render(resolve_date(date_arg), cli_options, cli_synopsis)
    FileUtils.mkdir_p(File.dirname(output))
    File.write(output, text, encoding: 'UTF-8')
    puts "Wrote man page to #{File.expand_path(output)}"
    output
  end

  def self.main(argv)
    args = argv.dup
    output = args.shift
    date_arg = nil
    until args.empty?
      flag = args.shift
      if flag == '--date' && !args.empty?
        date_arg = args.shift
      else
        warn "Unknown argument: #{flag}"
        exit 2
      end
    end
    if output.nil?
      warn 'Usage: man_page.rb <output-file> [--date YYYY-MM-DD]'
      exit 2
    end
    write(output, date_arg: date_arg)
  end
end

if $0 == __FILE__
  ManPage.main(ARGV)
end
