package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TornadoHeightRestoreContractTest {
    @Test
    void restoresEveryFurniToItsExactCapturedHeight() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/TrashCommand.java"));

        int restore = source.indexOf("private static void restoreTornado(");
        int heightFunction = source.indexOf("private static double tornadoHeight(", restore);
        String restoreBody = source.substring(restore, heightFunction);

        assertTrue(restoreBody.contains("new RemoveFloorItemComposer(captured.item(), true)"));
        assertTrue(restoreBody.contains("new AddFloorItemComposer("));
        assertFalse(restoreBody.contains("new FloorItemOnRollerComposer("));
    }
}
