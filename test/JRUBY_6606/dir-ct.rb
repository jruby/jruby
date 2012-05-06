# encoding: BIG5

Dir.chdir(File.dirname __FILE__) do
  Dir.entries('res/Ʀƨ').each{|n| p n unless n == '.' || n == '..'}
  Dir.foreach('res/Ʀƨ') {|n| p n unless n == '.' || n == '..'}
end
