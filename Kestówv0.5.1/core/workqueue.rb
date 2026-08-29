# frozen_string_literal: true

# Kestówv 0.5.1 — core/workqueue.rb
#
# Work queue with priority and scheduler integration.

module Kestowv
  module Core
    module Workqueue

      @queue   = []
      @q_head  = 0       # index of next item to dequeue
      @running = false
      @mutex   = Mutex.new

      class << self

        def register_with_boot
          Boot.register(:core_workqueue)
          Boot.set_bit(:core_workqueue)
        end

        def enqueue(priority: :normal, &block)
          entry = { block: block, priority: priority, enqueued_at: ::Time.now }
          @mutex.synchronize { @queue << entry }
        end

        def run
          @running = true
          while @running
            entry = @mutex.synchronize do
              if @q_head < @queue.size
                item = @queue[@q_head]
                @queue[@q_head] = nil   # release reference
                @q_head += 1
                if @q_head > 64
                  @queue = @queue[@q_head..]
                  @q_head = 0
                end
                item
              end
            end

            if entry
              begin
                entry[:block].call
              rescue => e
                Core::Klog.error("Workqueue job failed", error: e.message)
              end
            else
              sleep 0.05
            end
          end
        end

        def stop
          @running = false
        end

        def to_a
          @mutex.synchronize do
            {
              queued:  @queue.size - @q_head,
              running: @running
            }
          end
        end

        def stats
          @mutex.synchronize do
            {
              feature: :core_workqueue,
              queued:  @queue.size - @q_head
            }
          end
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :core_workqueue,
  __FILE__,
  feature:    :core_workqueue,
  depends_on: [:core_klog]
)
