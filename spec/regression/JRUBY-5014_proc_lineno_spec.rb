# JRUBY-5014
describe 'Lineno for proc' do

  class Proc
    def lineno
      inspect[/(\d+)\>$/,1].to_i
    end
  end

  if RUBY_VERSION =~ /1\.8/
    describe 'when within parenthesis' do
      it 'should reflect line where proc is declared' do
        ( # Nope, JRuby takes this line as Proc#lineno !!
            lambda { }
        ).lineno.should == (__LINE__ - 1)
      end
    end

    describe 'when within hash' do
      it 'should reflect line where proc is declared' do
        {
          __LINE__ => # Nope, JRuby takes this line as Proc#lineno !!
            lambda { }
        }.values.first.lineno.should == (__LINE__ - 1)
      end
    end

    describe 'when within array' do
      it 'should reflect line where proc is declared' do
        [ # Nope, JRuby takes this line as Proc#lineno !!
          lambda { }
        ].first.lineno.should == (__LINE__ - 1)
      end
    end

    describe 'when doing block dispatch' do

      def dispatch(*args, &block)
        case (arg = args.first)
        when Hash  then arg.values.first
        when Array then arg.first
        when Proc then arg
        else (block_given? && yield.nil?) ? block : yield
        end
      end

      it 'should reflect line where proc is declared (with array arg)' do
        dispatch([ # Nope, JRuby takes this line as Proc#lineno !!
          lambda {}
        ]).lineno.should == (__LINE__ - 1)
      end

      it 'should reflect line where proc is declared (with hash arg)' do
        dispatch({ # Nope, JRuby takes this line as Proc#lineno !!
          __LINE__ =>
            lambda {}
        }).lineno.should == (__LINE__ - 1)
      end

      it 'should reflect line when proc is declared (with proc arg)' do
        dispatch(( # Nope, JRuby takes this line as Proc#lineno !!
          lambda {}
        )).lineno.should == (__LINE__ - 1)
      end

      it 'should reflect line of attached code block' do
        # Yup, this is working as expected
        dispatch {
          # blah
        }.lineno.should == (__LINE__ - 2)
      end

      it "should reflect line of attached code block's returned proc" do
        # Yup, this is working as expected
        dispatch {
          lambda {}
        }.lineno.should == (__LINE__ - 1)
      end

    end

  end

end
