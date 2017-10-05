# frozen_string_literal: true
#
# .rb part for JRuby's native TracePoint impl

class TracePoint

  def self.trace(*events, &blk)  # :nodoc:
    tp = new(*events, &blk)
    tp.enable
    tp
  end

end
