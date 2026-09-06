package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PointsCommandAliasContractTest {
    @Test
    void givepointsUsesPuntiCurrency103() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/PointsCommand.java"));

        int alias = source.indexOf("equalsIgnoreCase(\"givepoints\")");
        int fixedType = source.indexOf("fixedPuntiCommand ? 103", alias);
        int grant = source.indexOf("habbo.givePoints(type, amount)", fixedType);

        assertTrue(alias > -1, ":givepoints must have dedicated behavior");
        assertTrue(fixedType > alias, ":givepoints must select currency 103");
        assertTrue(grant > fixedType, "the selected Punti currency must reach the wallet grant");
    }
}
