package com.eu.habbo.messages.incoming.catalog.catalogadmin.studio;

import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeGroup;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogConcurrentModificationException;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogUndoConflictException;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioChangedEntity;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioUndoComposer;
import java.util.List;

/**
 * Undo of one history group. Every refusal (stale revision, a later edit of the same entity, a missing group, an SQL
 * failure) is answered as a failed {@link CatalogStudioUndoComposer}: an escaped exception used to leave the studio
 * on "loading" with every Annulla button disabled â€” which is what "undo doesn't work" looked like.
 */
public final class CatalogStudioUndoEvent extends CatalogStudioEvent {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogStudioUndoEvent.class);

    @Override
    public void handle() {
        if (!authorize()) return;
        CatalogStudioUndoRequest request = CatalogStudioRequestParser.parseUndo(this.packet);

        try {
            CatalogChangeGroup group = studio().queries().loadChangeGroup(request.groupId());
            long revision = studio().liveUndo()
                    .undo(request.groupId(), actorId(), request.expectedRevision(), request.operationId());
            this.client.sendResponse(new CatalogStudioUndoComposer(
                    request.operationId(),
                    true,
                    "CHANGE_UNDONE",
                    "Operazione annullata",
                    revision,
                    group.entries().stream()
                            .map(entry -> new CatalogStudioChangedEntity(
                                    entry.entityType().name(), entry.entityId()))
                            .distinct()
                            .toList()));
        } catch (RuntimeException exception) {
            String code;
            String message;
            if (exception instanceof CatalogConcurrentModificationException) {
                code = "STALE_REVISION";
                message = "Il catalogo Ã¨ cambiato nel frattempo: ricaricato, riprova ad annullare.";
            } else if (exception instanceof CatalogUndoConflictException) {
                code = "UNDO_CONFLICT";
                message = exception.getMessage();
            } else {
                code = "UNDO_FAILED";
                message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            }
            LOGGER.warn("Catalog undo of group {} rejected ({}): {}", request.groupId(), code, message);

            long revision = request.expectedRevision();
            try {
                revision = studio().queries().loadSession().revision();
            } catch (RuntimeException ignored) {
                // the request's own revision is a fine fallback for the client
            }
            this.client.sendResponse(new CatalogStudioUndoComposer(
                    request.operationId(), false, code, message, revision, List.of()));
        }
    }
}
