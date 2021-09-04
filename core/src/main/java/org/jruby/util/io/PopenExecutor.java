package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.constants.platform.RLIMIT;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.posix.SpawnAttribute;
import jnr.posix.SpawnFileAction;
import org.jcodings.transcode.EConvFlags;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyProcess;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.API;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jruby.RubyString.newString;

/**
 * Port of MRI's popen+exec logic.
 */
public class PopenExecutor {

    public static final int SH_CHDIR_ARG_COUNT = 5;

    /**
     * Check properties and runtime state to determine whether a native popen is possible.
     *
     * @param runtime current runtime
     * @return true if popen can use native code, false otherwise
     */
    public static boolean nativePopenAvailable(Ruby runtime) {
        return Options.NATIVE_POPEN.load() && runtime.getPosix().isNative() && !Platform.IS_WINDOWS;
    }

    // MRI: check_pipe_command
    public static IRubyObject checkPipeCommand(ThreadContext context, IRubyObject filenameOrCommand) {
        RubyString filenameStr = filenameOrCommand.convertToString();
        ByteList filenameByteList = filenameStr.getByteList();
        final int[] chlen = {0};

        if (EncodingUtils.encAscget(
                filenameByteList.getUnsafeBytes(),
                filenameByteList.getBegin(),
                filenameByteList.getBegin() + filenameByteList.getRealSize(),
                chlen,
                filenameByteList.getEncoding()) == '|') {
            return filenameStr.makeShared(context.runtime, chlen[0], filenameByteList.length() - 1).infectBy(filenameOrCommand);
        }
        return context.nil;
    }

    // MRI: rb_f_spawn
    public static RubyFixnum spawn(ThreadContext context, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        long pid;
        String[] errmsg = { null };
        ExecArg eargp;
        IRubyObject fail_str;

        eargp = execargNew(context, argv, true, false);
        execargFixup(context, runtime, eargp);
        fail_str = eargp.use_shell ? eargp.command_name : eargp.command_name;

        PopenExecutor executor = new PopenExecutor();
        pid = executor.spawnProcess(context, runtime, eargp, errmsg);

        if (pid == -1) {
            if (errmsg[0] == null) {
                throw runtime.newErrnoFromErrno(executor.errno, fail_str.toString());
            }
            throw runtime.newErrnoFromErrno(executor.errno, errmsg[0]);
        }
        return runtime.newFixnum(pid);
    }

    // MRI: rb_f_system
    public IRubyObject systemInternal(ThreadContext context, IRubyObject[] argv, String[] errmsg) {
        Ruby runtime = context.runtime;

        ExecArg eargp;
        long pid;

        eargp = execargNew(context, argv, true, true);
        execargFixup(context, runtime, eargp);
        pid = spawnProcess(context, runtime, eargp, errmsg);

//            #if defined(HAVE_FORK) || defined(HAVE_SPAWNV)
        if (pid > 0) {
            long ret;
            ret = RubyProcess.waitpid(runtime, pid, 0);
            if (ret == -1)
                throw runtime.newErrnoFromInt(runtime.getPosix().errno(), "Another thread waited the process started by system().");
        }
//            #endif
//            #ifdef SIGCHLD
//            signal(SIGCHLD, chfunc);
//            #endif

        if (pid < 0) {
            if (eargp.exception) {
                int err = errno.intValue();
                RubyString command = eargp.command_name;
                throw runtime.newErrnoFromInt(err, command.toString());
            } else {
                return context.nil;
            }
        }

        int status = (int)((RubyProcess.RubyStatus) context.getLastExitStatus()).getStatus();

        if (status == 0) return runtime.getTrue();

        if (eargp.exception) {
            throw runtime.newRuntimeError(RubyProcess.RubyStatus.pst_message("Command failed with", pid, status));
        }

        return runtime.getFalse();
    }

    // MRI: rb_spawn_process
    long spawnProcess(ThreadContext context, Ruby runtime, ExecArg eargp, String[] errmsg) {
        long pid;
        RubyString prog;
        ExecArg sarg = new ExecArg();

        prog = eargp.use_shell ? eargp.command_name : eargp.command_name;

        if (eargp.chdirGiven) {
            // we can'd do chdir with posix_spawn, so we should be set to use_shell and now
            // just need to add chdir to the cmd
            String script = "cd '" + eargp.chdir_dir + "'; ";

            // use exec to eliminate extra sh process if we do not need to run command as a shell script
            if (!searchForMetaChars(prog)) {
                script = script + "exec ";
            }

            prog = (RubyString)prog.strDup(runtime).prepend(context, newString(runtime, script));
            eargp.chdir_dir = null;
            eargp.chdirGiven = false;
        }

        if (execargRunOptions(context, runtime, eargp, sarg, errmsg) < 0) {
            return -1;
        }

        if (prog != null && !eargp.use_shell) {
            // we handle argv[0] juggling in the spawn logic below
//            String[] argv = eargp.argv_str.argv;
//            if (argv.length > 0) {
//                argv[0] = prog.toString();
//            }
        }
        if (eargp.use_shell) {
            pid = procSpawnSh(runtime, prog.toString(), eargp);
        }
        else {
            String[] argv = eargp.argv_str.argv;
            pid = procSpawnCmd(runtime, argv, prog.toString(), eargp);
        }
        if (pid == -1) {
            context.setLastExitStatus(new RubyProcess.RubyStatus(runtime, runtime.getProcStatus(), 0x7f << 8, 0));
            if (errno == null || errno == Errno.__UNKNOWN_CONSTANT__) {
                errno = Errno.valueOf(runtime.getPosix().errno());
            }
        }

        execargRunOptions(context, runtime, sarg, null, errmsg);

        return pid;
    }

    // TODO: win32
//    #if defined(_WIN32)
//    #define proc_spawn_cmd_internal(argv, prog) rb_w32_uaspawn(P_NOWAIT, (prog), (argv))
//            #else
    long procSpawnCmdInternal(Ruby runtime, String[] argv, String prog, ExecArg eargp) {
        long status;

        if (prog == null)
            prog = argv[0];
        prog = dlnFindExeR(runtime, prog, eargp.path_env);
        if (prog == null) {
            errno = Errno.ENOENT;
            return -1;
        }

//        System.out.println(Arrays.asList(prog,
//                eargp.fileActions,
//                eargp.attributes,
//                Arrays.asList(argv),
//                eargp.envp_str == null ? Collections.EMPTY_LIST : Arrays.asList(eargp.envp_str)));
        // MRI does not do this check, but posix_spawn does not reliably ENOENT for bad filenames like ''
        if (prog == null || prog.length() == 0) {
            errno = Errno.ENOENT;
            return -1;
        }

        status = runtime.getPosix().posix_spawnp(
                prog,
                eargp.fileActions,
                eargp.attributes,
                Arrays.asList(argv),
                eargp.envp_str == null ? Collections.EMPTY_LIST : Arrays.asList(eargp.envp_str));
        if (status == -1) {
            if (runtime.getPosix().errno() == Errno.ENOEXEC.intValue()) {
                //String[] newArgv = new String[argv.length + 1];
                //newArgv[1] = prog;
                //newArgv[0] = "sh";
                status = runtime.getPosix().posix_spawnp(
                        "/bin/sh",
                        eargp.fileActions,
                        eargp.attributes,
                        Arrays.asList(argv),
                        eargp.envp_str == null ? Collections.EMPTY_LIST : Arrays.asList(eargp.envp_str));
                if (status == -1) errno = Errno.ENOEXEC;
            } else {
                errno = Errno.valueOf(runtime.getPosix().errno());
            }
        }
        return status;
    }


    long procSpawnCmd(Ruby runtime, String[] argv, String prog, ExecArg eargp) {
        long pid = -1;

        if (argv.length > 0 && argv[0] != null) {
            // TODO: win32
            if (Platform.IS_WINDOWS) {
                long flags = 0;
                if (eargp.newPgroupGiven && eargp.newPgroupFlag) {
//                    flags = CREATE_NEW_PROCESS_GROUP;
                }
//                pid = rb_w32_uaspawn_flags(P_NOWAIT, prog ? RSTRING_PTR(prog) : 0, argv, flags);
            }
            pid = procSpawnCmdInternal(runtime, argv, prog, eargp);
        }
        return pid;
    }

    // TODO: win32 version
//    #if defined(_WIN32)
//    #define proc_spawn_sh(str) rb_w32_uspawn(P_NOWAIT, (str), 0)
//            #else
    long procSpawnSh(Ruby runtime, String str, ExecArg eargp) {
        long status;

        String shell = dlnFindExeR(runtime, "sh", eargp.path_env);

//        System.out.println("before: " + shell + ", fa=" + eargp.fileActions + ", a=" + eargp.attributes + ", argv=" + Arrays.asList("sh", "-c", str));
        status = runtime.getPosix().posix_spawnp(
                shell != null ? shell : "/bin/sh",
                eargp.fileActions,
                eargp.attributes,
                Arrays.asList("sh", "-c", str),
                eargp.envp_str == null ? Collections.EMPTY_LIST : Arrays.asList(eargp.envp_str));

        if (status == -1) errno = Errno.valueOf(runtime.getPosix().errno());

        return status;
    }

