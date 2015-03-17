require 'mspec/helpers/tmp'
require 'fileutils'

# Lower-level output speccing mechanism for a single
# output stream. Unlike OutputMatcher which provides
# methods to capture the output, we actually replace
# the FD itself so that there is no reliance on a
# certain method being used.
class OutputToFDMatcher
  def initialize(expected, to)
    @to, @expected = to, expected

    case @to
    when STDOUT
      @to_name = "STDOUT"
    when STDERR
      @to_name = "STDERR"
    when IO
      @to_name = @to.object_id.to_s
    else
      raise ArgumentError, "#{@to.inspect} is not a supported output target"
    end
  end

  def matches?(block)
    old_to = @to.dup
    out = File.open(tmp("mspec_output_to_#{$$}_#{Time.now.to_i}"), 'w+')

    # Replacing with a file handle so that Readline etc. work
    @to.reopen out

    block.call

  ensure
    begin
      @to.reopen old_to

      out.rewind
      @actual = out.read

      case @expected
        when Regexp
          return !(@actual =~ @expected).nil?
        else
          return @actual == @expected
      end

    # Clean up
    ensure
      out.close unless out.closed?
      FileUtils.rm out.path
    end

    return true
  end

  def failure_message()
    ["Expected (#{@to_name}): #{@expected.inspect}\n",
     "#{'but got'.rjust(@to_name.length + 10)}: #{@actual.inspect}\nBacktrace"]
  end

  def negative_failure_message()
    ["Expected output (#{@to_name}) to NOT be:\n", @actual.inspect]
  end
end

class Object
  def output_to_fd(what, where = STDOUT)
    OutputToFDMatcher.new what, where
  end
end
