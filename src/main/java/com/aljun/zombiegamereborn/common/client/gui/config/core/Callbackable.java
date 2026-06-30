package com.aljun.zombiegamereborn.common.client.gui.config.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface Callbackable<T> {
    Screen getLastScreen();
    default void backAndSave(T t) {
        this.getCallBack().accept(t);
        Minecraft.getInstance().setScreen(this.getLastScreen());
    }

    default void backAndCancel() {
        if (this.getLastScreen() != null) {
            Minecraft.getInstance().setScreen(this.getLastScreen());
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    Consumer<T> getCallBack();


}
