require 'java'

module JRuby
  module JRubyParser
    include_package 'org.jruby.parser'
  end

  module JRE
    include_package 'java.io'
    include_package 'java.util'
  end

  module Ablaf
    include_package 'org.ablaf.internal.common'
    include_package 'org.ablaf.lexer'
  end

  class Parser
    def Parser::parse(source, name = '', handler = Ablaf::NullErrorHandler.new)
      config = JRubyParser::RubyParserConfiguration.new()
      config.setBlockVariables(JRE::ArrayList.new())
      config.setLocalVariables(JRE::ArrayList.new())
      begin
        parser = JRubyParser::RubyParserPool.getInstance().borrowParser()
        parser.setErrorHandler(handler)
        parser.init(config)
        lexerSource = Ablaf::LexerFactory.getInstance().getSource(name, source)
        return parser.parse(lexerSource).getAST()
      ensure
        JRubyParser::RubyParserPool.getInstance().returnParser(parser)
      end
    end
  end
end
