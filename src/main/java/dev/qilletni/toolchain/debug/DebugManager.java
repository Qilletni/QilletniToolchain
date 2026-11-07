package dev.qilletni.toolchain.debug;

import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.Thread;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugManager implements IDebugProtocolServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugManager.class);

    /**
     * How long to wait before exiting when the IDE ends the session while the program runs.
     */
    private static final long EXIT_DELAY_MS = 200;
    
    private IDebugProtocolClient client;
    private InitializeRequestArguments initializedArgs;
    
    private Socket socket;

    /**
     * Completes with {@code true} on {@code configurationDone}, or {@code false} if the session ends first. Only the
     * first completion counts, so a late {@code configurationDone} can't start an aborted program.
     */
    private final CompletableFuture<Boolean> configured = new CompletableFuture<>();

    /**
     * Set once the session is over, either by {@link #close(int)} or by the IDE going away, so the end is only
     * handled once. It also stops our own socket close from looking like the IDE disconnecting.
     */
    private final AtomicBoolean sessionOver = new AtomicBoolean();

    private final int debugPort;

    public DebugManager(int debugPort) {
        this.debugPort = debugPort;
    }

    /**
     * Connects to the IDE and starts handling DAP requests in the background. Returns as soon as the connection is
     * made; use {@link #awaitConfigured()} before running any Qilletni code.
     */
    public void connect() throws IOException {
        socket = new Socket("127.0.0.1", debugPort);

        // Daemon threads, so a stuck connection never keeps the JVM alive
        var executor = Executors.newCachedThreadPool(runnable -> {
            var thread = new Thread(runnable, "qilletni-dap");
            thread.setDaemon(true);
            return thread;
        });

        Launcher<IDebugProtocolClient> launcher = DSPLauncher.createServerLauncher(this, socket.getInputStream(), socket.getOutputStream(), executor, null);
        client = launcher.getRemoteProxy();

        var listening = launcher.startListening();
        LOGGER.info("Connected to debugger on port {}", debugPort);

        Thread.ofPlatform().daemon().name("qilletni-dap-watcher").start(() -> {
            try {
                listening.get();
            } catch (ExecutionException e) {
                LOGGER.debug("Debug connection failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            sessionEnded("debugger connection closed");
        });
    }

    /**
     * Blocks until the IDE has sent its breakpoints and {@code configurationDone}.
     *
     * @return {@code true} if the program should run, {@code false} if the IDE went away first
     */
    public boolean awaitConfigured() {
        LOGGER.debug("Waiting for the debugger to finish configuration...");
        var result = configured.join();
        LOGGER.debug("Debugger configuration finished, running: {}", result);
        return result;
    }

    /**
     * Reports the end of the program to the IDE and closes the connection. Safe to call in any state, including when
     * {@link #connect()} never ran or failed.
     */
    public void close(int exitCode) {
        sessionOver.set(true);

        if (client != null) {
            try {
                var exited = new ExitedEventArguments();
                exited.setExitCode(exitCode);

                client.terminated(new TerminatedEventArguments());
                client.exited(exited);
            } catch (RuntimeException e) {
                LOGGER.debug("Unable to send exit events, the debugger is already gone", e);
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.debug("Unable to close debug socket", e);
            }
        }
    }

    /**
     * Handles the IDE ending the session (disconnect request or dropped socket). Before configuration that just
     * releases {@link #awaitConfigured()} with {@code false}; afterwards the program is already running and must end.
     */
    private void sessionEnded(String reason) {
        if (!sessionOver.compareAndSet(false, true)) {
            return;
        }

        if (configured.complete(false)) {
            LOGGER.info("Debug session ended before the program started: {}", reason);
            return;
        }

        endRunningProgram(reason);
    }

    /**
     * Called at most once, on an lsp4j thread, when the IDE disconnects or the socket drops while the program runs.
     * For a {@code disconnect} request, the response hasn't been written yet when this is called.
     */
    private void endRunningProgram(String reason) {
        LOGGER.info("Ending program: {}", reason);

        // Exit from a separate thread after a short delay, so a disconnect response is written first. System.exit
        // (not halt) lets Log4j's shutdown hook flush the log socket. CommandRun's finally doesn't run, but nothing
        // there needs to: provider shutdown only stops thread pools, and the IDE is no longer listening for events.
        Thread.ofPlatform().daemon().name("qilletni-dap-exit").start(() -> {
            try {
                Thread.sleep(EXIT_DELAY_MS);
            } catch (InterruptedException ignored) {
                // Exit right away instead
            }

            System.exit(1);
        });
    }

    @Override
    public CompletableFuture<Void> cancel(CancelArguments args) {
        return IDebugProtocolServer.super.cancel(args);
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        LOGGER.info("Got initialize request");
        
        initializedArgs = args;
        
        var capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(true);
//        capabilities.setSupportsEvaluateForHovers(true);
        
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        LOGGER.debug("Configuration is done, notifying...");
        configured.complete(true);
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        LOGGER.debug("launch() args: {}", args);
        
        double qilletniProtocol = Double.parseDouble(args.getOrDefault("qilletniProtocol", "0.0").toString());
        
        // Stop on first statement of first file. Ignore for now.
        var stopOnEntry = Boolean.valueOf(args.getOrDefault("stopOnEntry", "false").toString());
        
        if (qilletniProtocol != 1.0) {
            LOGGER.error("Got invalid qilletniProtocol: {}", qilletniProtocol);
            return CompletableFuture.failedFuture(new ResponseErrorException(new ResponseError(ResponseErrorCode.InvalidParams, "Invalid qilletniProtocol", null)));
        }

        client.initialized();
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return IDebugProtocolServer.super.attach(args);
    }

    @Override
    public CompletableFuture<Void> restart(RestartArguments args) {
        return IDebugProtocolServer.super.restart(args);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        LOGGER.info("Debugger requested disconnect");
        sessionEnded("disconnect requested");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return IDebugProtocolServer.super.terminate(args);
    }

    @Override
    public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
        return IDebugProtocolServer.super.breakpointLocations(args);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return IDebugProtocolServer.super.setBreakpoints(args);
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return IDebugProtocolServer.super.setFunctionBreakpoints(args);
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return IDebugProtocolServer.super.setExceptionBreakpoints(args);
    }

    @Override
    public CompletableFuture<DataBreakpointInfoResponse> dataBreakpointInfo(DataBreakpointInfoArguments args) {
        return IDebugProtocolServer.super.dataBreakpointInfo(args);
    }

    @Override
    public CompletableFuture<SetDataBreakpointsResponse> setDataBreakpoints(SetDataBreakpointsArguments args) {
        return IDebugProtocolServer.super.setDataBreakpoints(args);
    }

    @Override
    public CompletableFuture<SetInstructionBreakpointsResponse> setInstructionBreakpoints(SetInstructionBreakpointsArguments args) {
        return IDebugProtocolServer.super.setInstructionBreakpoints(args);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return IDebugProtocolServer.super.continue_(args);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return IDebugProtocolServer.super.next(args);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return IDebugProtocolServer.super.stepIn(args);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return IDebugProtocolServer.super.stepOut(args);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        return IDebugProtocolServer.super.stepBack(args);
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        return IDebugProtocolServer.super.reverseContinue(args);
    }

    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        return IDebugProtocolServer.super.restartFrame(args);
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        return IDebugProtocolServer.super.goto_(args);
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return IDebugProtocolServer.super.pause(args);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return IDebugProtocolServer.super.stackTrace(args);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return IDebugProtocolServer.super.scopes(args);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return IDebugProtocolServer.super.variables(args);
    }

    @Override
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        return IDebugProtocolServer.super.setVariable(args);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return IDebugProtocolServer.super.source(args);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return IDebugProtocolServer.super.threads();
    }

    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        return IDebugProtocolServer.super.terminateThreads(args);
    }

    @Override
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        return IDebugProtocolServer.super.modules(args);
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        return IDebugProtocolServer.super.loadedSources(args);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return IDebugProtocolServer.super.evaluate(args);
    }

    @Override
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        return IDebugProtocolServer.super.setExpression(args);
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        return IDebugProtocolServer.super.stepInTargets(args);
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        return IDebugProtocolServer.super.gotoTargets(args);
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return IDebugProtocolServer.super.completions(args);
    }

    @Override
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        return IDebugProtocolServer.super.exceptionInfo(args);
    }

    @Override
    public CompletableFuture<ReadMemoryResponse> readMemory(ReadMemoryArguments args) {
        return IDebugProtocolServer.super.readMemory(args);
    }

    @Override
    public CompletableFuture<WriteMemoryResponse> writeMemory(WriteMemoryArguments args) {
        return IDebugProtocolServer.super.writeMemory(args);
    }

    @Override
    public CompletableFuture<DisassembleResponse> disassemble(DisassembleArguments args) {
        return IDebugProtocolServer.super.disassemble(args);
    }

    @Override
    public CompletableFuture<LocationsResponse> locations(LocationsArguments args) {
        return IDebugProtocolServer.super.locations(args);
    }
}