    // pipe_open_s
    public static IRubyObject pipeOpen(ThreadContext context, IRubyObject prog, String modestr, int fmode, IOEncodable convconfig) {
        IRubyObject[] argv = {prog};
        ExecArg execArg = null;

        if (!isPopenFork(context.runtime, (RubyString)prog))
            execArg = execargNew(context, argv, true, false);
        return new PopenExecutor().pipeOpen(context, execArg, modestr, fmode, convconfig);
    }

    // rb_io_s_popen
    public static IRubyObject popen(ThreadContext context, IRubyObject[] argv, RubyClass klass, Block block) {
        Ruby runtime = context.runtime;
        String modestr;
        IRubyObject pname, port, tmp, opt = context.nil, env = context.nil;
        Object pmode = EncodingUtils.vmodeVperm(null, null);
        ExecArg eargp;
        int[] oflags_p = {0}, fmode_p = {0};
        IOEncodable.ConvConfig convconfig = new IOEncodable.ConvConfig();
        int argc = argv.length;

        if (argc > 1 && !(opt = TypeConverter.checkHashType(runtime, argv[argc - 1])).isNil()) --argc;
        if (argc > 1 && !(env = TypeConverter.checkHashType(runtime, argv[0])).isNil()) {
            --argc;
            argv = Arrays.copyOfRange(argv, 1, argc + 1);
        }
        switch (argc) {
            case 2:
                EncodingUtils.vmode(pmode, argv[1]);
            case 1:
                pname = argv[0];
                break;
            default: {
                int ex = opt.isNil() ? 0 : 1;
                Arity.raiseArgumentError(runtime, argc + ex, 1 + ex, 2 + ex);
                return null; // not reached
            }
        }

        tmp = TypeConverter.checkArrayType(runtime, pname);
        if (!tmp.isNil()) {
//            int len = ((RubyArray)tmp).size();
//            #if SIZEOF_LONG > SIZEOF_INT
//            if (len > INT_MAX) {
//                throw runtime.newArgumentError("too many arguments");
//            }
//            #endif
            tmp = ((RubyArray)tmp).aryDup();
            //            RBASIC_CLEAR_CLASS(tmp);
            eargp = execargNew(context, ((RubyArray)tmp).toJavaArray(), false, false);
            ((RubyArray)tmp).clear();
        } else {
            pname = pname.convertToString();
            eargp = null;
            if (!isPopenFork(runtime, (RubyString)pname)) {
                IRubyObject[] pname_p = {pname};
                eargp = execargNew(context, pname_p, true, false);
                pname = pname_p[0];
            }
        }
        if (eargp != null) {
            if (!opt.isNil())
                opt = execargExtractOptions(context, runtime, eargp, (RubyHash)opt);
            if (!env.isNil())
                execargSetenv(context, runtime, eargp, env);
        }
        EncodingUtils.extractModeEncoding(context, convconfig, pmode, opt, oflags_p, fmode_p);
        modestr = OpenFile.ioOflagsModestr(runtime, oflags_p[0]);

        port = new PopenExecutor().pipeOpen(context, eargp, modestr, fmode_p[0], convconfig);
//        This is cleanup for failure to exec in the child.
//        if (port.isNil()) {
//            /* child */
//            if (rb_block_given_p()) {
//                rb_yield(Qnil);
//                rb_io_flush(rb_stdout);
//                rb_io_flush(rb_stderr);
//                _exit(0);
//            }
//            return Qnil;
//        }
        ((RubyBasicObject)port).setMetaClass(klass);
        return RubyIO.ensureYieldClose(context, port, block);
    }

    static void execargSetenv(ThreadContext context, Ruby runtime, ExecArg eargp, IRubyObject env) {
        eargp.env_modification = !env.isNil() ? checkExecEnv(context, (RubyHash)env, eargp) : null;
    }

    // MRI: rb_check_exec_env
    public static RubyArray checkExecEnv(ThreadContext context, RubyHash hash, ExecArg pathArg) {
        Ruby runtime = context.runtime;
        RubyArray env;

        env = runtime.newArray();
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)hash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();
            String k;

            RubyString keyString = StringSupport.checkEmbeddedNulls(runtime, key).export(context);

            k = keyString.toString();

            if (k.indexOf('=') != -1)
                throw runtime.newArgumentError("environment name contains a equal : " + k);

            if (!val.isNil()) {
                val = StringSupport.checkEmbeddedNulls(runtime, val);
            }

            if (!val.isNil()) {
                val = ((RubyString) val).export(context);
            }

            if (k.equalsIgnoreCase("PATH")) {
                pathArg.path_env = val;
            }

