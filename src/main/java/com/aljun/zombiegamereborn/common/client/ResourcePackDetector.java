package com.aljun.zombiegamereborn.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ResourcePackDetector {
    private static final List<ResourcePackInfo> resourcePackInfos = new ArrayList<>();

    public ResourcePackInfo TZP = new ResourcePackInfo(
            "tissou_zombie_pack",        // 内部标识，不参与匹配
            "Zombie Pack"           // 与 pack.mcmeta description 匹配
    );

    public static void refresh() {
        Minecraft mc = Minecraft.getInstance();

        var selectedPacks = mc.getResourcePackRepository().getSelectedPacks();

        for (ResourcePackInfo info : resourcePackInfos) {
            info.isLoaded = false;
            for (var pack : selectedPacks) {
                try (PackResources resources = pack.open()) {
                    PackMetadataSection meta = resources.getMetadataSection(PackMetadataSection.TYPE);
                    if (meta != null && meta.getDescription().getString().contains(info.keyword)) {
                        info.isLoaded = true;
                        break;
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static class ResourcePackInfo {
        public final String id;
        private final String keyword;
        private boolean isLoaded = false;

        public ResourcePackInfo(String id, String keyword) {
            this.id = id;
            this.keyword = keyword;
            resourcePackInfos.add(this);
        }

        public boolean isLoaded() {
            return isLoaded;
        }
    }
}
