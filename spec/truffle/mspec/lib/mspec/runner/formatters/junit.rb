require 'mspec/expectations/expectations'
require 'mspec/utils/ruby_name'
require 'mspec/runner/formatters/yaml'

class JUnitFormatter < YamlFormatter
  def initialize(out=nil)
    super
    @tests = []
  end

  def after(state = nil)
    super
    @tests << {:test => state, :exception => false} unless exception?
  end

  def exception(exception)
    super
    @tests << {:test => exception, :exception => true}
  end

  def finish
    switch

    time = @timer.elapsed
    tests = @tally.counter.examples
    errors = @tally.counter.errors
    failures = @tally.counter.failures

    printf <<-XML

<?xml version="1.0" encoding="UTF-8" ?>
    <testsuites
        testCount="#{tests}"
        errorCount="#{errors}"
        failureCount="#{failures}"
        timeCount="#{time}" time="#{time}">
      <testsuite
          tests="#{tests}"
          errors="#{errors}"
          failures="#{failures}"
          time="#{time}"
          name="Spec Output For #{::RUBY_NAME} (#{::RUBY_VERSION})">
    XML
    @tests.each do |h|
      description = encode_for_xml h[:test].description

      printf <<-XML, "Spec", description, 0.0
        <testcase classname="%s" name="%s" time="%f">
      XML
      if h[:exception]
        outcome = h[:test].failure? ? "failure" : "error"
        message = encode_for_xml h[:test].message
        backtrace = encode_for_xml h[:test].backtrace
        print <<-XML
          <#{outcome} message="error in #{description}" type="#{outcome}">
            #{message}
            #{backtrace}
          </#{outcome}>
        XML
      end
      print <<-XML
        </testcase>
      XML
    end

    print <<-XML
      </testsuite>
    </testsuites>
    XML
  end

  private
  LT = "&lt;"
  GT = "&gt;"
  QU = "&quot;"
  AP = "&apos;"
  AM = "&amp;"
  TARGET_ENCODING = "ISO-8859-1"

  def encode_for_xml(str)
    encode_as_latin1(str).gsub("<", LT).gsub(">", GT).
      gsub('"', QU).gsub("'", AP).gsub("&", AM).
      gsub(/[#{Regexp.escape("\0\1\2\3\4\5\6\7\8")}]/, "?")
  end

  if defined? Encoding
    def encode_as_latin1(str)
      str.encode(TARGET_ENCODING, :undef => :replace, :invalid => :replace)
    end
  else
    require 'iconv'
    def encode_as_latin1(str)
      Iconv.conv("#{TARGET_ENCODING}//TRANSLIT//IGNORE", "UTF-8", str)
    end
  end
end
