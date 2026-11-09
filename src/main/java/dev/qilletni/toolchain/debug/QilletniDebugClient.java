package dev.qilletni.toolchain.debug;

import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * The DAP client, plus the Qilletni-specific events in {@code docs/debugger-protocol.md}.
 */
public interface QilletniDebugClient extends IDebugProtocolClient {

    /**
     * A weighted pick was made. Only sent when the client launched with {@code weightEvents: true}.
     */
    @JsonNotification("qilletni/weightPicked")
    void weightPicked(WeightPickedEventArguments args);
}
