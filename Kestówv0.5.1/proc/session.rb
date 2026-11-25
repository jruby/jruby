# frozen_string_literal: true

# Kestówv 0.5.1 — proc/session.rb
#
# Process session and process group management.
# Mirrors POSIX session/pgroup model:
#   Session (SID) → one or more Process Groups (PGID) → Tasks
# The session leader is the task whose PID == SID.
# The pgroup leader is the task whose PID == PGID.

module Kestowv
  module Proc
    module Session

      # --------------------------------------------------------
      # DATA STRUCTS
      # --------------------------------------------------------

      PGroup = Struct.new(:pgid, :sid, :members, :created_at, keyword_init: true) do
        def add(pid)    = members.add(pid)
        def remove(pid) = members.delete(pid)
        def empty?      = members.empty?
        def size        = members.size

        def to_h
          super.merge(members: members.to_a.sort)
        end
      end

      Sess = Struct.new(:sid, :leader_pid, :pgroups, :created_at, keyword_init: true) do
        def add_pgroup(pg)    = pgroups[pg.pgid] = pg
        def remove_pgroup(pgid) = pgroups.delete(pgid)
        def pgroup(pgid)      = pgroups[pgid]
        def pgroup_ids        = pgroups.keys.sort
        def size              = pgroups.values.sum(&:size)

        def to_h
          {
            sid:        sid,
            leader_pid: leader_pid,
            pgroups:    pgroups.transform_values(&:to_h),
            created_at: created_at,
            size:       size
          }
        end
      end

      # --------------------------------------------------------
      # REGISTRY
      # --------------------------------------------------------

      @sessions = {}   # sid  => Sess
      @pgroups  = {}   # pgid => PGroup (global index)
      @task_sid = {}   # pid  => sid
      @task_pgid = {}  # pid  => pgid
      @mutex    = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_session)
          Boot.set_bit(:proc_session)
          self
        end

        # --------------------------------------------------------
        # SESSION MANAGEMENT
        # --------------------------------------------------------

        # Create a new session with leader_pid as the SID.
        # Also creates the initial process group (PGID == SID).
        def create_session(leader_pid)
          @mutex.synchronize do
            raise ArgumentError, "PID #{leader_pid} already leads a session" if @sessions.key?(leader_pid)

            pg = PGroup.new(
              pgid:       leader_pid,
              sid:        leader_pid,
              members:    Set.new([leader_pid]),
              created_at: Time.now.freeze
            )

            sess = Sess.new(
              sid:        leader_pid,
              leader_pid: leader_pid,
              pgroups:    { leader_pid => pg },
              created_at: Time.now.freeze
            )

            @sessions[leader_pid]    = sess
            @pgroups[leader_pid]     = pg
            @task_sid[leader_pid]    = leader_pid
            @task_pgid[leader_pid]   = leader_pid
          end

          Boot.register(sess_bit(leader_pid))
          Boot.set_bit(sess_bit(leader_pid))
          get_session(leader_pid)
        end

        def destroy_session(sid)
          @mutex.synchronize do
            sess = @sessions.delete(sid)
            return false unless sess

            sess.pgroup_ids.each { |pgid| @pgroups.delete(pgid) }

            @task_sid.delete_if  { |_, s| s == sid }
            @task_pgid.delete_if { |pid, _| !@task_sid.key?(pid) }
          end

          Boot.clear_bit(sess_bit(sid))
          true
        end

        # --------------------------------------------------------
        # PROCESS GROUP MANAGEMENT
        # --------------------------------------------------------

        def create_pgroup(pgid, sid:)
          @mutex.synchronize do
            sess = @sessions[sid]
            return false unless sess

            pg = PGroup.new(
              pgid:       pgid,
              sid:        sid,
              members:    Set.new,
              created_at: Time.now.freeze
            )

            @pgroups[pgid] = pg
            sess.add_pgroup(pg)
          end
          true
        end

        def add_to_pgroup(pid, pgid)
          @mutex.synchronize do
            pg = @pgroups[pgid]
            return false unless pg

            # Remove from old pgroup
            old_pgid = @task_pgid[pid]
            @pgroups[old_pgid]&.remove(pid) if old_pgid && old_pgid != pgid

            pg.add(pid)
            @task_pgid[pid] = pgid
            true
          end
        end

        def remove_from_session(pid)
          @mutex.synchronize do
            pgid = @task_pgid.delete(pid)
            sid  = @task_sid.delete(pid)

            @pgroups[pgid]&.remove(pid) if pgid

            # Clean up empty pgroups (but not the session leader's)
            if pgid && pgid != sid
              pg = @pgroups[pgid]
              if pg&.empty?
                @pgroups.delete(pgid)
                @sessions[sid]&.remove_pgroup(pgid)
              end
            end
          end
        end

        # --------------------------------------------------------
        # SIGNALS TO GROUPS / SESSIONS
        # --------------------------------------------------------

        # Returns all PIDs in a process group.
        def pids_in_pgroup(pgid)
          @mutex.synchronize { @pgroups[pgid]&.members&.to_a&.dup || [] }
        end

        # Returns all PIDs in a session.
        def pids_in_session(sid)
          @mutex.synchronize do
            sess = @sessions[sid]
            return [] unless sess
            sess.pgroups.values.flat_map { |pg| pg.members.to_a }
          end
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def get_session(sid)
          @mutex.synchronize { @sessions[sid] }
        end

        def get_pgroup(pgid)
          @mutex.synchronize { @pgroups[pgid] }
        end

        def sid_for(pid)
          @mutex.synchronize { @task_sid[pid] }
        end

        def pgid_for(pid)
          @mutex.synchronize { @task_pgid[pid] }
        end

        def to_a
          @mutex.synchronize { @sessions.values.map(&:to_h) }
        end

        def stats
          @mutex.synchronize do
            {
              feature:   :proc_session,
              sessions:  @sessions.size,
              pgroups:   @pgroups.size,
              tasks:     @task_sid.size
            }
          end
        end

        private

        def sess_bit(sid)
          :"proc_session_#{sid}"
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_session,
  __FILE__,
  feature:    :proc_session,
  depends_on: [:proc_pid, :proc_task]
)
