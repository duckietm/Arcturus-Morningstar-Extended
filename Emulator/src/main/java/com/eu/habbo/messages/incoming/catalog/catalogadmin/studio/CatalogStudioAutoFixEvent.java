package com.eu.habbo.messages.incoming.catalog.catalogadmin.studio;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogAutoFixPlanner;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeSetApplyResult;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogDraftValidationResult;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogImportDryRun;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogValidationIssue;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogValidationReferenceData;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogVersionSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.JdbcCatalogValidationDataRepository;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioDocumentResultComposer;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioValidationComposer;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioValidationIssue;
import java.sql.Connection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog Studio auto-fix (CUSTOM packet 10087): validate the live catalog, plan repairs server-side with the full
 * snapshot and reference data, apply them through the normal change-set path (journal + undo), repeat until clean
 * or no repair is left, then answer with a document result and the fresh validation report.
 */
public final class CatalogStudioAutoFixEvent extends CatalogStudioEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogStudioAutoFixEvent.class);
    private static final int MAX_PASSES = 4;

    @Override
    public void handle() {
        if (!authorize()) return;
        CatalogStudioRevisionRequest request = CatalogStudioRequestParser.parseRevision(this.packet);
        int actorId = actorId();
        Emulator.getThreading().run(() -> this.autoFix(request.operationId(), actorId));
    }

    private void autoFix(String operationId, int actorId) {
        int changedEntities = 0;
        int fixedIssues = 0;
        int passes = 0;
        long revision = 0;
        String failure = null;
        StringBuilder appliedSql = new StringBuilder();
        CatalogDraftValidationResult validation = null;

        try {
            for (int pass = 1; pass <= MAX_PASSES; pass++) {
                validation = studio().liveMutations().validateLive();
                revision = validation.revision();
                List<CatalogValidationIssue> issues = validation.report().issues();
                if (issues.isEmpty()) break;

                CatalogVersionSnapshot live = studio().liveMutations().loadLive();
                CatalogValidationReferenceData reference;
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                    reference = new JdbcCatalogValidationDataRepository().load(connection);
                } catch (java.sql.SQLException exception) {
                    throw new IllegalStateException("Catalog validation reference data could not be loaded", exception);
                }

                CatalogAutoFixPlanner.Plan plan = CatalogAutoFixPlanner.plan(live, issues, reference);
                if (plan.statements().isEmpty()) break;

                String document = String.join("\n", plan.statements());
                CatalogImportDryRun dryRun = studio().liveChangeSets().dryRun("SQL", document);
                if (dryRun.changes().isEmpty()) break;

                CatalogChangeSetApplyResult result = studio().liveChangeSets().apply(
                        operationId + "-p" + pass,
                        actorId,
                        "SQL",
                        dryRun.normalizedDocument(),
                        dryRun.fingerprint(),
                        "Auto-fix catalogo (passaggio " + pass + "): " + plan.describe());

                passes++;
                changedEntities += result.changedEntities();
                fixedIssues += plan.issuesAddressed();
                revision = result.revision();
                if (appliedSql.length() > 0) appliedSql.append('\n');
                appliedSql.append(document);
            }

            if (validation == null || passes > 0) validation = studio().liveMutations().validateLive();
            revision = validation.revision();
        } catch (RuntimeException exception) {
            LOGGER.error("[CatalogStudio] auto-fix failed", exception);
            failure = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            try {
                validation = studio().liveMutations().validateLive();
                revision = validation.revision();
            } catch (RuntimeException ignored) {
                // keep the last known report
            }
        }

        int remaining = validation == null ? 0 : validation.report().issues().size();
        String code;
        String message;
        if (failure != null) {
            code = "AUTO_FIX_FAILED";
            message = "Auto-fix interrotto: " + failure + (fixedIssues > 0 ? " (" + fixedIssues + " problemi già corretti)" : "");
        } else if (passes == 0) {
            code = "AUTO_FIX_NOTHING";
            message = remaining == 0 ? "Il catalogo non ha problemi." : "Nessuna correzione automatica disponibile per i " + remaining + " problemi rimasti.";
        } else {
            code = "AUTO_FIX_APPLIED";
            message = "Corretti " + fixedIssues + " problemi in " + passes + (passes == 1 ? " passaggio" : " passaggi")
                    + " (" + changedEntities + " modifiche, annullabili dalla cronologia)"
                    + (remaining > 0 ? "; restano " + remaining + " problemi non riparabili automaticamente." : ". Catalogo pulito.");
        }

        if (!this.client.getChannel().isActive()) return;

        this.client.sendResponse(new CatalogStudioDocumentResultComposer(
                operationId, failure == null, code, message, revision, "SQL", appliedSql.toString(), "", changedEntities));

        if (validation != null) {
            boolean valid = validation.report().valid();
            this.client.sendResponse(new CatalogStudioValidationComposer(
                    operationId,
                    valid,
                    valid ? "VALID" : "VALIDATION_FAILED",
                    valid ? "Live catalog is valid" : validation.report().issues().size() + " live catalog issues found",
                    validation.revision(),
                    true,
                    validation.report().issues().stream()
                            .map(issue -> new CatalogStudioValidationIssue(
                                    issue.code(), issue.entityType().name(), issue.entityId(), issue.field(), issue.message()))
                            .toList()));
        }
    }
}
