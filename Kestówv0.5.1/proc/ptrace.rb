# frozen_string_literal: true

# Kestówv 0.5.1 — proc/ptrace.rb
#
# Process tracing / debugging support.
# Implements ptrace-style attach/detach with a per-tracee request queue.
# Requires CAP_SYS_PTRACE or same-uid relationship (enforced by caller).
#
# Tracer → Tracee relationship is 1:1 per tracee.
# One tracer can trace multiple tracees.

module Kestowv
  module Proc
    module Ptrace

      # --------------------------------------------------------
      # PTRACE REQUESTS
      # --------------------------------------------------------

      REQUESTS = %i[
        peek_data   # read word from tracee address space
        poke_data   # write word to tracee address space
        peek_regs   # read register state
        poke_regs   # write register state
        single_step # execute one instruction and stop
        cont        # resume execution
        kill        # send SIGKILL to tracee
        get_sigmask # get tracee signal mask
        set_sigmask # set tracee signal mask
      ].freeze

      # --------------------------------------------------------
      # TRACE SESSION
      # --------------------------------------------------------

      TraceSession = Struct.new(
        :tracer_pid,
        :tracee_pid,
        :attached_at,
        :request_log,
        :log_pos,
        keyword_init: true
      ) do
        LOG_CAP = 128

        def log_request(req, args, result)
          entry = { request: req, args: args, result: result, at: Time.now.freeze }
          if request_log.size < LOG_CAP
            request_log << entry
          else
            request_log[log_pos] = entry   # overwrite oldest — O(1)
          end
          self.log_pos = (log_pos + 1) % LOG_CAP
        end

        def recent_requests
          return request_log.dup if request_log.size < LOG_CAP
          pos = log_pos
          request_log[pos..] + request_log[0...pos]
        end

        def to_h
          {
            tracer_pid:  tracer_pid,
            tracee_pid:  tracee_pid,
            attached_at: attached_at,
            requests:    [request_log.size, LOG_CAP].min
          }
        end
      end

      # --------------------------------------------------------
      # REGISTRY
      # --------------------------------------------------------

      @sessions    = {}   # tracee_pid => TraceSession
      @tracer_map  = Hash.new { |h, k| h[k] = [] }  # tracer_pid => [tracee_pids]
      @mutex       = Mutex.new
      @stats       = Hash.new(0)

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_ptrace)
          Boot.set_bit(:proc_ptrace)
          self
        end

        # --------------------------------------------------------
        # ATTACH / DETACH
        # --------------------------------------------------------

        # Attach tracer_pid to tracee_pid.
        # Returns :ok or an error symbol.
        # Credential check (CAP_SYS_PTRACE) is the caller's responsibility.
        def attach(tracer_pid, tracee_pid)
          @mutex.synchronize do
            if @sessions.key?(tracee_pid)
              return :already_traced
            end

            tracee = Pid.task_for(tracee_pid)
            return :no_such_process unless tracee
            return :zombie          if tracee.zombie?

            session = TraceSession.new(
              tracer_pid:  tracer_pid,
              tracee_pid:  tracee_pid,
              attached_at: Time.now.freeze,
              request_log: [],
              log_pos:     0
            )

            @sessions[tracee_pid]         = session
            @tracer_map[tracer_pid]       << tracee_pid
            @stats[:attach] += 1
          end

          Boot.register(ptrace_bit(tracee_pid))
          Boot.set_bit(ptrace_bit(tracee_pid))

          warn "[Ptrace] #{tracer_pid} → #{tracee_pid}" unless Boot.config.quiet
          :ok
        end

        def detach(tracer_pid, tracee_pid)
          removed = @mutex.synchronize do
            sess = @sessions.delete(tracee_pid)
            return :not_traced unless sess
            return :wrong_tracer if sess.tracer_pid != tracer_pid

            @tracer_map[tracer_pid].delete(tracee_pid)
            @stats[:detach] += 1
            sess
          end

          Boot.clear_bit(ptrace_bit(tracee_pid))
          warn "[Ptrace] detached #{tracer_pid} ← #{tracee_pid}" unless Boot.config.quiet
          :ok
        end

        # Detach all tracees when a tracer exits.
        def detach_all(tracer_pid)
          tracees = @mutex.synchronize { @tracer_map.delete(tracer_pid)&.dup || [] }
          tracees.each { |pid| detach(tracer_pid, pid) }
          tracees.size
        end

        # --------------------------------------------------------
        # REQUEST DISPATCH
        # --------------------------------------------------------

        # Issue a ptrace request from tracer to tracee.
        # Returns { result:, error: } hash.
        def request(tracer_pid, tracee_pid, req, *args)
          unless REQUESTS.include?(req)
            return { result: nil, error: :unknown_request }
          end

          sess = @mutex.synchronize { @sessions[tracee_pid] }
          unless sess
            return { result: nil, error: :not_traced }
          end
          unless sess.tracer_pid == tracer_pid
            return { result: nil, error: :wrong_tracer }
          end

          result = dispatch_request(req, tracee_pid, args)
          sess.log_request(req, args, result)
          @mutex.synchronize { @stats[:requests] += 1 }

          { result: result, error: nil }
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def traced?(pid)
          @mutex.synchronize { @sessions.key?(pid) }
        end

        def tracer_for(pid)
          @mutex.synchronize { @sessions[pid]&.tracer_pid }
        end

        def tracees_for(tracer_pid)
          @mutex.synchronize { @tracer_map[tracer_pid].dup }
        end

        def session_for(tracee_pid)
          @mutex.synchronize { @sessions[tracee_pid] }
        end

        def to_a
          @mutex.synchronize { @sessions.values.map(&:to_h) }
        end

        def stats
          @mutex.synchronize do
            {
              feature:   :proc_ptrace,
              sessions:  @sessions.size,
              tracers:   @tracer_map.size,
              attach:    @stats[:attach],
              detach:    @stats[:detach],
              requests:  @stats[:requests]
            }
          end
        end

        private

        # --------------------------------------------------------
        # REQUEST IMPLEMENTATION STUBS
        # Fill in when Task register state + VmSpace are accessible.
        # --------------------------------------------------------

        def dispatch_request(req, tracee_pid, args)
          case req
          when :peek_data
            # TODO: read from tracee VmSpace at args[0] (address)
            nil
          when :poke_data
            # TODO: write args[1] to tracee VmSpace at args[0]
            nil
          when :peek_regs
            # TODO: return tracee register snapshot
            {}
          when :poke_regs
            # TODO: write args[0] (hash) into tracee registers
            nil
          when :cont
            task = Pid.task_for(tracee_pid)
            task&.transition(:running)
            :ok
          when :single_step
            # TODO: set single-step flag in tracee task
            :ok
          when :kill
            task = Pid.task_for(tracee_pid)
            task&.transition(:zombie)
            Wait.notify_exit(tracee_pid)
            :ok
          when :get_sigmask, :set_sigmask
            # TODO: implement when signal subsystem lands
            nil
          end
        end

        def ptrace_bit(pid)
          :"proc_ptrace_#{pid}"
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_ptrace,
  __FILE__,
  feature:    :proc_ptrace,
  depends_on: [:proc_pid, :proc_task, :proc_wait, :proc_credentials]
)
