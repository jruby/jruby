# frozen_string_literal: true

# Generates the `jruby.1` man page
# that backs `jruby --help` (see `core/src/main/java/org/jruby/util/cli/OutputStrings.java`).
#
# Usage: bin/jruby tool/man/man_page.rb <output-file> [--date YYYY-MM-DD]

require 'date'
require 'erb'
require 'fileutils'

module ManPage
  TEMPLATE = File.join(File.dirname(__FILE__), 'jruby.1.erb')
  WRAP_COLUMN = 76

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

  def self.display_date(date)
    date.strftime('%B %-d, %Y')
  end

  # Escape text for man output: backslashes, dashes, and leading `.`/`'`.
  def self.man_escape(text)
    escaped = text.gsub('\\') { '\\\\' }.gsub('-') { '\\-' }
    escaped = "\\&#{escaped}" if escaped.start_with?('.', "'")
    escaped
  end

  # Greedy word-wrap matching the historical man page layout.
  def self.man_wrap(text)
    out = +''
    length = 0
    text.split(/\s+/).each do |word|
      if length > 0 && length + 1 + word.length > WRAP_COLUMN
        out << "\n"
        length = 0
      elsif length > 0
        out << ' '
        length += 1
      end
      out << word
      length += word.length
    end
    out
  end

  class Renderer
    def initialize(display_date, options, synopsis)
      @display_date = display_date
      @options = options
      @synopsis = synopsis
    end

    def render
      ERB.new(File.read(TEMPLATE, encoding: 'UTF-8'), trim_mode: '-').result(binding)
    end

    private

    attr_reader :display_date, :options, :synopsis

    def synopsis_cmd
      synopsis.split(' ', 2).first
    end

    def synopsis_rest
      synopsis.split(' ', 2).last
    end

    def man_escape(text)
      ManPage.man_escape(text)
    end

    def man_wrap(text)
      ManPage.man_wrap(text)
    end
  end

  def self.render(display_date, options, synopsis)
    Renderer.new(display_date, options, synopsis).render
  end

  def self.write(output, date_arg: nil)
    text = render(display_date(resolve_date(date_arg)), cli_options, cli_synopsis)
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
