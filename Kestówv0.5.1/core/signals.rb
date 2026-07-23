# frozen_string_literal: true

# Kestówv 0.5.1 — core/signals.rb
#
# Signal constants, SigInfo, bitmask helpers, and validation.
# Real-time signals (SIGRTMIN..SIGRTMAX) deferred — RT_CAPABLE = false.
# Handler model: :default | :ignore | Proc

module Kestowv
  module Signal

    RT_CAPABLE = false   # RT signals not implemented — placeholder for SIGRTMIN..SIGRTMAX

    # --------------------------------------------------------
    # SIGNAL NUMBERS — POSIX subset + kernel extension
    # --------------------------------------------------------

    SIGHUP    =  1   # hangup
    SIGINT    =  2   # interrupt (Ctrl-C)
    SIGQUIT   =  3   # quit (Ctrl-\)
    SIGILL    =  4   # illegal instruction
    SIGTRAP   =  5   # trace/breakpoint trap (ptrace)
    SIGABRT   =  6   # abort
    SIGBUS    =  7   # bus error (bad memory access)
    SIGFPE    =  8   # floating point exception
    SIGKILL   =  9   # kill — unblockable, unignorable
    SIGUSR1   = 10   # user-defined 1
    SIGSEGV   = 11   # segmentation fault
    SIGUSR2   = 12   # user-defined 2
    SIGPIPE   = 13   # broken pipe
    SIGALRM   = 14   # alarm clock
    SIGTERM   = 15   # termination request
    SIGCHLD   = 17   # child stopped or exited
    SIGCONT   = 18   # continue if stopped
    SIGSTOP   = 19   # stop — unblockable
    SIGTSTP   = 20   # terminal stop (Ctrl-Z)
    SIGTTIN   = 21   # background read from tty
    SIGTTOU   = 22   # background write to tty
    SIGKERN   = 30   # internal kernel notification (custom)

    # --------------------------------------------------------
    # LOOKUP TABLES
    # --------------------------------------------------------

    NAMES = {
      SIGHUP  => :SIGHUP,   SIGINT  => :SIGINT,   SIGQUIT => :SIGQUIT,
      SIGILL  => :SIGILL,   SIGTRAP => :SIGTRAP,  SIGABRT => :SIGABRT,
      SIGBUS  => :SIGBUS,   SIGFPE  => :SIGFPE,   SIGKILL => :SIGKILL,
      SIGUSR1 => :SIGUSR1,  SIGSEGV => :SIGSEGV,  SIGUSR2 => :SIGUSR2,
      SIGPIPE => :SIGPIPE,  SIGALRM => :SIGALRM,  SIGTERM => :SIGTERM,
      SIGCHLD => :SIGCHLD,  SIGCONT => :SIGCONT,  SIGSTOP => :SIGSTOP,
      SIGTSTP => :SIGTSTP,  SIGTTIN => :SIGTTIN,  SIGTTOU => :SIGTTOU,
      SIGKERN => :SIGKERN
    }.freeze

    # Reverse: :SIGKILL => 9
    NUMBERS = NAMES.invert.freeze

    # Default kernel action when no handler is registered.
    # :terminate — kill the task
    # :stop      — stop the task (SIGSTOP-style)
    # :continue  — resume a stopped task
    # :ignore    — silently discard
    DEFAULT_ACTIONS = {
      SIGHUP  => :terminate, SIGINT  => :terminate, SIGQUIT => :terminate,
      SIGILL  => :terminate, SIGTRAP => :stop,      SIGABRT => :terminate,
      SIGBUS  => :terminate, SIGFPE  => :terminate, SIGKILL => :terminate,
      SIGUSR1 => :terminate, SIGSEGV => :terminate, SIGUSR2 => :terminate,
      SIGPIPE => :terminate, SIGALRM => :terminate, SIGTERM => :terminate,
      SIGCHLD => :ignore,    SIGCONT => :continue,  SIGSTOP => :stop,
      SIGTSTP => :stop,      SIGTTIN => :stop,      SIGTTOU => :stop,
      SIGKERN => :ignore
    }.freeze

    # Signals that cannot be blocked or caught — always force-delivered.
    UNBLOCKABLE = [SIGKILL, SIGSTOP].freeze

    # Signals that reset to :default on exec (POSIX requirement).
    RESET_ON_EXEC = (NAMES.keys - [SIGKILL, SIGSTOP]).freeze

    # --------------------------------------------------------
    # HELPERS
    # --------------------------------------------------------

    def self.valid?(sig)
      NAMES.key?(sig)
    end

    def self.name(sig)
      NAMES[sig]&.to_s || "SIG#{sig}"
    end

    def self.number(name)
      NUMBERS[name.to_sym]
    end

    def self.default_action(sig)
      DEFAULT_ACTIONS[sig] || :terminate
    end

    def self.unblockable?(sig)
      UNBLOCKABLE.include?(sig)
    end

    # Convert signal number to bitmask bit position.
    def self.bit(sig)
      1 << sig
    end

    # Build a bitmask from an array of signal numbers.
    def self.mask(*sigs)
      sigs.flatten.reduce(0) { |m, s| m | bit(s) }
    end

    # Extract signal numbers set in a bitmask.
    def self.from_mask(bitmask)
      NAMES.keys.select { |sig| bitmask & bit(sig) != 0 }
    end

    # Validate handler value — :default, :ignore, or Proc.
    def self.valid_handler?(handler)
      handler == :default || handler == :ignore || handler.is_a?(::Proc)
    end

    # --------------------------------------------------------
    # SigInfo — metadata carried with every delivered signal.
    # Equivalent to POSIX siginfo_t.
    # --------------------------------------------------------

    SigInfo = Struct.new(
      :signo,       # Integer — signal number
      :sender_pid,  # Integer — PID of sender (0 if kernel-generated)
      :sender_uid,  # Integer — UID of sender
      :reason,      # Symbol — :user, :kernel, :child, :ptrace, :fault
      :sent_at,     # Time — when the signal was posted
      keyword_init: true
    ) do
      def name
        Signal.name(signo)
      end

      def kernel?
        sender_pid == 0
      end

      def unblockable?
        Signal.unblockable?(signo)
      end

      def to_s
        "#<SigInfo #{name} from=#{sender_pid} reason=#{reason}>"
      end
    end

    # --------------------------------------------------------
    # SIGPROCMASK HOW constants
    # --------------------------------------------------------

    SIG_BLOCK   = :block    # add signals to mask
    SIG_UNBLOCK = :unblock  # remove signals from mask
    SIG_SETMASK = :setmask  # replace mask entirely

  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_signals,
  __FILE__,
  feature:    :signals,
  depends_on: [:core_kobject]
)
