#
#   nop.rb -
#   	$Release Version: 0.9.6$
#   	$Revision: 23985 $
#   	by Keiju ISHITSUKA(keiju@ruby-lang.org)
#
# --
#
#
#
module IRB
  module ExtendCommand
    class Nop

      @RCS_ID='-$Id: nop.rb 23985 2009-07-07 11:36:20Z keiju $-'

      def self.execute(conf, *opts)
	command = new(conf)
	command.execute(*opts)
      end

      def initialize(conf)
	@irb_context = conf
      end

      attr_reader :irb_context

      def irb
	@irb_context.irb
      end

      def execute(*opts)
	#nop
      end
    end
  end
end

