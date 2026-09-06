# frozen_string_literal: true

# Kestówv 0.5.1 — proc/signal_delivery.rb
#
# Signal delivery logic.
# Enforces PID namespace boundaries, consults the signal mask,
# and invokes handlers (Proc | :default | :ignore).
#
# NOTE: Handlers are invoked in kernel context — no userspace signal
# frames until a real scheduler context-switch is implemented.

module Kestowv
  module Proc
    module SignalDelivery

      @stats = Hash.new(0)
      @mutex = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_signal_delivery)
          Boot.set_bit(:proc_signal_delivery)
          self
        end

        # --------------------------------------------------------
        # POST — enqueue a signal onto a task
        # --------------------------------------------------------

        # Post a signal to a target task.
        # Returns :ok, :invalid, :namespace_denied, or :task_dead.
        def post(target_task, signo, sender_pid: 0, sender_uid: 0, reason: :kernel)
          unless Signal.valid?(signo)
            return :invalid
          end

          if target_task.zombie?
            return :task_dead
          end

          unless namespace_allowed?(sender_pid, target_task)
            @mutex.synchronize { @stats[:namespace_denied] += 1 }
            return :namespace_denied
          end

          info = Signal::SigInfo.new(
            signo:      signo,
            sender_pid: sender_pid,
            sender_uid: sender_uid,
            reason:     reason,
            sent_at:    Time.now.freeze
          )

          target_task.post_signal(info)
          @mutex.synchronize { @stats[:posted] += 1 }
          :ok
        end

        # Post to every task in a process group (for job control signals).
        def post_to_pgroup(pgid, signo, sender_pid: 0, reason: :user)
          pids = Session.pids_in_pgroup(pgid)
          results = pids.map do |pid|
            task = Pid.task_for(pid)
            next [:skip, pid] unless task
            [post(task, signo, sender_pid: sender_pid, reason: reason), pid]
          end
          results.compact
        end

        # --------------------------------------------------------
        # DELIVER_PENDING — called by scheduler on task resume
        # --------------------------------------------------------

        # Deliver all pending, unmasked signals to the task.
        # Must be called with the task in a stable state (not mid-transition).
        def deliver_pending(task)
          return if task.zombie?

          pending_mask = task.sig_pending
          block_mask   = task.sig_mask
          return if pending_mask == 0

          # Unblockable signals bypass the mask entirely
          unblockable_pending = pending_mask & Signal.mask(*Signal::UNBLOCKABLE)

          # Deliverable = (pending & ~blocked) | unblockable
          deliverable_mask = (pending_mask & ~block_mask) | unblockable_pending
          return if deliverable_mask == 0

          # Extract individual signal numbers and deliver in priority order
          Signal.from_mask(deliverable_mask).sort.each do |signo|
            deliver(task, signo)
            break if task.zombie?  # stop delivering if task was killed
          end
        end

        # --------------------------------------------------------
        # STATS
        # --------------------------------------------------------

        def stats
          @mutex.synchronize do
            {
              feature:           :proc_signal_delivery,
              posted:            @stats[:posted],
              delivered:         @stats[:delivered],
              ignored:           @stats[:ignored],
              namespace_denied:  @stats[:namespace_denied],
              handler_errors:    @stats[:handler_errors]
            }
          end
        end

        private

        # --------------------------------------------------------
        # NAMESPACE BOUNDARY CHECK
        # --------------------------------------------------------

        # sender_pid == 0 means kernel-generated — always allowed.
        # Otherwise both sender and target must share the same PID namespace.
        def namespace_allowed?(sender_pid, target_task)
          return true if sender_pid == 0

          sender_task = Pid.task_for(sender_pid)
          return false unless sender_task

          sender_ns_id = sender_task.ns_set&.ns_id(:pid)
          target_ns_id = target_task.ns_set&.ns_id(:pid)

          # nil ns_id = root namespace = sees everything
          return true if sender_ns_id.nil? || target_ns_id.nil?

          sender_ns_id == target_ns_id
        end

        # --------------------------------------------------------
        # DELIVER — single signal to a task
        # --------------------------------------------------------

        def deliver(task, signo)
          # Always check unblockable — mask cannot override
          unless Signal.unblockable?(signo)
            return if task.sig_pending & Signal.bit(signo) == 0
          end

          handler = task.sig_handlers[signo] || :default

          case handler
          when :ignore
            task.clear_signal(signo)
            @mutex.synchronize { @stats[:ignored] += 1 }

          when ::Proc
            # NOTE: invoked in kernel context — no signal frame.
            # Handler receives (signo, siginfo) — caller can inspect sender.
            info = task.dequeue_signal(signo)
            begin
              handler.call(signo, info)
            rescue => e
              @mutex.synchronize { @stats[:handler_errors] += 1 }
              Boot.handle_error(e, {
                signal:  Signal.name(signo),
                task_id: task.tid,
                reason:  info&.reason
              })
            end
            @mutex.synchronize { @stats[:delivered] += 1 }

          else
            # :default
            info = task.dequeue_signal(signo)
            apply_default_action(task, signo, info)
            @mutex.synchronize { @stats[:delivered] += 1 }
          end
        end

        # --------------------------------------------------------
        # DEFAULT ACTIONS
        # --------------------------------------------------------

        def apply_default_action(task, signo, info)
          action = Signal.default_action(signo)

          case action
          when :terminate
            terminate_task(task, signo, info)

          when :stop
            task.transition(:blocked)
            # Wake any ptrace waiter observing this task
            Ptrace.notify_stop(task.tid, signo) if defined?(Ptrace) && Ptrace.traced?(task.tid)

          when :continue
            task.transition(:ready) if task.blocked?
            # Deliver SIGCHLD to parent when continuing
            notify_parent_sigchld(task, :continued) if signo == Signal::SIGCONT

          when :ignore
            # Already handled above but DEFAULT_ACTIONS can specify :ignore
            task.clear_signal(signo)
          end
        end

        def terminate_task(task, signo, info)
          task.transition(:zombie)

          # Notify waiting parent via SIGCHLD + Wait.notify_exit
          notify_parent_sigchld(task, :killed)
          Wait.notify_exit(task.tid) if defined?(Wait)

          warn "[Signal] Task #{task.tid} killed by #{Signal.name(signo)} " \
               "(from=#{info&.sender_pid})" unless Boot.config.quiet
        end

        def notify_parent_sigchld(task, reason)
          parent_pid = Session.sid_for(task.tid)
          return unless parent_pid

          parent_task = Pid.task_for(parent_pid)
          return unless parent_task

          post(parent_task, Signal::SIGCHLD,
            sender_pid: task.tid,
            sender_uid: 0,
            reason:     reason
          )
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_signal_delivery,
  __FILE__,
  feature:    :signals,
  depends_on: [:core_signals, :proc_task, :proc_pid, :proc_wait, :proc_session, :proc_ptrace]
)
