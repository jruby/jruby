class ForInLambdaInEvalInDefineMethod
  def initialize
    code = eval(<<~RUBY)
          -> {
              for _ in [1]
                value = defined?(Array)
              end
              value
            }
          RUBY

    self.define_singleton_method( :bar, &code )
  end
end