            env.push(runtime.newArray(keyString, val));
        }

        return env;
    }

    // MRI: execarg_extract_options
    static IRubyObject execargExtractOptions(ThreadContext context, Ruby runtime, ExecArg eargp, RubyHash opthash) {
        return handleOptionsCommon(context, runtime, eargp, opthash, false);
    }

    // MRI: check_exec_options
    static void checkExecOptions(ThreadContext context, Ruby runtime, RubyHash opthash, ExecArg eargp) {
        handleOptionsCommon(context, runtime, eargp, opthash, true);
    }

    static IRubyObject handleOptionsCommon(ThreadContext context, Ruby runtime, ExecArg eargp, RubyHash opthash, boolean raise) {
        if (opthash.isEmpty())
            return null;

        RubyHash nonopts = null;

        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)opthash.directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();

            if (execargAddopt(context, runtime, eargp, key, val) != ST_CONTINUE) {
                if (raise) {
                    if (key instanceof RubySymbol) {
                        switch (key.toString()) {
                            case "gid" :
                                throw runtime.newNotImplementedError("popen does not support :gid option in JRuby");
                            case "uid" :
                                throw runtime.newNotImplementedError("popen does not support :uid option in JRuby");
                            default :
                                throw runtime.newArgumentError("wrong exec option symbol: " + key);
                        }
                    }
                    else {
                        throw runtime.newArgumentError("wrong exec option: " + key);
                    }
                }

                if (nonopts == null) nonopts = RubyHash.newHash(runtime);
                nonopts.op_aset(context, key, val);
            }
        }
        return nonopts != null ? nonopts : context.nil;
    }

    // MRI: is_popen_fork
    static boolean isPopenFork(Ruby runtime, RubyString prog) {
        if (prog.size() == 1 && prog.getByteList().get(0) == '-') {
            throw runtime.newNotImplementedError("fork() function is unimplemented on JRuby");
        }
        return false;
    }

    // MRI: DO_SPAWN macro in pipe_open
    private long DO_SPAWN(Ruby runtime, ExecArg eargp, String cmd, String[] args, String[] envp) {
        if (eargp.use_shell) {
            return procSpawnSh(runtime, eargp, cmd, envp);
        }

//        System.out.println(Arrays.asList(
//                cmd,
//                eargp.fileActions,
//                eargp.attributes,
//                args == null ? Collections.EMPTY_LIST : Arrays.asList(args),
//                envp == null ? Collections.EMPTY_LIST : Arrays.asList(envp)));
        // MRI does not do this check, but posix_spawn does not reliably ENOENT for bad filenames like ''
        if (cmd == null || cmd.length() == 0) {
            errno = Errno.ENOENT;
            return -1;
        }
        long ret = runtime.getPosix().posix_spawnp(
                cmd,
                eargp.fileActions,
                eargp.attributes,
                args == null ? Collections.EMPTY_LIST : Arrays.asList(args),
                envp == null ? Collections.EMPTY_LIST : Arrays.asList(envp));

        if (ret == -1) {
            errno = Errno.valueOf(runtime.getPosix().errno());
        }

        return ret;
    }

    // MRI: Basically doing sh processing from proc_exec_sh but for non-fork path
    private long procSpawnSh(Ruby runtime, ExecArg eargp, String str, String[] envp) {
        char[] sChars;
        int s = 0;

        sChars = str.toCharArray();
        while (s < sChars.length && (sChars[s] == ' ' || sChars[s] == '\t' || sChars[s] == '\n'))
            s++;

        if (s >= sChars.length) {
            errno = Errno.ENOENT;
            return -1;
        }

        // TODO: Windows
        if (Platform.IS_WINDOWS) { // #ifdef _WIN32
//            rb_w32_uspawn(P_OVERLAY, (char *)str, 0);
            return -1;
        } else {
//            #if defined(__CYGWIN32__) || defined(__EMX__)
//            {
//                char fbuf[MAXPATHLEN];
//                char *shell = dln_find_exe_r("sh", 0, fbuf, sizeof(fbuf));
//                int status = -1;
//                if (shell)
//                    execl(shell, "sh", "-c", str, (char *) NULL);
//                else
//                status = system(str);
//                if (status != -1)
//                    exit(status);
//            }
//            #else
            long ret = runtime.getPosix().posix_spawnp(
                    "/bin/sh",
                    eargp.fileActions,
                    eargp.attributes,
                    Arrays.asList("sh", "-c", str),
                    envp == null ? Collections.EMPTY_LIST : Arrays.asList(envp));

            if (ret == -1) {
                errno = Errno.valueOf(runtime.getPosix().errno());
            }

            return ret;
        }
    }

    private static class PopenArg {
        ExecArg eargp;
        int modef;
    }

    private static String[] ARGVSTR2ARGV(byte[][] argv_str) {
        String[] argv = new String[argv_str.length];
        for (int i = 0; i < argv_str.length; i++) {
            // FIXME: probably should be using a specific encoding
            if (argv_str[i] == null) continue; // placeholder for /bin/sh, but unsure where it's supposed to be added
            argv[i] = new String(argv_str[i]);
        }
        return argv;
    }

    private Errno errno = null;

    // MRI: pipe_open
    private RubyIO pipeOpen(ThreadContext context, ExecArg eargp, String modestr, int fmode, IOEncodable convconfig) {
        final Ruby runtime = context.runtime;
        IRubyObject prog = eargp != null ? (eargp.use_shell ? eargp.command_name : eargp.command_name) : null;
        long pid;
        OpenFile fptr;
        RubyIO port;
        OpenFile write_fptr;
        IRubyObject write_port;
        PosixShim posix = new PosixShim(runtime);

        Errno e = null;

        String[] args = null;
        String[] envp = null;

        ExecArg sargp = new ExecArg();
        int fd;
        int write_fd = -1;
        String cmd = null;

        if (prog != null)
            cmd = StringSupport.checkEmbeddedNulls(runtime, prog).toString();

        if (eargp.chdirGiven) {
            // we can'd do chdir with posix_spawn, so we should be set to use_shell and now
            // just need to add chdir to the cmd
            cmd = "cd '" + eargp.chdir_dir + "'; " + cmd;
            eargp.chdir_dir = null;
            eargp.chdirGiven = false;
        }

        if (eargp != null && !eargp.use_shell) {
            args = eargp.argv_str.argv;
        }
        int[] pair = {-1,-1}, writePair = {-1, -1};
        switch (fmode & (OpenFile.READABLE|OpenFile.WRITABLE)) {
            case OpenFile.READABLE | OpenFile.WRITABLE:
                if (API.rb_pipe(runtime, writePair) == -1)
                    throw runtime.newErrnoFromErrno(posix.getErrno(), prog.toString());
                if (API.rb_pipe(runtime, pair) == -1) {
                    e = posix.getErrno();
                    runtime.getPosix().close(writePair[1]);
                    runtime.getPosix().close(writePair[0]);
                    posix.setErrno(e);
                    throw runtime.newErrnoFromErrno(posix.getErrno(), prog.toString());
                }

                if (eargp != null) prepareStdioRedirects(runtime, pair, writePair, eargp);

                break;
            case OpenFile.READABLE:
                if (API.rb_pipe(runtime, pair) == -1)
                    throw runtime.newErrnoFromErrno(posix.getErrno(), prog.toString());

                if (eargp != null) prepareStdioRedirects(runtime, pair, null, eargp);

                break;
            case OpenFile.WRITABLE:
                if (API.rb_pipe(runtime, pair) == -1)
                    throw runtime.newErrnoFromErrno(posix.getErrno(), prog.toString());

                if (eargp != null) prepareStdioRedirects(runtime, null, pair, eargp);

                break;
            default:
                throw runtime.newSystemCallError(prog.toString());
        }
        if (eargp != null) {
            try {
                execargFixup(context, runtime, eargp);
            } catch (RaiseException re) { // if (state)
                if (writePair[0] != -1) runtime.getPosix().close(writePair[0]);
                if (writePair[1] != -1) runtime.getPosix().close(writePair[1]);
                if (pair[0] != -1) runtime.getPosix().close(pair[0]);
                if (pair[1] != -1) runtime.getPosix().close(pair[1]);
                throw re;
            }
            execargRunOptions(context, runtime, eargp, sargp, null);
            if (eargp.envp_str != null) envp = eargp.envp_str;
            while ((pid = DO_SPAWN(runtime, eargp, cmd, args, envp)) == -1) {
	            /* exec failed */
                switch (e = errno) {
                    case EAGAIN:
                    case EWOULDBLOCK:
                        try {Thread.sleep(1000);} catch (InterruptedException ie) {}
                        continue;
                }
                break;
            }
            if (eargp != null)
                execargRunOptions(context, runtime, sargp, null, null);
        }
        else {
            throw runtime.newNotImplementedError("spawn without exec args (probably a bug)");
        }

        /* parent */
        if (pid == -1) {
            runtime.getPosix().close(pair[1]);
            runtime.getPosix().close(pair[0]);
            if ((fmode & (OpenFile.READABLE|OpenFile.WRITABLE)) == (OpenFile.READABLE|OpenFile.WRITABLE)) {
                runtime.getPosix().close(pair[1]);
                runtime.getPosix().close(pair[0]);
            }
            errno = e;
            throw runtime.newErrnoFromErrno(errno, prog.toString());
        }
        if ((fmode & OpenFile.READABLE) != 0 && (fmode & OpenFile.WRITABLE) != 0) {
            runtime.getPosix().close(pair[1]);
            fd = pair[0];
            runtime.getPosix().close(writePair[0]);
            write_fd = writePair[1];
        }
        else if ((fmode & OpenFile.READABLE) != 0) {
            runtime.getPosix().close(pair[1]);
            fd = pair[0];
        }
        else {
            runtime.getPosix().close(pair[0]);
            fd = pair[1];
        }

        port = (RubyIO) runtime.getIO().allocate();
        fptr = port.MakeOpenFile();
        fptr.setChannel(new NativeDeviceChannel(fd));
        fptr.setMode(fmode | (OpenFile.SYNC|OpenFile.DUPLEX));
        if (convconfig != null) {
            fptr.encs.copy(convconfig);
            if (Platform.IS_WINDOWS) { // #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
                if ((fptr.encs.ecflags & EncodingUtils.ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
                    fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
                }
            }
        }
        else {
            if (fptr.NEED_NEWLINE_DECORATOR_ON_READ()) {
                fptr.encs.ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
            }
            if (EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != 0) {
                if (fptr.NEED_NEWLINE_DECORATOR_ON_WRITE()) {
                    fptr.encs.ecflags |= EncodingUtils.TEXTMODE_NEWLINE_DECORATOR_ON_WRITE;
                }
            }
        }
        final long finalPid = pid;
        fptr.setPid(pid);
        fptr.setProcess(new POSIXProcess(runtime, finalPid));

        if (write_fd != -1) {
            write_port = runtime.getIO().allocate();
            write_fptr = ((RubyIO)write_port).MakeOpenFile();
            write_fptr.setChannel(new NativeDeviceChannel(write_fd));
            write_fptr.setMode((fmode & ~OpenFile.READABLE)| OpenFile.SYNC|OpenFile.DUPLEX);
            fptr.setMode(fptr.getMode() & ~OpenFile.WRITABLE);
            fptr.tiedIOForWriting = (RubyIO)write_port;
            port.setInstanceVariable("@tied_io_for_writing", write_port);
        }

//        fptr.setFinalizer(fptr.PIPE_FINALIZE);

        // TODO?
//        pipeAddFptr(fptr);
        return port;
    }

    private void prepareStdioRedirects(Ruby runtime, int[] readPipe, int[] writePipe, ExecArg eargp) {
        // We insert these redirects directly into fd_dup2 so that chained redirection can be
        // validated and set up properly by the execargFixup logic.
        // The closes do not appear to be part of MRI's logic (they close the fd before exec/spawn),
        // so rather than using execargAddopt we do them directly here.

        if (readPipe != null) {
            // dup our read pipe's write end into stdout
            int readPipeWriteFD = readPipe[1];
            eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, runtime.newFixnum(1), runtime.newFixnum(readPipeWriteFD));

            // close the other end of the pipe in the child
            int readPipeReadFD = readPipe[0];
            eargp.fileActions.add(SpawnFileAction.close(readPipeReadFD));
        }

        if (writePipe != null) {
            // dup our write pipe's read end into stdin
            int writePipeReadFD = writePipe[0];
            eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, runtime.newFixnum(0), runtime.newFixnum(writePipeReadFD));

            // close the other end of the pipe in the child
            int writePipeWriteFD = writePipe[1];
            eargp.fileActions.add(SpawnFileAction.close(writePipeWriteFD));
        }
    }

    static int run_exec_pgroup(Ruby runtime, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        /*
         * If FD_CLOEXEC is available, rb_fork waits the child's execve.
         * So setpgid is done in the child when rb_fork is returned in the parent.
         * No race condition, even without setpgid from the parent.
         * (Is there an environment which has setpgid but no FD_CLOEXEC?)
         */
        int ret = 0;
        long pgroup;

        pgroup = eargp.pgroup_pgid;
        if (pgroup == -1) {
            // inherit parent's process group (default behavior)
            return ret;
        }

        eargp.attributes.add(SpawnAttribute.pgroup(pgroup));
        eargp.attributes.add(SpawnAttribute.flags((short)SpawnAttribute.SETPGROUP));

        return ret;
    }

    static int run_exec_rlimit(Ruby runtime, RubyArray ary, ExecArg sargp, String[] errmsg) {
        throw runtime.newNotImplementedError("changing rlimit in child is not supported");
        /* Not supported by posix_spawn
        long i;
        for (i = 0; i < ary.size(); i++) {
            IRubyObject elt = ary.eltOk(i);
            int rtype = RubyNumeric.num2int(((RubyArray)elt).eltOk(0));

            struct rlimit rlim;
            if (sargp != null) {
                IRubyObject tmp, newary;
                if (runtime.getPosix().getrlimit(rtype, &rlim) == -1) {
                    if (errmsg != null) errmsg[0] = "getrlimit";
                    return -1;
                }
                tmp = runtime.newArray(((RubyArray)elt).eltOk(0),
                        runtime.newFixnum(rlim.rlim_cur),
                        runtime.newFixnum(rlim.rlim_max));
                if (sargp.rlimit_limits == null)
                    newary = sargp.rlimit_limits = runtime.newArray();
                else
                    newary = sargp.rlimit_limits;
                ((RubyArray)newary).push(tmp);
            }
            rlim.rlim_cur = RubyNumeric.num2int(((RubyArray)elt).eltOk(1));
            rlim.rlim_max = RubyNumeric.num2int(((RubyArray)elt).eltOk(2));
            */

            // we can't setrlimit in parent
//            if (runtime.getPosix().setrlimit(rtype, &rlim) == -1) { /* hopefully async-signal-safe */
//                if (errmsg != null) errmsg[0] = "setrlimit";
//                return -1;
//            }
//        }
//        return 0;
    }

    static void saveEnv(ThreadContext context, Ruby runtime, ExecArg sargp) {
        // We don't need to save env in parent because we let posix_spawn set it up
//        if (sargp == null)
//            return;
//        if (sargp.env_modification == null) {
//            RubyHash env = runtime.getENV();
//            if (!env.isNil()) {
//                final RubyArray ary = runtime.newArray();
//                BlockCallback SaveEnvBody = new BlockCallback() {
//                    @Override
//                    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
//                        ary.push(args[0].dup());
//                        return context.nil;
//                    }
//                };
//                env.each(context, CallBlock.newCallClosure(env, runtime.getHash(),
//                        Arity.OPTIONAL, SaveEnvBody, context));
//                sargp.env_modification = ary;
//            }
//            sargp.unsetenv_others_given_set();
//            sargp.unsetenv_others_do_set();
//        }
    }

    static int run_exec_dup2(Ruby runtime, RubyArray ary, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        int n, i;
        int ret;
        int extra_fd = -1;
        run_exec_dup2_fd_pair[] pairs = eargp.dup2_tmpbuf;

        n = ary.size();

        /* initialize oldfd and newfd: O(n) */
        for (i = 0; i < n; i++) {
            IRubyObject elt = ary.eltOk(i);
            pairs[i].oldfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
            pairs[i].newfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0)); /* unique */
            pairs[i].older_index = -1;
        }

        /* sort the table by oldfd: O(n log n) */
        if (sargp == null)
            Arrays.sort(pairs, intcmp); /* hopefully async-signal-safe */
        else
            Arrays.sort(pairs, intrcmp);

        /* initialize older_index and num_newer: O(n log n) */
        for (i = 0; i < n; i++) {
            int newfd = pairs[i].newfd;
            run_exec_dup2_fd_pair key = new run_exec_dup2_fd_pair();
            key.oldfd = newfd;
            int found = Arrays.binarySearch(pairs, key, intcmp); /* hopefully async-signal-safe */
            pairs[i].num_newer = 0;
            if (found >= 0) {
                while (found > 0 && pairs[found-1].oldfd == newfd) found--;
                while (found < n && pairs[found].oldfd == newfd) {
                    pairs[i].num_newer++;
                    pairs[found].older_index = i;
                    found++;
                }
            }
        }

        /* non-cyclic redirection: O(n) */
        for (i = 0; i < n; i++) {
            int j = i;
            while (j != -1 && pairs[j].oldfd != -1 && pairs[j].num_newer == 0) {
                if (saveRedirectFd(runtime, pairs[j].newfd, sargp, errmsg) < 0) /* async-signal-safe */
                    return -1;

                // This always succeeds because we just defer it to posix_spawn.
                redirectDup2(eargp, pairs[j].oldfd, pairs[j].newfd); /* async-signal-safe */
                pairs[j].oldfd = -1;
                j = (int) pairs[j].older_index;
                if (j != -1) pairs[j].num_newer--;
            }
        }

        /* cyclic redirection: O(n) */
        for (i = 0; i < n; i++) {
            int j;
            if (pairs[i].oldfd == -1)
                continue;
            if (pairs[i].oldfd == pairs[i].newfd) { /* self cycle */
                int fd = pairs[i].oldfd;
                ret = runtime.getPosix().fcntl(fd, Fcntl.F_GETFD); /* async-signal-safe */
                if (ret == -1) {
                    if (errmsg != null) errmsg[0] = "fcntl(F_GETFD)";
                    return -1;
                }
                if ((ret & FcntlLibrary.FD_CLOEXEC) != 0) {
                    ret &= ~FcntlLibrary.FD_CLOEXEC;
                    ret = runtime.getPosix().fcntlInt(fd, Fcntl.F_SETFD, ret); /* async-signal-safe */
                    if (ret == -1) {
                        if (errmsg != null) errmsg[0] = "fcntl(F_SETFD)";
                        return -1;
                    }
                }
                pairs[i].oldfd = -1;
                continue;
            }
            if (extra_fd == -1) {
                extra_fd = redirectDup(runtime, pairs[i].oldfd); /* async-signal-safe */
                if (extra_fd == -1) {
                    if (errmsg != null) errmsg[0] = "dup";
                    return -1;
                }
//                rb_update_max_fd(extra_fd);
            }
            else {
                // This always succeeds because we just defer it to posix_spawn.
                redirectDup2(eargp, pairs[i].oldfd, extra_fd); /* async-signal-safe */
            }
            pairs[i].oldfd = extra_fd;
            j = pairs[i].older_index;
            pairs[i].older_index = -1;
            while (j != -1) {
                // This always succeeds because we just defer it to posix_spawn.
                redirectDup2(eargp, pairs[j].oldfd, pairs[j].newfd); /* async-signal-safe */
                pairs[j].oldfd = -1;
                j = pairs[j].older_index;
            }
        }
        if (extra_fd != -1) {
            redirectClose(runtime, eargp, extra_fd);
        }

        return 0;
    }

    static int redirectDup(Ruby runtime, int oldfd)
    {
        // Partial impl of rb_cloexec_fcntl_dup
        int ret;
        ret = runtime.getPosix().dup(oldfd);
        int flags = runtime.getPosix().fcntl(ret, Fcntl.F_GETFD);
        runtime.getPosix().fcntlInt(ret, Fcntl.F_SETFD, flags | FcntlLibrary.FD_CLOEXEC);
        return ret;
    }

    static int redirectCloexecDup(Ruby runtime, int oldfd)
    {
        int ret = redirectDup(runtime, oldfd);

        int flags = runtime.getPosix().fcntl(ret, Fcntl.F_GETFD);
        runtime.getPosix().fcntlInt(ret, Fcntl.F_SETFD, flags | FcntlLibrary.FD_CLOEXEC);

        return ret;
    }

    static void redirectDup2(ExecArg eargp, int oldfd, int newfd)
    {
        eargp.fileActions.add(SpawnFileAction.dup(oldfd, newfd));
    }

    static void redirectClose(Ruby runtime, ExecArg eargp, int fd) {
        eargp.fileActions.add(SpawnFileAction.close(fd));
    }

    static void redirectOpen(ExecArg eargp, int fd, String pathname, int flags, int perm) {
        eargp.fileActions.add(SpawnFileAction.open(pathname, fd, flags, perm));
    }

    static int saveRedirectFd(Ruby runtime, int fd, ExecArg sargp, String[] errmsg) {
        // This logic is to restore the parent's fd. Since we let posix_spawn do dup2 for us in the
        // child, it's not necessary for us to fix up the parent.

        if (false && sargp != null) {
            RubyArray newary;
            int save_fd = redirectCloexecDup(runtime, fd);
            if (save_fd == -1) {
                if (runtime.getPosix().errno() == Errno.EBADF.intValue())
                    return 0;
                if (errmsg != null) errmsg[0] = "dup";
                return -1;
            }
//            rb_update_max_fd(save_fd);

            newary = sargp.fd_dup2;
            if (newary == null) {
                newary = runtime.newArray();
                sargp.fd_dup2 = newary;
            }
            newary.push(runtime.newArray(runtime.newFixnum(fd), runtime.newFixnum(save_fd)));

            newary = sargp.fd_close;
            if (newary == null) {
                newary = runtime.newArray();
                sargp.fd_close = newary;
            }
            newary.push(runtime.newArray(runtime.newFixnum(save_fd), runtime.getNil()));
        }

        return 0;
    }

    int execargRunOptions(ThreadContext context, Ruby runtime, ExecArg eargp, ExecArg sargp, String[] errmsg) {
        IRubyObject obj;

        if (sargp != null) {
            /* assume that sargp is always NULL on fork-able environments */
            sargp.redirect_fds = context.nil;
        }

        if (eargp.pgroupGiven) {
            if (run_exec_pgroup(runtime, eargp, sargp, errmsg) == -1) /* async-signal-safe */
                return -1;
        }

        obj = eargp.rlimit_limits;
        if (obj != null) {
            throw runtime.newNotImplementedError("setting rlimit in child is unsupported");
        }

        boolean clearEnv = false;
        if (eargp.unsetenvOthersGiven && eargp.unsetenvOthersDo) {
            // we handle this elsewhere by starting from a blank env
            clearEnv = true;
        }

        RubyArray env = eargp.env_modification;
        if (env != null) {
            eargp.envp_str = ShellLauncher.getModifiedEnv(runtime, env, clearEnv);
        }

        if (eargp.umaskGiven) {
            throw runtime.newNotImplementedError("setting umask in child is unsupported");
        }

        obj = eargp.fd_dup2;
        if (obj != null) {
            if (run_exec_dup2(runtime, (RubyArray)obj, eargp, sargp, errmsg) == -1) /* hopefully async-signal-safe */
                return -1;
        }

        obj = eargp.fd_close;
        if (obj != null) {
            run_exec_close(runtime, (RubyArray)obj, eargp);
        }

        obj = eargp.fd_dup2_child;
        if (obj != null) {
            run_exec_dup2_child(runtime, (RubyArray)obj, eargp);
        }

        if (eargp.chdirGiven) {
            // should have been set up in pipe_open, so we just raise here
            throw new RuntimeException("BUG: chdir not supported in posix_spawn; should have been made into chdir");
        }

        if (eargp.gidGiven) {
            throw runtime.newNotImplementedError("setgid in the child is not supported");
        }

        if (eargp.uidGiven) {
            throw runtime.newNotImplementedError("setuid in the child is not supported");
        }

        return 0;
    }

    static void run_exec_close(Ruby runtime, RubyArray ary, ExecArg eargp) {
        for (int i = 0; i < ary.size(); i++) {
            RubyArray elt = (RubyArray)ary.eltOk(i);
            int fd = RubyNumeric.fix2int(elt.eltOk(0));
            redirectClose(runtime, eargp, fd);
        }
    }

    static void run_exec_open(ThreadContext context, Ruby runtime, RubyArray<RubyArray> ary, ExecArg eargp) {
        for (int i = 0; i < ary.size(); i++) {
            RubyArray<RubyArray> elt = ary.eltOk(i);
            int fd = RubyNumeric.fix2int(elt.eltOk(0));
            RubyArray param = elt.eltOk(1);
            IRubyObject vpath = param.eltOk(0);
            int flags = RubyNumeric.num2int(param.eltOk(1));
            int perm = RubyNumeric.num2int(param.eltOk(2));
            IRubyObject fd2v = param.entry(3);

            if (fd2v.isNil()) {
                redirectOpen(eargp, fd, vpath.toString(), flags, perm);
                param.store(3, elt.eltOk(0));
            } else {
                redirectDup2(eargp, RubyNumeric.num2int(fd2v), fd);
            }
        }
    }

    /**
     * Add spawn configuration for duplicating descriptors in the child
     */
    static void run_exec_dup2_child(Ruby runtime, RubyArray ary, ExecArg eargp) {
        for (int i = 0; i < ary.size(); i++) {
            RubyArray elt = (RubyArray)ary.eltOk(i);
            int newfd = RubyNumeric.fix2int(elt.eltOk(0));
            int oldfd = RubyNumeric.fix2int(elt.eltOk(1));

            redirectDup2(eargp, oldfd, newfd);
        }
    }

    private static class run_exec_dup2_fd_pair {
        int oldfd;
        int newfd;
        int older_index;
        int num_newer;
    };

    static int runExecDup2TmpbufSize(int n) {
        return n;
    }

    static void execargFixup(ThreadContext context, Ruby runtime, ExecArg eargp) {
        execargParentStart(context, runtime, eargp);
    }

    static void execargParentStart(ThreadContext context, Ruby runtime, ExecArg eargp) {
        try {
            execargParentStart1(context, runtime, eargp);
        } catch (RaiseException re) {
            throw re;
        }
    }

    static void execargParentStart1(ThreadContext context, Ruby runtime, ExecArg eargp) {
        boolean unsetenv_others;
        RubyArray envopts;
        RubyArray<RubyArray> ary;

        eargp.redirect_fds = checkExecFds(context, runtime, eargp);

        ary = eargp.fd_open;
        if (ary != null) {
            run_exec_open(context, runtime, ary, eargp);
        }

        ary = eargp.fd_dup2;
        if (ary != null) {
            int len = runExecDup2TmpbufSize(ary.size());
            run_exec_dup2_fd_pair[] tmpbuf = new run_exec_dup2_fd_pair[len];
            for (int i = 0; i < tmpbuf.length; i++) tmpbuf[i] = new run_exec_dup2_fd_pair();
            eargp.dup2_tmpbuf = tmpbuf;
        }

        IRubyObject envtbl;
        unsetenv_others = eargp.unsetenvOthersGiven && eargp.unsetenvOthersDo;
        envopts = eargp.env_modification;
        if (unsetenv_others || envopts != null) {
            if (unsetenv_others) {
                envtbl = RubyHash.newHash(runtime);
            }
            else {
                envtbl = runtime.getObject().getConstant("ENV");
                envtbl = TypeConverter.convertToType(envtbl, runtime.getHash(), "to_hash").dup();
            }
            if (envopts != null) {
                RubyHash stenv = (RubyHash)envtbl;
                long i;
                for (i = 0; i < envopts.size(); i++) {
                    IRubyObject pair = envopts.eltOk(i);
                    IRubyObject key = ((RubyArray)pair).eltOk(0);
                    IRubyObject val = ((RubyArray)pair).eltOk(1);
                    if (val.isNil()) {
                        IRubyObject stkey = key;
                        stenv.fastDelete(stkey);
                    }
                    else {
                        stenv.op_aset(context, key, val);
                    }
                }
            }
        } else {
            // In MRI, they use the current env as the baseline because they fork+exec. We can't do that,
            // and posix_spawn needs a full env, so we pass even unmodified env through.
            envtbl = runtime.getObject().getConstant("ENV");
            envtbl = TypeConverter.convertToType(envtbl, runtime.getHash(), "to_hash");
        }
        buildEnvp(runtime, eargp, envtbl);
//        RB_GC_GUARD(execarg_obj);
    }

    static ChannelFD open_func(Ruby runtime, RubyIO.Sysopen data) {
        ChannelFD ret = parentRedirectOpen(runtime, data);
        data.errno = Errno.valueOf(runtime.getPosix().errno());
        return ret;
    }

    static ChannelFD parentRedirectOpen(Ruby runtime, RubyIO.Sysopen data) {
        return RubyIO.cloexecOpen(runtime, data);
    }

    static void parentRedirectClose(Ruby runtime, int fd) {
        // close_unless_reserved
        if (fd > 2) runtime.getPosix().close(fd);
    }

    private static void buildEnvp(Ruby runtime, ExecArg eargp, IRubyObject envtbl) {
        String[] envp_str;
        List<String> envp_buf;
        envp_buf = new ArrayList();
        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)((RubyHash)envtbl).directEntrySet()) {
            IRubyObject key = entry.getKey();
            IRubyObject val = entry.getValue();

            envp_buf.add(StringSupport.checkEmbeddedNulls(runtime, key).toString()
                    + "="
                    + StringSupport.checkEmbeddedNulls(runtime, val));
        }
        envp_str = new String[envp_buf.size()];
        envp_buf.toArray(envp_str);
        eargp.envp_str = envp_str;
        eargp.envp_buf = envp_buf;
    }

    static int checkExecFds1(ThreadContext context, Ruby runtime, ExecArg eargp, RubyHash h, int maxhint, IRubyObject ary) {
        long i;

        if (ary != null) {
            for (i = 0; i < ((RubyArray)ary).size(); i++) {
                IRubyObject elt = ((RubyArray)ary).eltOk(i);
                int fd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0));
                if (h.fastARef(runtime.newFixnum(fd)) != null) {
                    throw runtime.newArgumentError("fd " + fd + " specified twice");
                }
                if (ary == eargp.fd_open || ary == eargp.fd_dup2)
                    h.op_aset(context, runtime.newFixnum(fd), runtime.getTrue());
                else if (ary == eargp.fd_dup2_child)
                    h.op_aset(context, runtime.newFixnum(fd), ((RubyArray)elt).eltOk(1));
                else /* ary == eargp.fd_close */
                    h.op_aset(context, runtime.newFixnum(fd), runtime.newFixnum(-1));
                if (maxhint < fd)
                    maxhint = fd;
                if (ary == eargp.fd_dup2 || ary == eargp.fd_dup2_child) {
                    fd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
                    if (maxhint < fd)
                        maxhint = fd;
                }
            }
        }
        return maxhint;
    }

    static IRubyObject checkExecFds(ThreadContext context, Ruby runtime, ExecArg eargp) {
        RubyHash h = RubyHash.newHash(runtime);
        IRubyObject ary;
        int maxhint = -1;
        long i;

        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_dup2);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_close);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_open);
        maxhint = checkExecFds1(context, runtime, eargp, h, maxhint, eargp.fd_dup2_child);

        if (eargp.fd_dup2_child != null) {
            ary = eargp.fd_dup2_child;
            for (i = 0; i < ((RubyArray)ary).size(); i++) {
                IRubyObject elt = ((RubyArray)ary).eltOk(i);
                int newfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(0));
                int oldfd = RubyNumeric.fix2int(((RubyArray)elt).eltOk(1));
                int lastfd = oldfd;
                IRubyObject val = h.fastARef(runtime.newFixnum(lastfd));
                long depth = 0;
                while (val instanceof RubyFixnum && 0 <= ((RubyFixnum)val).getIntValue()) {
                    lastfd = RubyNumeric.fix2int(val);
                    val = h.fastARef(val);
                    if (((RubyArray)ary).size() < depth)
                        throw runtime.newArgumentError("cyclic child fd redirection from " + oldfd);
                    depth++;
                }
                if (val != runtime.getTrue())
                    throw runtime.newArgumentError("child fd " + oldfd + " is not redirected");
                if (oldfd != lastfd) {
                    IRubyObject val2;
                    ((RubyArray)elt).store(1, runtime.newFixnum(lastfd));
                    h.op_aset(context, runtime.newFixnum(newfd), runtime.newFixnum(lastfd));
                    val = runtime.newFixnum(oldfd);
                    while ((val2 = h.fastARef(val)) instanceof RubyFixnum) {
                        h.op_aset(context, val, runtime.newFixnum(lastfd));
                        val = val2;
                    }
                }
            }
        }

        eargp.close_others_maxhint = maxhint;
        return h;
    }

    static int execargAddopt(ThreadContext context, Ruby runtime, ExecArg eargp, IRubyObject key, IRubyObject val) {
        String id;
        int rtype = 0;

        boolean redirect = false;
        switch (key.getType().getClassIndex()) {
            case SYMBOL:
                id = key.toString();
//                #ifdef HAVE_SETPGID
                if (id.equals("pgroup")) {
                    long pgroup;
                    if (eargp.pgroupGiven) {
                        throw runtime.newArgumentError("pgroup option specified twice");
                    }
                    if (val == null || !val.isTrue())
                        pgroup = -1; /* asis(-1) means "don't call setpgid()". */
                    else if (val == runtime.getTrue())
                        pgroup = 0; /* new process group. */
                    else {
                        pgroup = val.convertToInteger().getLongValue();
                        if (pgroup < 0) {
                            throw runtime.newArgumentError("negative process group symbol : " + pgroup);
                        }
                    }
                    eargp.pgroupGiven = true;
                    eargp.pgroup_pgid = pgroup;
                }
                else if (Platform.IS_WINDOWS && id.equals("new_pgroup")) {
                    if (eargp.newPgroupGiven) {
                        throw runtime.newArgumentError("new_pgroup option specified twice");
                    }
                    eargp.newPgroupGiven = true;
                    eargp.newPgroupFlag = val.isTrue();
                }
                else if (false &&  // unsupported
                        RLIMIT.RLIMIT_AS.defined() && id.startsWith("rlimit_")) {
//                        && (rtype = rlimitTypeByLname(id.substring(7)) != -1)) {
                    IRubyObject ary;
                    IRubyObject tmp, softlim, hardlim;
                    if (eargp.rlimit_limits == null)
                        ary = eargp.rlimit_limits = runtime.newArray();
                    else
                        ary = eargp.rlimit_limits;
                    tmp = TypeConverter.checkArrayType(runtime, val);
                    if (!tmp.isNil()) {
                        if (((RubyArray)tmp).size() == 1)
                            softlim = hardlim = ((RubyArray)tmp).eltOk(0).convertToInteger();
                        else if (((RubyArray)tmp).size() == 2) {
                            softlim = ((RubyArray)tmp).eltOk(0).convertToInteger();
                            hardlim = ((RubyArray)tmp).eltOk(1).convertToInteger();
                        }
                        else {
                            throw runtime.newArgumentError("wrong exec rlimit option");
                        }
                    }
                    else {
                        softlim = hardlim = val.convertToInteger();
                    }
                    tmp = RubyArray.newArray(runtime, runtime.newFixnum(rtype), softlim, hardlim);
                    ((RubyArray)ary).push(tmp);
                }
                else if (id.equals("unsetenv_others")) {
                    if (eargp.unsetenvOthersGiven) {
                        throw runtime.newArgumentError("unsetenv_others option specified twice");
                    }
                    eargp.unsetenvOthersGiven = true;
                    if (val.isTrue()) {
                        eargp.unsetenvOthersDo = true;
                    } else {
                        eargp.unsetenvOthersDo = false;
                    }
                }
                else if (id.equals("chdir")) {
                    if (eargp.chdirGiven) {
                        throw runtime.newArgumentError("chdir option specified twice");
                    }
                    RubyString valTmp = RubyFile.get_path(context, val);
                    eargp.chdirGiven = true;
                    eargp.chdir_dir = valTmp.toString();
                }
                else if (id.equals("umask")) {
                    int cmask = val.convertToInteger().getIntValue();
                    if (eargp.umaskGiven) {
                        throw runtime.newArgumentError("umask option specified twice");
                    }
                    eargp.umaskGiven = true;
                    eargp.umask_mask = cmask;
                }
                else if (id.equals("close_others")) {
                    if (eargp.closeOthersGiven) {
                        throw runtime.newArgumentError("close_others option specified twice");
                    }
                    eargp.closeOthersGiven = true;
                    if (!val.isNil()) {
                        eargp.closeOthersDo = true;
                    } else {
                        eargp.closeOthersDo = false;
                    }
                }
                else if (id.equals("in")) {
                    key = RubyFixnum.zero(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("out")) {
                    key = RubyFixnum.one(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("err")) {
                    key = RubyFixnum.two(runtime);
                    checkExecRedirect(context, runtime, key, val, eargp);
                }
                else if (id.equals("uid") && false) { // TODO
//                    #ifdef HAVE_SETUID
                    if (eargp.uidGiven) {
                        throw runtime.newArgumentError("uid option specified twice");
                    }
//                    checkUidSwitch();
                    {
//                        PREPARE_GETPWNAM;
                        eargp.uid = val.convertToInteger().getIntValue();
                        eargp.uidGiven = true;
                    }
//                    #else
//                    rb_raise(rb_eNotImpError,
//                            "uid option is unimplemented on this machine");
//                    #endif
                }
                else if (id.equals("gid") && false) { // TODO
//                    #ifdef HAVE_SETGID
                    if (eargp.gidGiven) {
                        throw runtime.newArgumentError("gid option specified twice");
                    }
//                    checkGidSwitch();
                    {
//                        PREPARE_GETGRNAM;
                        eargp.gid = val.convertToInteger().getIntValue();
                        eargp.gidGiven = true;
                    }
//                    #else
//                    rb_raise(rb_eNotImpError,
//                            "gid option is unimplemented on this machine");
//                    #endif
                }
                else if (id.equals("exception")) {
                    if (eargp.exception_given) {
                        throw runtime.newArgumentError("exception option specified twice");
                    }
                    eargp.exception_given = true;
                    eargp.exception = val.isTrue();
                }
                else {
                    return ST_STOP;
                }
                break;

            case INTEGER:
                if (!(key instanceof RubyFixnum)) {
                    return ST_STOP;
                }
            case FILE:
            case IO:
            case ARRAY:
                checkExecRedirect(context, runtime, key, val, eargp);
                break;

            default:
                return ST_STOP;
        }

        return ST_CONTINUE;
    }

    // MRI: check_exec_redirect
    static void checkExecRedirect(ThreadContext context, Ruby runtime, IRubyObject key, IRubyObject val, ExecArg eargp) {
        IRubyObject param;
        IRubyObject path, flags, perm;
        IRubyObject tmp;
        String id;

        switch (val.getMetaClass().getRealClass().getClassIndex()) {
            case SYMBOL:
                id = val.toString();
                if (id.equals("close")) {
                    param = context.nil;
                    eargp.fd_close = checkExecRedirect1(runtime, eargp.fd_close, key, param);
                }
                else if (id.equals("in")) {
                    param = runtime.newFixnum(0);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else if (id.equals("out")) {
                    param = runtime.newFixnum(1);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else if (id.equals("err")) {
                    param = runtime.newFixnum(2);
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                }
                else {
                    throw runtime.newArgumentError("wrong exec redirect symbol: " + id);
                }
                break;

            case FILE:
            case IO:
                val = checkExecRedirectFd(runtime, val, false);
                /* fall through */
            case INTEGER:
                if (val instanceof RubyFixnum) {
                    param = val;
                    eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
                    break;
                }

                checkExecRedirectDefault(runtime, key, val, eargp);
                break;

            case ARRAY:
                path = ((RubyArray)val).eltOk(0);
                if (((RubyArray)val).size() == 2 && path instanceof RubySymbol &&
                        path.toString().equals("child")) {
                    param = checkExecRedirectFd(runtime, ((RubyArray)val).eltOk(1), false);
                    eargp.fd_dup2_child = checkExecRedirect1(runtime, eargp.fd_dup2_child, key, param);
                }
                else {
                    path = RubyFile.get_path(context, path);
                    flags = ((RubyArray)val).eltOk(1);
                    int intFlags;
                    if (flags.isNil())
                        intFlags = OpenFlags.O_RDONLY.intValue();
                    else if (flags instanceof RubyString)
                        intFlags = OpenFile.ioModestrOflags(runtime, flags.toString());
                    else
                        intFlags = flags.convertToInteger().getIntValue();
                    flags = runtime.newFixnum(intFlags);
                    perm = ((RubyArray)val).entry(2);
                    perm = perm.isNil() ? runtime.newFixnum(0644) : perm.convertToInteger();
                    param = RubyArray.newArray(runtime,
                            ((RubyString)path).strDup(runtime).export(context),
                            flags,
                            perm);
                    eargp.fd_open = checkExecRedirect1(runtime, eargp.fd_open, key, param);
                }
                break;

            case STRING:
                path = val;
                path = RubyFile.get_path(context, path);
                if (key instanceof RubyIO) {
                    key = checkExecRedirectFd(runtime, key, true);
                }
                if (key instanceof RubyFixnum && (((RubyFixnum)key).getIntValue() == 1 || ((RubyFixnum)key).getIntValue() == 2)) {
                    flags = runtime.newFixnum(OpenFlags.O_WRONLY.intValue()|OpenFlags.O_CREAT.intValue()|OpenFlags.O_TRUNC.intValue());
                } else if (key instanceof RubyArray) {
                    RubyArray keyAry = (RubyArray) key;
                    int i;
                    boolean allOut = true;
                    for (i = 0; i < keyAry.size(); i++) {
                        IRubyObject v = keyAry.eltOk(i);
                        IRubyObject fd = checkExecRedirectFd(runtime, v, true);
                        if (RubyNumeric.fix2int(fd) != 1 && RubyNumeric.fix2int(fd) != 2) {
                            allOut = false;
                            break;
                        }
                    }
                    if (allOut) {
                        flags = runtime.newFixnum(OpenFlags.O_WRONLY.intValue()|OpenFlags.O_CREAT.intValue()|OpenFlags.O_TRUNC.intValue());
                    } else {
                        flags = runtime.newFixnum(OpenFlags.O_RDONLY.intValue());
                    }
                } else {
                    flags = runtime.newFixnum(OpenFlags.O_RDONLY.intValue());
                }
                perm = runtime.newFixnum(0644);
                param = RubyArray.newArray(runtime,
                        ((RubyString)path).strDup(runtime).export(context),
                        flags,
                        perm);
                eargp.fd_open = checkExecRedirect1(runtime, eargp.fd_open, key, param);
                break;

            default:
                checkExecRedirectDefault(runtime, key, val, eargp);
        }

    }

    private static void checkExecRedirectDefault(Ruby runtime, IRubyObject key, IRubyObject val, ExecArg eargp) {
        IRubyObject tmp;
        IRubyObject param;
        tmp = val;
        val = TypeConverter.ioCheckIO(runtime, tmp);
        if (!val.isNil()) {
            val = checkExecRedirectFd(runtime, val, false);
            param = val;
            eargp.fd_dup2 = checkExecRedirect1(runtime, eargp.fd_dup2, key, param);
        }
        throw runtime.newArgumentError("wrong exec redirect action");
    }

    // MRI: check_exec_redirect_fd
    static IRubyObject checkExecRedirectFd(Ruby runtime, IRubyObject v, boolean iskey) {
        IRubyObject tmp;
        int fd;
        if (v instanceof RubyFixnum) {
            fd = RubyNumeric.fix2int(v);
        }
        else if (v instanceof RubySymbol) {
            String id = v.toString();
            if (id.equals("in"))
                fd = 0;
            else if (id.equals("out"))
                fd = 1;
            else if (id.equals("err"))
                fd = 2;
            else
                throw runtime.newArgumentError("wrong exec redirect");
        }
        else if (!(tmp = TypeConverter.convertToTypeWithCheck(v, runtime.getIO(), "to_io")).isNil()) {
            OpenFile fptr;
            fptr = ((RubyIO)tmp).getOpenFileChecked();
            if (fptr.tiedIOForWriting != null)
                throw runtime.newArgumentError("duplex IO redirection");
            fd = fptr.fd().bestFileno();
        }
        else {
            throw runtime.newArgumentError("wrong exec redirect");
        }
        if (fd < 0) {
            throw runtime.newArgumentError("negative file descriptor");
        }
        else if (Platform.IS_WINDOWS && fd >= 3 && iskey) {
            throw runtime.newArgumentError("wrong file descriptor (" + fd + ")");
        }
        return runtime.newFixnum(fd);
    }

    // MRI: check_exec_redirect1
    static RubyArray checkExecRedirect1(Ruby runtime, RubyArray ary, IRubyObject key, IRubyObject param) {
        if (ary == null) {
            ary = runtime.newArray();
        }
        if (!(key instanceof RubyArray)) {
            IRubyObject fd = checkExecRedirectFd(runtime, key, !param.isNil());
            ary.push(runtime.newArray(fd, param));
        }
        else {
            int i, n=0;
            for (i = 0 ; i < ((RubyArray)key).size(); i++) {
                IRubyObject v = ((RubyArray)key).eltOk(i);
                IRubyObject fd = checkExecRedirectFd(runtime, v, !param.isNil());
                ary.push(runtime.newArray(fd, param));
                n++;
            }
        }
        return ary;
    }

    private static final int ST_CONTINUE = 0;
    private static final int ST_STOP = 1;

    // rb_execarg_new
    public static ExecArg execargNew(ThreadContext context, IRubyObject[] argv, boolean accept_shell, boolean allow_exc_opt) {
        ExecArg eargp = new ExecArg();
        execargInit(context, argv, accept_shell, eargp, allow_exc_opt);
        return eargp;
    }

    // rb_execarg_init
    private static RubyString execargInit(ThreadContext context, IRubyObject[] argv, boolean accept_shell, ExecArg eargp, boolean allow_exc_opt) {
        RubyString prog, ret;
        IRubyObject[] env_opt = {context.nil, context.nil};
        IRubyObject[][] argv_p = {argv};
        IRubyObject exception = context.nil;
        prog = execGetargs(context, argv_p, accept_shell, env_opt);
        IRubyObject opt = env_opt[1];
        RubyHash optHash;
        RubySymbol exceptionSym = context.runtime.newSymbol("exception");
        if (allow_exc_opt && !opt.isNil() && (optHash = ((RubyHash) opt)).has_key_p(context, exceptionSym).isTrue()) {
            optHash = optHash.dupFast(context);
            exception = optHash.delete(context, exceptionSym);
        }
        execFillarg(context, prog, argv_p[0], env_opt[0], env_opt[1], eargp);
        if (exception.isTrue()) {
            eargp.exception = true;
        }
        ret = eargp.use_shell ? eargp.command_name : eargp.command_name;
        return ret;
    }

    // rb_exec_getargs
    private static RubyString execGetargs(ThreadContext context, IRubyObject[][] argv_p, boolean accept_shell, IRubyObject[] env_opt) {
        Ruby runtime = context.runtime;
        IRubyObject hash;
        RubyString prog;
        int beg = 0;
        int end = argv_p[0].length;

        // extract environment and options from args
        if (end >= 1) {
            hash = TypeConverter.checkHashType(runtime, argv_p[0][end - 1]);
            if (!hash.isNil()) {
                env_opt[1] = hash;
                end--;
            }
        }
        if (end >= 1) {
            hash = TypeConverter.checkHashType(runtime, argv_p[0][0]);
            if (!hash.isNil()) {
                env_opt[0] = hash;
                beg++;
            }
        }
        argv_p[0] = Arrays.copyOfRange(argv_p[0], beg, end);

        // try to extract program from args
        prog = checkArgv(context, argv_p[0]);

        if (prog == null) {
            // use first arg as program name and clear argv if we can use sh
            prog = (RubyString)argv_p[0][0];
            if (accept_shell && (end - beg) == 1) {
                argv_p[0] = IRubyObject.NULL_ARRAY;
            }
        }

        return prog;
    }

    // rb_check_argv
    public static RubyString checkArgv(ThreadContext context, IRubyObject[] argv) {
        Ruby runtime = context.runtime;
        IRubyObject tmp;
        RubyString prog;
        int i;

        Arity.checkArgumentCount(runtime, argv, 1, Integer.MAX_VALUE);

        prog = null;

        // if first parameter is an array, it is expected to be [program, $0 name]
        tmp = TypeConverter.checkArrayType(runtime, argv[0]);
        if (!tmp.isNil()) {
            RubyArray arrayArg = (RubyArray) tmp;
            if (arrayArg.size() != 2) {
                throw runtime.newArgumentError("wrong first argument");
            }
            prog = arrayArg.eltOk(0).convertToString();
            argv[0] = arrayArg.eltOk(1);
            StringSupport.checkEmbeddedNulls(runtime, prog);
            prog = prog.strDup(runtime);
            prog.setFrozen(true);
        }

        // process all arguments
        for (i = 0; i < argv.length; i++) {
            argv[i] = argv[i].convertToString().newFrozen();
            StringSupport.checkEmbeddedNulls(runtime, argv[i]);
        }

        // return program, or null if we did not yet determine it
        return prog;
    }

    private static final int posix_sh_cmd_length = 8;
    private static final String posix_sh_cmds[] = {
            "!",		/* reserved */
            ".",		/* special built-in */
            ":",		/* special built-in */
            "break",		/* special built-in */
            "case",		/* reserved */
            "continue",		/* special built-in */
            "do",		/* reserved */
            "done",		/* reserved */
            "elif",		/* reserved */
            "else",		/* reserved */
            "esac",		/* reserved */
            "eval",		/* special built-in */
            "exec",		/* special built-in */
            "exit",		/* special built-in */
            "export",		/* special built-in */
            "fi",		/* reserved */
            "for",		/* reserved */
            "if",		/* reserved */
            "in",		/* reserved */
            "readonly",		/* special built-in */
            "return",		/* special built-in */
            "set",		/* special built-in */
            "shift",		/* special built-in */
            "then",		/* reserved */
            "times",		/* special built-in */
            "trap",		/* special built-in */
            "unset",		/* special built-in */
            "until",		/* reserved */
            "while",		/* reserved */
    };

    private static final byte[] DUMMY_ARRAY = ByteList.NULL_ARRAY;

    private static void execFillarg(ThreadContext context, RubyString prog, IRubyObject[] argv, IRubyObject env, IRubyObject opthash, ExecArg eargp) {
        Ruby runtime = context.runtime;
        int argc = argv.length;

        if (!opthash.isNil()) {
            checkExecOptions(context, runtime, (RubyHash)opthash, eargp);
        }
        // add chdir if necessary
        String virtualCWD = runtime.getCurrentDirectory();
        if (!virtualCWD.equals(runtime.getPosix().getcwd())) {
            String arg = prog.toString();

            // if we're launching org.jruby.Main, adjust args to -C to new dir
            if ((arg = ShellLauncher.changeDirInsideJar(runtime, arg)) != null) {
                prog = newString(runtime, arg);
            } else if (virtualCWD.startsWith("uri:classloader:")) {
                // can't switch to uri:classloader URL, so just run in cwd
            } else if (!eargp.chdirGiven) {
                // only if :chdir is not specified
                eargp.chdirGiven = true;
                eargp.chdir_dir = virtualCWD;
            }
        }

        // restructure chdir plus command as call to sh with arguments
        if (eargp.chdirGiven && argc > 1) {
            argc = argc + SH_CHDIR_ARG_COUNT;

            IRubyObject[] newArgv = new IRubyObject[argc];

            newArgv[0] = newString(runtime, "sh");
            newArgv[1] = newString(runtime, "-c");
            newArgv[2] = newString(runtime, "cd -- \"$1\"; shift; exec \"$@\"");
            newArgv[3] = newString(runtime, "sh");
            newArgv[4] = newString(runtime, eargp.chdir_dir);

            System.arraycopy(argv, 0, newArgv, SH_CHDIR_ARG_COUNT, argv.length);

            argv = newArgv;

            prog = newString(runtime, "/bin/sh");

            eargp.chdirGiven = false;
        }

        if (!env.isNil()) {
            eargp.env_modification = checkExecEnv(context, (RubyHash) env, eargp);
        }

        prog = prog.export(context);
        // need to use shell
        eargp.use_shell = argc == 0 || eargp.chdirGiven;
        if (eargp.use_shell)
            eargp.command_name = prog;
        else
            eargp.command_name = prog;

        if (!Platform.IS_WINDOWS) {
            if (eargp.use_shell) {
                byte[] pBytes;

                boolean has_meta = searchForMetaChars(prog);

                if (!has_meta && !eargp.chdirGiven) {
                    /* avoid shell since no shell meta character found and no chdir needed. */
                    eargp.use_shell = false;
                }
                if (!eargp.use_shell) {
                    List<byte[]> argv_buf = new ArrayList<>();
                    pBytes = prog.getByteList().unsafeBytes();
                    int p = prog.getByteList().begin();
                    int pEnd = prog.getByteList().length() + p;
                    while (p < pEnd){
                        while (p < pEnd && (pBytes[p] == ' ' || pBytes[p] == '\t'))
                            p++;
                        if (p < pEnd){
                            int w = p;
                            while (p < pEnd && pBytes[p] != ' ' && pBytes[p] != '\t')
                                p++;
                            argv_buf.add(Arrays.copyOfRange(pBytes, w, p));
                            eargp.argv_buf = argv_buf;
                        }
                    }
                    if (argv_buf.size() > 0) {
                        eargp.command_name = RubyString.newStringNoCopy(runtime, argv_buf.get(0));
                    } else {
                        eargp.command_name = RubyString.newEmptyString(runtime); // empty command will get caught below shortly
                    }
                }
            }
        }

        // if not using shell to launch, validate and get abspath for command
        if (!eargp.use_shell) {
            String abspath;
            abspath = dlnFindExeR(runtime, eargp.command_name.toString(), eargp.path_env);
            if (abspath != null)
                eargp.command_abspath = StringSupport.checkEmbeddedNulls(runtime, newString(runtime, abspath));
            else
                eargp.command_abspath = null;
        }

        // if not using shell and we have not prepared arg list, do that now
        if (!eargp.use_shell && eargp.argv_buf == null) {
            int i;
            ArrayList<byte[]> argv_buf = new ArrayList<>(argc);
            for (i = 0; i < argc; i++) {
                IRubyObject arg = argv[i];
                RubyString argStr = StringSupport.checkEmbeddedNulls(runtime, arg);
                argStr = argStr.export(context);
                argv_buf.add(argStr.getBytes());
            }
            eargp.argv_buf = argv_buf;
        }

        // if not using shell, reassemble argv arguments as strings
        if (!eargp.use_shell) {
            ArgvStr argv_str = new ArgvStr();
            argv_str.argv = new String[eargp.argv_buf.size()];
            int i = 0;
            for (byte[] bytes : eargp.argv_buf) {
                argv_str.argv[i++] = new String(bytes);
            }
            eargp.argv_str = argv_str;
        }
    }

    /**
     * Search for meta characters in the command, to know whether we should use a shell to launch.
     *
     * meta characters:
     *
     * *    Pathname Expansion
     * ?    Pathname Expansion
     * {}   Grouping Commands
     * []   Pathname Expansion
     * <>   Redirection
     * ()   Grouping Commands
     * ~    Tilde Expansion
     * &    AND Lists, Asynchronous Lists
     * |    OR Lists, Pipelines
     * \    Escape Character
     * $    Parameter Expansion
     * ;    Sequential Lists
     * '    Single-Quotes
     * `    Command Substitution
     * "    Double-Quotes
     * \n   Lists
     *
     * #    Comment
     * =    Assignment preceding command name
     * %    (used in Parameter Expansion)
     */
    private static boolean searchForMetaChars(RubyString prog) {
        boolean has_meta = false;
        ByteList first = new ByteList(DUMMY_ARRAY, false);
        int p = 0;

        ByteList progByteList = prog.getByteList();
        byte[] pBytes = progByteList.unsafeBytes();

        for (; p < progByteList.length(); p++){
            if (progByteList.get(p) == ' ' || progByteList.get(p) == '\t'){
                if (first.unsafeBytes() != DUMMY_ARRAY && first.length() == 0) {
                    first.setRealSize(p - first.begin());
                }
            } else {
                if (first.unsafeBytes() == DUMMY_ARRAY) {
                    first.setUnsafeBytes(pBytes); first.setBegin(p + progByteList.begin());
                }
            }

            if (!has_meta && "*?{}[]<>()~&|\\$;'`\"\n#".indexOf(progByteList.get(p) & 0xFF) != -1) {
                has_meta = true;
            }

            if (first.length() == 0) {
                if (progByteList.get(p) == '='){
                    has_meta = true;
                } else if (progByteList.get(p) == '/'){
                    first.setRealSize(0x100); /* longer than any posix_sh_cmds */
                }
            }

            if (has_meta) {
                break;
            }
        }

        if (!has_meta && first.getUnsafeBytes() != DUMMY_ARRAY) {
            int length = first.length();

            if (length == 0) {
                first.setRealSize(p - first.getBegin());
            }

            if (length > 0 && length <= posix_sh_cmd_length &&
                Arrays.binarySearch(posix_sh_cmds, first.toString(), StringComparator.INSTANCE) >= 0) {
                has_meta = true;
            }
        }

        return has_meta;
    }

    private static final class StringComparator implements Comparator<String> {

        static final StringComparator INSTANCE = new StringComparator();

        public int compare(String o1, String o2) {
            int ret = o1.compareTo(o2);
            if (ret == 0 && o1.length() > o2.length()) return -1;
            return ret;
        }

    }

    private static String dlnFindExeR(Ruby runtime, String fname, IRubyObject path) {
        File exePath = ShellLauncher.findPathExecutable(runtime, fname, path);
        return exePath != null ? exePath.getAbsolutePath() : null;
    }

    private static class ArgvStr {
        String[] argv;
    }

    public static class ExecArg {
        boolean use_shell;
        RubyString command_name;
        RubyString command_abspath; /* full path string or nil */
        ArgvStr argv_str;
        List<byte[]> argv_buf;
        IRubyObject redirect_fds;
        String[] envp_str;
        List<String> envp_buf;
        run_exec_dup2_fd_pair[] dup2_tmpbuf;
        long pgroup_pgid = -1; /* asis(-1), new pgroup(0), specified pgroup (0<V). */
        IRubyObject rlimit_limits; /* null or [[rtype, softlim, hardlim], ...] */
        int umask_mask;
        int uid;
        int gid;
        RubyArray fd_dup2;
        RubyArray fd_close;
        RubyArray<RubyArray> fd_open;
        RubyArray fd_dup2_child;
        int close_others_maxhint;
        RubyArray env_modification; /* null or [[k1,v1], ...] */
        String chdir_dir;
        final List<SpawnFileAction> fileActions = new ArrayList();
        final List<SpawnAttribute> attributes = new ArrayList();
        IRubyObject path_env;

        boolean exception_given;
        boolean exception;
        boolean pgroupGiven;
        boolean umaskGiven;
        boolean unsetenvOthersGiven;
        boolean unsetenvOthersDo;
        boolean closeOthersGiven;
        boolean closeOthersDo;
        boolean chdirGiven;
        boolean newPgroupGiven;
        boolean newPgroupFlag;
        boolean uidGiven;
        boolean gidGiven;

    }

    private static final Comparator<run_exec_dup2_fd_pair> intcmp = new Comparator<run_exec_dup2_fd_pair>() {
        @Override
        public int compare(run_exec_dup2_fd_pair o1, run_exec_dup2_fd_pair o2) {
            return Integer.compare(o1.oldfd, o2.oldfd);
        }
    };

    private static final Comparator<run_exec_dup2_fd_pair> intrcmp = new Comparator<run_exec_dup2_fd_pair>() {
        @Override
        public int compare(run_exec_dup2_fd_pair o1, run_exec_dup2_fd_pair o2) {
            return Integer.compare(o2.oldfd, o1.oldfd);
        }
    };

}
