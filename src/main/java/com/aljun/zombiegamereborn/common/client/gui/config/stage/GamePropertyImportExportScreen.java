package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class GamePropertyImportExportScreen extends Screen {

    private static final int ITEM_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int MIN_SCROLLBAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 40;
    private static final int LIST_START_Y = 50;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_SPACING = 5;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<FileEntry> fileEntries = new ArrayList<>();
    private final JsonObject currentConfig;
    private final Consumer<JsonObject> onLoadCallback;
    private final Screen lastScreen;

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartMouseY = 0;
    private int dragStartScrollOffset = 0;
    private int currentMouseY = 0;

    private Button cancelButton;
    private Button loadButton;
    private Button exportButton;
    private Button openFolderButton;
    private Button renameButton;
    private Button refreshButton;
    private Button importButton;

    public GamePropertyImportExportScreen(
            JsonObject currentConfig,
            Consumer<JsonObject> onLoadCallback,
            Screen lastScreen
    ) {
        super(Component.translatable("gui.zombiegamereborn.gameproperty.import_export_title"));
        this.currentConfig = currentConfig;
        this.onLoadCallback = onLoadCallback;
        this.lastScreen = lastScreen;
        refreshFileList();
    }

    private void refreshFileList() {
        fileEntries.clear();
        Path dir = getPresetDirectory();
        try {
            Files.createDirectories(dir);
            List<Path> jsonFiles;
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                jsonFiles = stream.filter(path -> path.toString().endsWith(".json"))
                        .sorted()
                        .toList();
            }
            for (Path path : jsonFiles) {
                String fileName = path.getFileName().toString();
                fileName = fileName.substring(0, fileName.length() - ".json".length());
                long lastModified = Files.getLastModifiedTime(path).toMillis();
                boolean valid = false;
                JsonObject json = null;
                try (Reader reader = Files.newBufferedReader(path)) {
                    json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null && isValidGameProperty(json)) {
                        valid = true;
                    }
                } catch (Exception ignored) {
                }
                fileEntries.add(new FileEntry(fileName, json, path, lastModified, valid));
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isValidGameProperty(JsonObject json) {
        return json.has("stage_properties") || json.has("can_zombie_break_block");
    }

    static Path getPresetDirectory() {
        return Path.of("config", ZombieGameReborn.MOD_ID, "game_properties");
    }

    private static Path resolveDedupPath(String baseName) {
        Path dir = getPresetDirectory();
        Path path = dir.resolve(baseName + ".json");
        if (!Files.exists(path)) {
            return path;
        }
        int counter = 2;
        while (true) {
            path = dir.resolve(baseName + " (" + counter + ").json");
            if (!Files.exists(path)) {
                return path;
            }
            counter++;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseY = mouseY;
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title.getString(), 10, 20, 0xFFFFFF);

        renderListArea(guiGraphics, mouseX, mouseY, partialTick);
        renderScrollbar(guiGraphics, mouseX, mouseY, partialTick);

        if (selectedIndex >= 0 && selectedIndex < fileEntries.size()) {
            String selectedText = I18n.get("gui.zombiegamereborn.core.selected_prefix") + fileEntries.get(selectedIndex).name;
            guiGraphics.drawString(this.font, selectedText, 10, this.height - BOTTOM_BAR_HEIGHT - 15, 0xAAAAAA);
        }
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null) {
            this.width = this.minecraft.getWindow().getGuiScaledWidth();
            this.height = this.minecraft.getWindow().getGuiScaledHeight();
        }

        int buttonY = this.height - BOTTOM_BAR_HEIGHT + 10;

        if (this.cancelButton != null) {
            this.removeWidget(this.cancelButton);
            this.removeWidget(this.loadButton);
            this.removeWidget(this.exportButton);
            this.removeWidget(this.openFolderButton);
            this.removeWidget(this.renameButton);
            this.removeWidget(this.refreshButton);
        }

        int padding = 10;
        this.cancelButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.core.cancel"),
                btn -> onCancel()
        ).bounds(padding, buttonY, BUTTON_WIDTH, 20).build();

        int centerButtonCount = 4;
        int centerGroupWidth = centerButtonCount * BUTTON_WIDTH + (centerButtonCount - 1) * BUTTON_SPACING;
        int centerX = this.width / 2;
        int centerGroupStartX = centerX - centerGroupWidth / 2;

        this.exportButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.gameproperty.export"),
                btn -> onUpload()
        ).bounds(centerGroupStartX, buttonY, BUTTON_WIDTH, 20).build();

        this.openFolderButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.gameproperty.open_folder"),
                btn -> onOpenFolder()
        ).bounds(centerGroupStartX + (BUTTON_WIDTH + BUTTON_SPACING), buttonY, BUTTON_WIDTH, 20).build();

        this.renameButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.gameproperty.rename"),
                btn -> onRename()
        ).bounds(centerGroupStartX + (BUTTON_WIDTH + BUTTON_SPACING) * 2, buttonY, BUTTON_WIDTH, 20).build();

        this.refreshButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.gameproperty.refresh"),
                btn -> onRefresh()
        ).bounds(centerGroupStartX + (BUTTON_WIDTH + BUTTON_SPACING) * 3, buttonY, BUTTON_WIDTH, 20).build();

        int rightPadding = 10;
        this.loadButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.gameproperty.load"),
                btn -> onLoad()
        ).bounds(this.width - rightPadding - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, 20).build();

        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(exportButton);
        this.addRenderableWidget(openFolderButton);
        this.addRenderableWidget(renameButton);
        this.addRenderableWidget(refreshButton);
        this.addRenderableWidget(loadButton);

        updateLoadButtonState();
        updateMaxScrollOffset();

        if (selectedIndex >= fileEntries.size()) {
            selectedIndex = fileEntries.size() - 1;
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        if (cancelButton != null) {
            init();
        }
        updateMaxScrollOffset();
    }

    private void updateMaxScrollOffset() {
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int contentHeight = fileEntries.size() * ITEM_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - listHeight);
    }

    private void onCancel() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    private void onLoad() {
        if (selectedIndex < 0 || selectedIndex >= fileEntries.size()) {
            return;
        }
        FileEntry entry = fileEntries.get(selectedIndex);
        if (!entry.valid) {
            return;
        }
        onLoadCallback.accept(entry.content);
        Minecraft.getInstance().setScreen(lastScreen);
    }

    private void onUpload() {
        Minecraft.getInstance().setScreen(new ExportNameScreen(this, currentConfig, name -> {
            Path savePath = resolveDedupPath(name);
            try {
                Files.createDirectories(savePath.getParent());
                try (Writer writer = Files.newBufferedWriter(savePath)) {
                    GSON.toJson(currentConfig, writer);
                }
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("gui.zombiegamereborn.gameproperty.export_success"), false
                    );
                }
                refreshFileList();
                selectedIndex = -1;
                updateMaxScrollOffset();
                Minecraft.getInstance().setScreen(this);
            } catch (Exception e) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("gui.zombiegamereborn.gameproperty.export_error"), false
                    );
                }
                Minecraft.getInstance().setScreen(this);
            }
        }));
    }

    private void onImport() {
        String originalHeadless = System.setProperty("java.awt.headless", "false");
        try {
            java.awt.Frame frame = new java.awt.Frame();
            frame.setUndecorated(true);
            frame.setVisible(true);
            frame.toFront();

            java.awt.FileDialog dialog = new java.awt.FileDialog(
                    frame,
                    I18n.get("gui.zombiegamereborn.gameproperty.import_dialog_title"),
                    java.awt.FileDialog.LOAD
            );
            dialog.setFilenameFilter((dir, name) -> name.endsWith(".json"));
            dialog.setVisible(true);

            String fileName = dialog.getFile();
            String dirPath = dialog.getDirectory();
            dialog.dispose();
            frame.dispose();

            if (fileName == null) return;

            Path sourcePath = Path.of(dirPath, fileName);
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(sourcePath)) {
                json = GSON.fromJson(reader, JsonObject.class);
            }
            if (json == null || !isValidGameProperty(json)) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("gui.zombiegamereborn.gameproperty.import_invalid"), false
                    );
                }
                return;
            }

            // Copy to preset directory
            Path destPath = getPresetDirectory().resolve(
                    fileName.endsWith(".json") ? fileName : fileName + ".json");
            Files.createDirectories(destPath.getParent());
            try (Writer writer = Files.newBufferedWriter(destPath)) {
                GSON.toJson(json, writer);
            }

            refreshFileList();
            onLoadCallback.accept(json);
            Minecraft.getInstance().setScreen(lastScreen);
        } catch (Exception e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.gameproperty.import_error"), false
                );
            }
        } finally {
            String prev = originalHeadless;
            if (prev == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", prev);
            }
        }
    }

    private void onOpenFolder() {
        try {
            Path dir = getPresetDirectory();
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            String[] cmd;
            if (os.contains("win")) {
                cmd = new String[]{"explorer.exe", dir.toAbsolutePath().toString()};
            } else if (os.contains("mac")) {
                cmd = new String[]{"open", dir.toAbsolutePath().toString()};
            } else {
                cmd = new String[]{"xdg-open", dir.toAbsolutePath().toString()};
            }
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ignored) {
        }
    }

    private void onRefresh() {
        refreshFileList();
        selectedIndex = -1;
        updateLoadButtonState();
        updateMaxScrollOffset();
    }

    private void onRename() {
        if (selectedIndex < 0 || selectedIndex >= fileEntries.size()) {
            return;
        }
        FileEntry entry = fileEntries.get(selectedIndex);
        Minecraft.getInstance().setScreen(new RenameScreen(this, entry.name, newName -> {
            Path oldPath = entry.path;
            Path newPath = oldPath.resolveSibling(newName + ".json");
            try {
                Files.move(oldPath, newPath);
                refreshFileList();
                for (int i = 0; i < fileEntries.size(); i++) {
                    if (fileEntries.get(i).name.equals(newName)) {
                        selectedIndex = i;
                        break;
                    }
                }
                updateLoadButtonState();
                updateMaxScrollOffset();
                Minecraft.getInstance().setScreen(this);
            } catch (Exception e) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("gui.zombiegamereborn.gameproperty.rename_error"), false
                    );
                }
                Minecraft.getInstance().setScreen(this);
            }
        }));
    }

    private void updateLoadButtonState() {
        if (loadButton != null) {
            loadButton.active = selectedIndex >= 0 && selectedIndex < fileEntries.size()
                    && fileEntries.get(selectedIndex).valid;
        }
    }

    private void renderListArea(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;

        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + listHeight, 0x30000000);
        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + 1, 0xFF666666);
        guiGraphics.fill(15, LIST_START_Y + listHeight - 1, this.width - 15, LIST_START_Y + listHeight, 0xFF666666);

        for (int i = 0; i < fileEntries.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;

            if (itemY >= LIST_START_Y && itemY + ITEM_HEIGHT <= LIST_START_Y + listHeight) {
                boolean isSelected = (i == selectedIndex);

                if (isSelected) {
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + ITEM_HEIGHT,
                            fileEntries.get(i).valid ? 0x404444FF : 0x44FF4444);
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + 1,
                            fileEntries.get(i).valid ? 0xFF4444FF : 0xFFFF4444);
                    guiGraphics.fill(16, itemY + ITEM_HEIGHT - 1, this.width - 16, itemY + ITEM_HEIGHT,
                            fileEntries.get(i).valid ? 0xFF4444FF : 0xFFFF4444);
                }

                FileEntry entry = fileEntries.get(i);
                String timeStr = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(entry.lastModified),
                        ZoneId.systemDefault()
                ).format(DATE_FORMATTER);
                String timeText = "§7" + timeStr;
                String displayText = entry.valid ? entry.name : (entry.name + " §c[错误]");

                int nameColor = entry.valid ? (isSelected ? 0xFFFFAA : 0xDDDDDD) : (isSelected ? 0xFFAA55 : 0xFF7777);
                guiGraphics.drawString(this.font, displayText, 25, itemY + 7, nameColor);
                guiGraphics.drawString(this.font, timeText,
                        this.width - 25 - this.font.width(timeText), itemY + 7,
                        isSelected ? 0xAAAAAA : 0x888888);
            }
        }

        if (fileEntries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.core.list_empty"),
                    this.width / 2, LIST_START_Y + 50, 0x888888);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (maxScrollOffset <= 0) return;

        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;

        float scrollRatio = (float) scrollOffset / maxScrollOffset;
        int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                (int) (listHeight * listHeight / (float) (fileEntries.size() * ITEM_HEIGHT)));
        int availableTrackHeight = listHeight - scrollbarThumbHeight;
        int scrollbarThumbY = LIST_START_Y + (int) (availableTrackHeight * scrollRatio);

        guiGraphics.fill(scrollbarX, LIST_START_Y, scrollbarX + SCROLLBAR_WIDTH,
                LIST_START_Y + listHeight, 0x40000000);

        boolean isHovered = isMouseOverScrollbar(mouseX, mouseY, scrollbarX, scrollbarThumbY, scrollbarThumbHeight);
        int thumbColor = isDraggingScrollbar ? 0xFF888888 : (isHovered ? 0xFFAAAAAA : 0xFF666666);
        guiGraphics.fill(scrollbarX + 2, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 2,
                scrollbarThumbY + scrollbarThumbHeight, thumbColor);
        guiGraphics.fill(scrollbarX + 3, scrollbarThumbY + 1, scrollbarX + SCROLLBAR_WIDTH - 3,
                scrollbarThumbY + scrollbarThumbHeight - 1, 0xFFFFFFFF);
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY, int scrollbarX, int scrollbarThumbY, int scrollbarThumbHeight) {
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (maxScrollOffset > 0) {
            int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                    (int) (listHeight * listHeight / (float) (fileEntries.size() * ITEM_HEIGHT)));
            float trackAvailableHeight = listHeight - scrollbarThumbHeight;
            int thumbY = LIST_START_Y + (int) ((scrollOffset / (float) maxScrollOffset) * trackAvailableHeight);

            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
                    mouseY >= thumbY && mouseY < thumbY + scrollbarThumbHeight) {
                isDraggingScrollbar = true;
                dragStartMouseY = (int) mouseY;
                dragStartScrollOffset = scrollOffset;
                return true;
            }
        }

        if (mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_HEIGHT - 10) {
            int clickedIndex = (int) ((mouseY - LIST_START_Y + scrollOffset) / ITEM_HEIGHT);

            if (clickedIndex >= 0 && clickedIndex < fileEntries.size()) {
                selectedIndex = clickedIndex;
                updateLoadButtonState();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && maxScrollOffset > 0) {
            int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                    (int) (listHeight * listHeight / (float) (fileEntries.size() * ITEM_HEIGHT)));
            float trackAvailableHeight = listHeight - scrollbarThumbHeight;

            if (trackAvailableHeight > 0) {
                int deltaY = (int) mouseY - dragStartMouseY;
                float scrollRatio = deltaY / trackAvailableHeight;
                int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScrollOffset));

                if (newScrollOffset != scrollOffset) {
                    scrollOffset = newScrollOffset;
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        int scrollDelta = (int) (scrollAmount * ITEM_HEIGHT);
        int newScrollOffset = scrollOffset - scrollDelta;

        if (newScrollOffset < 0) newScrollOffset = 0;
        if (newScrollOffset > maxScrollOffset) newScrollOffset = maxScrollOffset;

        if (newScrollOffset != scrollOffset) {
            scrollOffset = newScrollOffset;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private record FileEntry(String name, JsonObject content, Path path, long lastModified, boolean valid) {}

    @OnlyIn(Dist.CLIENT)
    private static class ExportNameScreen extends Screen {
        private final Screen parent;
        private final JsonObject config;
        private final Consumer<String> onConfirm;
        private EditBox editBox;
        private Button confirmButton;
        private Button cancelButton;

        protected ExportNameScreen(Screen parent, JsonObject config, Consumer<String> onConfirm) {
            super(Component.translatable("gui.zombiegamereborn.gameproperty.export_dialog_title"));
            this.parent = parent;
            this.config = config;
            this.onConfirm = onConfirm;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.gameproperty.export_dialog_title"),
                    this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        }

        @Override
        protected void init() {
            super.init();
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.editBox = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20,
                    Component.translatable("gui.zombiegamereborn.gameproperty.export_file_name"));
            this.editBox.setValue("New Config");
            this.addWidget(this.editBox);
            this.setFocused(this.editBox);

            this.confirmButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.confirm"),
                    btn -> {
                        String name = editBox.getValue().trim();
                        if (!name.isEmpty()) {
                            onConfirm.accept(name);
                        }
                    }
            ).bounds(centerX - 110, centerY + 10, 100, 20).build();

            this.cancelButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.cancel"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).bounds(centerX + 10, centerY + 10, 100, 20).build();

            this.addRenderableWidget(confirmButton);
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class RenameScreen extends Screen {
        private final Screen parent;
        private final String oldName;
        private final Consumer<String> onConfirm;
        private EditBox editBox;
        private Button confirmButton;
        private Button cancelButton;

        protected RenameScreen(Screen parent, String oldName, Consumer<String> onConfirm) {
            super(Component.translatable("gui.zombiegamereborn.gameproperty.rename_title"));
            this.parent = parent;
            this.oldName = oldName;
            this.onConfirm = onConfirm;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.gameproperty.rename_title"),
                    this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        }

        @Override
        protected void init() {
            super.init();
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.editBox = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20,
                    Component.translatable("gui.zombiegamereborn.gameproperty.rename_file_name"));
            this.editBox.setValue(oldName);
            this.addWidget(this.editBox);
            this.setFocused(this.editBox);

            this.confirmButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.confirm"),
                    btn -> {
                        String name = editBox.getValue().trim();
                        if (!name.isEmpty() && !name.equals(oldName)) {
                            onConfirm.accept(name);
                        }
                    }
            ).bounds(centerX - 110, centerY + 10, 100, 20).build();

            this.cancelButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.cancel"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).bounds(centerX + 10, centerY + 10, 100, 20).build();

            this.addRenderableWidget(confirmButton);
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }
}
