package dev.qilletni.toolchain.debug;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.util.concurrent.CompletableFuture;

/**
 * The Qilletni-specific requests in {@code docs/debugger-protocol.md}, served alongside the DAP ones.
 */
public interface QilletniDebugServer {

    /**
     * The songs a list, collection or album weight entry picks from. Answered while the program runs, without loading
     * music.
     */
    @JsonRequest("qilletni/weightEntrySongs")
    CompletableFuture<WeightEntrySongsResponse> weightEntrySongs(WeightEntrySongsArguments args);
}
