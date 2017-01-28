require 'mspec/guards/feature'
require 'mspec/helpers/io'
require 'mspec/utils/deprecate'

class HaveDataMatcher
  def initialize(data, mode="rb:binary")
    @data = data
    @mode = mode
  end

  def matches?(name)
    @name = name

    if FeatureGuard.enabled? :encoding
      size = @data.bytesize
    else
      size = @data.size
    end

    @contents = File.open @name, fmode(@mode) do |f|
      f.read(size)
    end
    @contents == @data
  end

  def failure_message
    ["Expected #{@name}",
     "to have data #{@data.pretty_inspect}" +
     "but starts with #{@contents.pretty_inspect}"]
  end

  def negative_failure_message
    ["Expected #{@name}",
     "not to have data #{@data.pretty_inspect}"]
  end
end

class Object
  # Opens a file specified by the string the matcher is called on
  # and compares the +data+ passed to the matcher with the contents
  # of the file. Expects to match the first N bytes of the file
  # with +data+. For example, suppose @name is the name of a file:
  #
  #   @name.should have_data("123")
  #
  # passes if the file @name has "123" as the first 3 bytes. The
  # file can contain more bytes than +data+. The extra bytes do not
  # affect the result.
  def have_data(data, mode="rb:binary")
    MSpec.deprecate "have_data", "File.read or File.binread(file).should == data"
    HaveDataMatcher.new(data, mode)
  end
end
