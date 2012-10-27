files = File.join(File.dirname(__FILE__), (RUBY_VERSION >= '1.9.0' ? '1.9' : '1.8'), 'test_*.rb')
Dir.glob(files).sort.each do |tc|
  require tc
end
