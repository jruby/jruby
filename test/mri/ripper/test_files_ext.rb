require_relative 'assert_parse_files.rb'
class TestRipper::Generic
  # Modified for JRuby internal Ruby sources, roughly equivalent to ext
  %w[core/src/main/ruby].each do |dir|
    define_method("test_parse_files:#{dir}") do
      assert_parse_files(dir)
    end
  end
end
