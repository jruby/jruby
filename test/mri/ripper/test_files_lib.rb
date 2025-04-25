require_relative 'assert_parse_files.rb'
class TestRipper::Generic
  # Modified for JRuby stdlib location
  %w[lib/ruby/stdlib].each do |dir|
    define_method("test_parse_files:#{dir}") do
      assert_parse_files(dir, "*.rb")
    end
    Dir["#{SRCDIR}/#{dir}/*/"].each do |dir|
      dir = dir[(SRCDIR.length+1)..-2]
      define_method("test_parse_files:#{dir}") do
        assert_parse_files(dir)
      end
    end
  end
end
