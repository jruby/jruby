def jflex(file)
  sh "#{JFLEX_BIN} #{file}"
end

def jay(name='JavaSignatureParser', skeleton='signature_skeleton.parser')
  sh "#{JAY_BIN} #{PARSER_DIR}/#{name}.y  < #{PARSER_DIR}/#{skeleton} | grep -v ^//t > #{PARSER_DIR}/#{name}.java"
end


namespace :parse do
  task :generate_java_signature_parser do
    jflex 'src/org/jruby/lexer/JavaSignatureLexer.flex'
    jay 'JavaSignatureParser', 'signature_skeleton.parser'
  end
end
