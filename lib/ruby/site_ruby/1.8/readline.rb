$ReadlineLibrary = "GnuReadline"

require 'java'

begin

JReadline = org.gnu.readline.Readline
JReadlineLibrary = org.gnu.readline.ReadlineLibrary
JStack = java.util.Stack
JEOFException = java.io.EOFException

JReadline.load(JReadlineLibrary.byName($ReadlineLibrary))
JReadline.initReadline("Ruby")

module Readline
  def readline(prompt, add_to_hist)
    begin
      line = JReadline.readline(prompt, add_to_hist)
    rescue JEOFException
      return nil
    end
    if line.nil?
      return ''
    end
    line
  end
  module_function :readline
  HISTORY = Object.new
  class << HISTORY
    def push(line)
      JReadline.addToHistory(line)
    end
    def pop(); end
  end
end

rescue NameError
  raise LoadError.new("Missing libreadline-java library; see Readline-HOWTO.txt in docs")
end
