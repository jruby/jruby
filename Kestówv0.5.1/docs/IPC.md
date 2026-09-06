# IPC

`ipc/` — `Kestowv::Ipc`

Inter-process communication primitives. All four core primitives are module-level singletons with mutex-protected state — they model kernel IPC objects, not per-process objects.

---

## Pipe

`ipc/pipe.rb` — `Ipc::Pipe`

Byte-stream pipe with head-compaction. State is shared across the kernel; each pipe is identified by a hex string ID.

```ruby
id = Ipc::Pipe.create          # => "a3f2b1c4"
Ipc::Pipe.write(id, "data")
Ipc::Pipe.read(id)             # => "data"
Ipc::Pipe.close(id)            # frees @pipes[id] and @heads[id]
```

The backing store is a Ruby Array (`@pipes[id]`). `read` advances a head pointer (`@heads[id]`) rather than shifting the array (shift is O(n)). When the consumed prefix exceeds 32 entries and constitutes more than half the array, the prefix is compacted: `@pipes[id] = arr[(head+1)..]` and the head resets to 0. This is O(1) amortised.

`close(id)` deletes both the pipe array and the head pointer from their respective hashes. Calling `close` on completion is required — failure to close leaks both entries indefinitely. The stress test creates and closes ~1,600 pipes/second.

```ruby
Ipc::Pipe.stats   # => { feature: :ipc_pipe, pipes: <count> }
Ipc::Pipe.to_a    # => [id, id, ...]
```

---

## Queue

`ipc/queue.rb` — `Ipc::Queue`

Named FIFO message queue. Queues are created once and reused; they are not per-transaction objects.

```ruby
Ipc::Queue.create(:my_queue)
Ipc::Queue.enqueue(:my_queue, "message")
Ipc::Queue.dequeue(:my_queue)   # => "message"
Ipc::Queue.size(:my_queue)      # => 0
```

Backed by a Hash of Arrays, keyed by symbol. `dequeue` returns `nil` if the queue is empty. Thread-safe via a single module-level mutex.

---

## Semaphore

`ipc/sem.rb` — `Ipc::Sem`

Counting semaphore. Blocking `wait` is implemented with `Mutex` + `ConditionVariable`.

```ruby
Ipc::Sem.create(:my_sem, 1)   # initial count = 1
Ipc::Sem.wait(:my_sem)        # decrement; block if count == 0
Ipc::Sem.post(:my_sem)        # increment; wake blocked waiter
Ipc::Sem.value(:my_sem)       # => current count
```

`post` calls `signal` on the condition variable after incrementing, waking exactly one blocked `wait` call. The blocking behaviour makes this suitable for mutual exclusion (`create(key, 1)`) and producer/consumer signalling.

---

## Shared Memory

`ipc/shm.rb` — `Ipc::Shm`

Named shared memory segment registry. In the kernel model, "shared memory" is a named slot with a declared size; actual data sharing happens at the VmRegion level by mapping the same backing to multiple address spaces. The registry tracks existence and metadata.

```ruby
Ipc::Shm.create(:my_seg, 4096)   # register 4096-byte segment
Ipc::Shm.attach(:my_seg)          # => { size: 4096, created_at: ... }
Ipc::Shm.detach(:my_seg)          # detach (no-op at registry level)
Ipc::Shm.destroy(:my_seg)         # remove from registry
```

`destroy(key)` deletes the segment from `@segments`. Calling `destroy` when finished is required — the segment hash is the only reference; un-destroyed segments leak indefinitely.

```ruby
Ipc::Shm.stats   # => { feature: :ipc_shm, segments: <count> }
```

---

## Unix Domain Sockets

`net/unix_hub.rb` — `Net::UnixHub`
`net/unix_socket.rb` — `Net::UnixSocket`

The UDS layer is the primary IPC mechanism for cross-process (and cross-runtime) communication. It is also the mechanism the kernel uses to communicate with the CRuby Tk dashboard.

### UnixHub

A registry of all Unix domain sockets known to the kernel. Acts as the kernel's analogue of a socket file descriptor table, but at the system level rather than per-process.

```ruby
sock = Net::UnixSocket::Socket.new("/tmp/my.sock", :stream)
Net::UnixHub.register(sock)
Net::UnixHub.get("/tmp/my.sock")       # => sock
Net::UnixHub.unregister("/tmp/my.sock")
Net::UnixHub.list                       # => ["/tmp/my.sock", ...]
Net::UnixHub.stats                      # => { active_sockets:, backend: }
Net::UnixHub.active_sockets             # => count
```

### UnixSocket

```ruby
sock = Net::UnixSocket::Socket.new(path, :stream)
sock.connect
sock.send_io(fd, payload_hash)
sock.recv_io                     # => payload_hash
sock.close

s1, s2 = Net::UnixSocket.pair   # connected socket pair
Net::UnixSocket.close(path)     # close and unregister
```

`send_io` / `recv_io` use a simple queue internally; this is a simulation of the kernel socket send/receive path rather than a real `sendmsg`/`recvmsg`. The OS-level UDS used by the Tk dashboard is a standard `UNIXServer`/`UNIXSocket` pair managed directly in `boot/tk_stress.rb` and registered with `UnixHub` for kernel tracking.

---

## Additional IPC Primitives

The following are implemented and auto-registered but not exercised by the default stress test:

| Module | File | Purpose |
|---|---|---|
| `Ipc::Bus` | `ipc/bus.rb` | Publish/subscribe event bus |
| `Ipc::Channel` | `ipc/channel.rb` | Typed bidirectional channel |
| `Ipc::Msg` | `ipc/msg.rb` | Message struct |
| `Ipc::Namespace` | `ipc/namespace.rb` | IPC namespace isolation |
| `Ipc::RPC` | `ipc/rpc.rb` | Remote procedure call over IPC |
| `Ipc::Transport` | `ipc/transport.rb` | Transport abstraction |
| `Ipc::Discovery` | `ipc/discovery.rb` | Service discovery |
| `Ipc::Client` | `ipc/client.rb` | IPC client helper |
| `Ipc::Server` | `ipc/server.rb` | IPC server helper |
