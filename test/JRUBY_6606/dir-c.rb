# encoding: GBK

Dir.chdir(File.dirname __FILE__) do
  Dir.entries('res/����').each{|n| p n unless n == '.' || n == '..'}
  Dir.foreach('res/����') {|n| p n unless n == '.' || n == '..'}
end
