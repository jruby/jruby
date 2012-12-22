# encoding: utf-8
require 'rspec'
require 'strscan'

describe "JRUBY-6668: StringScanner.scan" do
  it "must properly negotiate 1.9 encoding based on incoming string" do
    regex = /(^[ \t]*)?\{\{/
    text = "<h1>中文 test</h1>\n\n{{> utf8_partial}}\n"
    text.force_encoding 'BINARY' if RUBY_VERSION >= '1.9'
    scanner = StringScanner.new(text)
    scanner.scan_until(regex) # Fans spin up, and this method never returns.
  end
end