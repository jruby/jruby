require 'rspec'

# https://github.com/jruby/jruby/issues/9623
#
# When the String given to Time.new ends exactly at the byte where the parser
# expects a ':', building the ArgumentError message read one byte past the end
# of the string's byte array and an ArrayIndexOutOfBoundsException escaped in
# place of the ArgumentError.
describe 'Time.new with a String that ends where a colon is expected' do
  it 'raises ArgumentError when the sec part is missing' do
    expect { Time.new("2020-12-25 00:56") }.to raise_error(ArgumentError, "missing sec part: 00:56")
  end

  it 'raises ArgumentError when the sec part is missing after a T separator' do
    expect { Time.new("2020-12-25T00:56") }.to raise_error(ArgumentError, "missing sec part: 00:56")
  end

  it 'raises ArgumentError when the min part is missing' do
    expect { Time.new("2020-12-25 00") }.to raise_error(ArgumentError, "missing min part: 00")
  end

  # CRuby formats these messages with "%.*s" and a length of one past the
  # consumed region, so the byte that failed the check is part of the message.
  # That precision stops at the string's NUL terminator, which is why the byte
  # is absent only when the string ends there. These four inputs do not end
  # there and must keep the trailing byte.
  it 'keeps the offending byte in the message when the string does not end there' do
    expect { Time.new("2020-12-25 00:56 +09:00") }.to raise_error(ArgumentError, "missing sec part: 00:56 ")
    expect { Time.new("2020-12-25 00 +09:00") }.to raise_error(ArgumentError, "missing min part: 00 ")
    expect { Time.new("2020-12-25 00. +0900") }.to raise_error(ArgumentError, "fraction hour is not supported: 00.")
    expect { Time.new("2020-12-25 00:56. +0900") }.to raise_error(ArgumentError, "fraction min is not supported: 00:56.")
  end
end
