# frozen_string_literal: true

# Kestówv 0.5.1 — core/scheduler.rb
#
# Cooperative/preemptive hybrid scheduler.
# - Cooperative now: tasks yield by calling Scheduler.yield_task
# - Preemptive hook stubbed: Scheduler.preempt(task) for timer ISR
# - Per-CPU RunQueues driven by Hal::Cpu topology
# - SignalDelivery.deliver_pending called on every task resume
# - Idle loop uses RunQueue's timeout-based wait — no busy polling

module Kestowv
  module Core
    module Scheduler

      POLICY_WEIGHTS = {
        fair:      1,
        realtime:  0,   # always first
        powersave: 2    # yield more aggressively
      }.freeze

      @run_queues  = {}
      @mutex       = Mutex.new
      @running     = false
      @policy      = :fair
      @idle_tasks  = {}   # cpu_id => idle Task
      @sched_bits  = {}   # tid    => pre-registered bit position (lazy)
      @idle_bits   = {}   # cpu_id => pre-registered bit position (eager)

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:core_scheduler)
          Boot.set_bit(:core_scheduler)
          init_run_queues
          self
        end

        def init_run_queues
          cpu_count = (Hal::Cpu.online_count rescue 1).clamp(1, 64)
          cpu_count.times do |id|
            @run_queues[id] = RunQueue.new(id)
            @idle_bits[id]  = Boot.register(:"cpu_idle_#{id}")
          end
          Boot.set_bit(:scheduler_queues_ready)
          self
        end

        # --------------------------------------------------------
        # POLICY
        # --------------------------------------------------------

        def policy=(p)
          raise ArgumentError, "Unknown policy: #{p}" unless POLICY_WEIGHTS.key?(p)
          @mutex.synchronize { @policy = p }
          Boot.set_bit(:"scheduler_policy_#{p}")
        end

        def policy
          @mutex.synchronize { @policy }
        end

        # --------------------------------------------------------
        # TASK MANAGEMENT
        # --------------------------------------------------------

        def enqueue(task, cpu_id: best_cpu)
          return false unless task.runnable?
          rq = run_queue(cpu_id)
          rq.enqueue(task)
          Boot.set_bit_raw(sched_bit_pos(task.tid))
          true
        end

        def block_task(task)
          find_queue_for(task)&.remove(task)
          task.transition(:blocked)
          Boot.clear_bit_raw(sched_bit_pos(task.tid))
        end

        def unblock_task(task, cpu_id: best_cpu, priority: false)
          return false unless task.blocked?
          task.transition(:ready)
          rq = run_queue(cpu_id)
          priority ? rq.prioritize(task) : rq.enqueue(task)
          Boot.set_bit_raw(sched_bit_pos(task.tid))
          true
        end

        # Cooperative yield — task relinquishes CPU voluntarily.
        def yield_task(task)
          rq = find_queue_for(task) || run_queue(best_cpu)
          task.transition(:ready)
          rq.enqueue(task)
          # In a real implementation, context-switch here.
          # For now, the schedule loop picks it up on the next iteration.
        end

        # Preemptive hook — called by timer ISR when quantum expires.
        # Stub until real timer interrupt is wired in hal/interrupts.rb.
        def preempt(task)
          return unless task.state == :running
          yield_task(task)
          Boot.set_bit(:scheduler_preempt_fired)
          # TODO: trigger context switch via KThread or Fiber
        end

        # --------------------------------------------------------
        # MAIN SCHEDULER LOOP
        # Runs on its own KThread per CPU.
        # --------------------------------------------------------

        def start(cpu_id: 0)
          @mutex.synchronize { @running = true }
          Boot.set_bit(:scheduler_running)

          loop do
            break unless @mutex.synchronize { @running }

            task = pick_next_task(cpu_id)

            if task.nil?
              run_idle(cpu_id)
              next
            end

            resume_task(task)
          end

          Boot.clear_bit(:scheduler_running)
        end

        def stop
          @mutex.synchronize { @running = false }
          # Signal all queues so their dequeue unblocks
          @run_queues.each_value { |rq| rq.enqueue(nil) rescue nil }
        end

        def running?
          @mutex.synchronize { @running }
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def run_queue(cpu_id = 0)
          @mutex.synchronize { @run_queues[cpu_id] ||= RunQueue.new(cpu_id) }
        end

        def queue_depths
          @run_queues.transform_values(&:size)
        end

        def stats
          @mutex.synchronize do
            {
              feature:  :core_scheduler,
              running:  @running,
              policy:   @policy,
              cpus:     @run_queues.size,
              depths:   @run_queues.transform_values(&:size)
            }
          end
        end

        def to_s
          "#<Scheduler policy=#{policy} cpus=#{@run_queues.size} running=#{running?}>"
        end

        private

        # --------------------------------------------------------
        # TASK SELECTION — policy-aware
        # --------------------------------------------------------

        def pick_next_task(cpu_id)
          rq   = run_queue(cpu_id)
          task = rq.dequeue   # returns nil after IDLE_TIMEOUT

          return nil unless task
          return nil unless task.runnable?

          # Policy: realtime tasks skip the weight delay
          if @policy == :powersave && task.runnable?
            # powersave: yield sooner — re-enqueue after one step
            # (placeholder for actual time-slice accounting)
          end

          task
        end

        # --------------------------------------------------------
        # TASK RESUME — "return to userspace" point
        # --------------------------------------------------------

        def resume_task(task)
          task.transition(:running)

          # === Signal delivery checkpoint ===
          # This is the architectural "return to userspace" point.
          # Every task resume checks for pending signals.
          SignalDelivery.deliver_pending(task) if defined?(SignalDelivery)

          # TODO: real context switch — KThread/Fiber handoff here
          # For cooperative model, task runs until it calls yield_task or block_task.
          # For preemptive, timer ISR will call preempt(task).

          # After execution, task should have transitioned itself.
          # If still :running (shouldn't happen in coop), yield it.
          yield_task(task) if task.state == :running
        end

        # --------------------------------------------------------
        # IDLE
        # --------------------------------------------------------

        def run_idle(cpu_id)
          # Idle loop — called when no runnable task is available.
          # RunQueue#dequeue already waited IDLE_TIMEOUT before returning nil.
          # Here we do any background housekeeping.
          idle_pos = @idle_bits[cpu_id] || Boot.register(:"cpu_idle_#{cpu_id}")
          Boot.set_bit_raw(idle_pos)

          # Reap any zombies that wait.rb hasn't collected
          reap_zombies

          Boot.clear_bit_raw(idle_pos)
        end

        def reap_zombies
          return unless defined?(Proc::Pid)
          Proc::Pid.all_pids.each do |pid|
            task = Proc::Pid.task_for(pid)
            next unless task&.zombie?
            Proc::Wait.notify_exit(pid) if defined?(Proc::Wait)
          end
        end

        # --------------------------------------------------------
        # CPU SELECTION — simple load balancing
        # --------------------------------------------------------

        def best_cpu
          return 0 if @run_queues.size == 1
          @run_queues.min_by { |_, rq| rq.size }&.first || 0
        end

        def find_queue_for(task)
          @run_queues.values.find { |rq| rq.include?(task) }
        end

        # Lazy-register a bit position for a task's scheduler slot.
        # Boot.register is idempotent, so a benign double-call on first
        # concurrent enqueue of the same TID is safe.
        def sched_bit_pos(tid)
          @sched_bits[tid] ||= Boot.register(:"sched_task_#{tid}")
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :core_scheduler,
  __FILE__,
  feature:    :scheduler,
  depends_on: [:core_run_queue, :proc_task, :core_signals, :hal_cpu]
)
