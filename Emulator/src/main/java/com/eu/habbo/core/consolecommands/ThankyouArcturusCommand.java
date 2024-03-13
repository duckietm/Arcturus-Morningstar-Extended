package com.eu.habbo.core.consolecommands;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ThankyouArcturusCommand extends ConsoleCommand {

    public ThankyouArcturusCommand() {
        super("thankyou", "A thankyou message.");
    }

    @Override
    public void handle(String[] args) throws Exception {
		log.info("Arcturus Morningstar is an opensource community fork of Arcturus Emulator by TheGeneral");
        log.info("Thankyou to the following people who have helped with it's development:");
        log.info("TheGeneral - For Creating Arcturus.");
        log.info("Capheus - Decompilation");
        log.info("Beny - Lead Developer");
        log.info("Alejandro - Lead Developer");
        log.info("Harmonic - Developer");
        log.info("ArpyAge - Developer");
        log.info("Mike - Developer");
        log.info("Skeletor - Developer");
        log.info("zGrav - Developer");
        log.info("Swirny - Developer");
        log.info("Quadral - Developer");
        log.info("Dome - Developer");
        log.info("Necmi - Developer");
        log.info("Oliver - Support");
        log.info("Rasmus - Support");
        log.info("Layne - Support");
        log.info("Bill - Support");
        log.info("Harmony - Support");
        log.info("Ridge - Catalogue");
        log.info("Tenshie - Catalogue");
        log.info("Wulles - Catalogue");
        log.info("Gizmo - Catalogue");
        log.info("TheJava - Motivation");
        log.info("The Entire Krews.org Community.");
        }
    }