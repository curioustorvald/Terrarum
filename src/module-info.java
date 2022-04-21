module Terrarum {
    // java
    requires java.base;
    requires java.desktop;
    requires java.logging;
    requires jdk.unsupported; // sun.misc.Unsafe

    // kotlin
    requires kotlin.stdlib;
    requires kotlin.test;
    requires kotlin.reflect;
    requires kotlin.stdlib.jdk8; // for some reason it's required

    // gdx
    requires gdx;
    requires gdx.platform;
    requires gdx.backend.lwjgl3;
    requires gdx.controllers.core;
    requires gdx.controllers.desktop;

    // terrarum
    requires TerrarumSansBitmap;
    requires Terrarum.Joise;

    // etc
    requires GetCpuName;
    requires org.apache.commons.codec;
    requires commons.csv;
    requires jxinput;
    requires org.graalvm.sdk;
    requires com.ibm.icu;
    requires org.lwjgl.opengl;
    requires prtree;


    exports net.torvald.colourutil;
    exports net.torvald.gdx.graphics;
    exports net.torvald.random;
    exports net.torvald.spriteanimation;
    exports net.torvald.util;
    exports net.torvald.unicode;

    exports net.torvald.terrarum;
    exports net.torvald.terrarum.blockproperties;
    exports net.torvald.terrarum.concurrent;
    exports net.torvald.terrarum.console;
    exports net.torvald.terrarum.gameactors;
    exports net.torvald.terrarum.gamecontroller;
    exports net.torvald.terrarum.gameitems;
    exports net.torvald.terrarum.gameparticles;
    exports net.torvald.terrarum.gameworld;
    exports net.torvald.terrarum.imagefont;
    exports net.torvald.terrarum.itemproperties;
    exports net.torvald.terrarum.langpack;
    exports net.torvald.terrarum.realestate;
    exports net.torvald.terrarum.ui;
    exports net.torvald.terrarum.utils;
    exports net.torvald.terrarum.weather;

    exports net.torvald.terrarum.modulebasegame;
    exports net.torvald.terrarum.modulebasegame.gameactors;
    exports net.torvald.terrarum.modulebasegame.gameitems;
    exports net.torvald.terrarum.modulebasegame.gameparticles;
    exports net.torvald.terrarum.modulebasegame.gameworld;
    exports net.torvald.terrarum.modulebasegame.ui;
    exports net.torvald.terrarum.modulebasegame.worldgenerator;
}