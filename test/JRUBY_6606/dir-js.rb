# encoding: Shift_JIS

Dir.chdir(File.dirname __FILE__) do
  Dir.entries('res/あい').each{|n| p n unless n == '.' || n == '..'}
  Dir.foreach('res/あい') {|n| p n unless n == '.' || n == '..'}
end
