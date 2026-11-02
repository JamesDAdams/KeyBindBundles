package com.matyrobbrt.keybindbundles.render;

import com.matyrobbrt.keybindbundles.KeyBindBundle;
import com.matyrobbrt.keybindbundles.KeyBindBundleManager;
import com.matyrobbrt.keybindbundles.KeyMappingUtil;
import com.matyrobbrt.keybindbundles.util.SearchTreeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KeyBundleModificationScreen extends Screen {
    public static KeyBindBundle currentlySelecting;

    private static final KeyBindBundle.KeyEntry ADD_ENTRY = new KeyBindBundle.KeyEntry("add", "§6Add Entry§r", ItemStack.EMPTY);
    private static final List<Component> TOOLTIPS = List.of(
            Component.translatable("tooltip.keybindbundles.bundle.edit_entry"),
            Component.translatable("tooltip.keybindbundles.bundle.delete_entry"),
            Component.translatable("tooltip.keybindbundles.bundle.move_clockwise"),
            Component.translatable("tooltip.keybindbundles.bundle.move_counterclockwise")
    );
    private static final WidgetSprites CROSS_BUTTON_SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted")
    );

    private final RadialMenuRenderer<KeyBindBundle.KeyEntry> renderer = new RadialMenuRenderer<>() {
        @Override
        public List<KeyBindBundle.KeyEntry> getEntries() {
            return entries;
        }

        @Override
        public int getCurrentlySelected() {
            return bundle.getBookmark();
        }

        @Override
        public Component getTitle(KeyBindBundle.KeyEntry entry) {
            return Component.literal(entry.title());
        }

        @Override
        public ItemStack getIcon(KeyBindBundle.KeyEntry entry) {
            return entry.icon();
        }

        @Override
        public String getCustomIcon(KeyBindBundle.KeyEntry entry) {
            return entry.customIcon();
        }

        @Override
        public boolean trackOffCircleMouse() {
            return false;
        }
    };

    private final KeyBindBundle bundle;
    private List<KeyBindBundle.KeyEntry> entries = List.of();

    public KeyBundleModificationScreen(KeyBindBundle bundle) {
        super(Component.translatable("title.keybindbundles.editing_keybundle", Component.literal(bundle.name).withStyle(ChatFormatting.GOLD)));
        this.bundle = bundle;
    }

    @Override
    protected void init() {
        super.init();
        entries = new ArrayList<>(bundle.getEntries());
        entries.add(ADD_ENTRY);

        var deleteButton = this.addRenderableWidget(
                new ImageButton(width - 18, height - 18, 14, 14, CROSS_BUTTON_SPRITES, but -> {
                    Minecraft.getInstance().setScreen(new ConfirmScreen(click -> {
                        if (click) {
                            currentlySelecting = null;
                            Minecraft.getInstance().setScreen(null);
                            KeyBindBundleManager.delete(bundle);
                        } else {
                            Minecraft.getInstance().setScreen(this);
                        }
                    }, Component.translatable("title.keybindbundles.confirm_deletion"), Component.translatable("message.keybindbundles.confirm_deletion")));
                }, Component.translatable("button.keybindbundles.delete"))
        );
        deleteButton.setTooltip(Tooltip.create(Component.translatable("tooltip.keybindbundles.delete_bundle")));
    }

    @Override
    public void onClose() {
        KeyBindBundleManager.write();
        currentlySelecting = null;
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(font, getTitle(), width / 2 - font.width(getTitle()) / 2, 10, 0xffffffff);

        var render = Minecraft.getInstance().screen == this;
        renderer.render(guiGraphics, render);
        if (render) {
            var element = renderer.getElementUnderMouse(true);
            if (element >= 0 && element < entries.size() - 1) {
                guiGraphics.renderTooltip(font, TOOLTIPS, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var element = renderer.getElementUnderMouse(true);
        if (element < 0) return super.mouseClicked(mouseX, mouseY, button);

        if (element == entries.size() - 1) {
            currentlySelecting = bundle;
            Minecraft.getInstance().setScreen(new KeyBindsScreen(
                    this, Minecraft.getInstance().options
            ));
        } else {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (Screen.hasShiftDown()) {
                    if (bundle.getBookmark() == element) {
                        bundle.setBookmark(-1);
                    }

                    entries.remove(element);
                    bundle.getEntries().remove(element);
                } else {
                    var key = KeyMappingUtil.getByName(bundle.getEntries().get(element).key());
                    Minecraft.getInstance().pushGuiLayer(new EditKeyScreen(element, key == null ? Component.empty() : Component.translatable("title.keybindbundles.editing_key", key.getDisplayName().copy().withStyle(ChatFormatting.GOLD))));
                }
            } else {
                int newPos;
                if (Screen.hasShiftDown()) {
                    newPos = element - 1;
                    if (newPos < 0) {
                        newPos = bundle.getEntries().size() - 1;
                    }
                } else {
                    newPos = element + 1;
                    if (newPos >= bundle.getEntries().size()) {
                        newPos = 0;
                    }
                }

                Collections.swap(entries, element, newPos);
                Collections.swap(bundle.getEntries(), element, newPos);
            }
        }

        return true;
    }

    public class EditKeyScreen extends Screen {
        private final int key;
        private int middlePos;
        protected EditKeyScreen(int key, Component title) {
            super(title);
            this.key = key;
        }

        private EditBox title;
        private AutoCompleteEditBox<ItemStack> icon;
        private String customIconFilename = "";
        private ItemStack selectedItemFromPicker = null;
        private String cachedTitle = null;

        @Override
        protected void init() {
            var font = Minecraft.getInstance().font;
            middlePos = height / 2 - 30;
            if (selectedItemFromPicker != null) {
                this.customIconFilename = "";
            } else if (customIconFilename == null || customIconFilename.isBlank()) {
                customIconFilename = getEntry().customIcon();
                if (customIconFilename == null) customIconFilename = "";
            }

            title = new EnterEditBox(font, width / 2 - 120, middlePos - 2 - 20, 240, 20, Component.translatable("box.keybindbundles.key_title")) {
                @Override
                protected void onEnter() {
                    EditKeyScreen.this.setFocused(icon);
                }
            };
            title.setValue(cachedTitle != null ? cachedTitle : getEntry().title());

            SearchTree<ItemStack> tree = SearchTreeManager.getSearchTree();
            icon = new AutoCompleteEditBox<>(font, width / 2 - 120, middlePos + 2 + 9 + 2, 240, 20, 16, 18, 5, Component.translatable("box.keybindbundles.key_icon_id"), tree, i -> i.getItemHolder().getKey().location()) {
                @Override
                public void renderItem(GuiGraphics graphics, int x, int y, ItemStack item) {
                    graphics.renderItem(item, x, y);
                }
            };
            icon.setMaxLength(512);

            if (selectedItemFromPicker != null && !selectedItemFromPicker.isEmpty()) {
                icon.setValue(BuiltInRegistries.ITEM.getKey(selectedItemFromPicker.getItem()).toString());
            } else if (!getEntry().icon().isEmpty()) {
                icon.setValue(BuiltInRegistries.ITEM.getKey(getEntry().icon().getItem()).toString());
            }

            addRenderableWidget(title);
            addRenderableWidget(icon);

            // Action Buttons under icon box: [Browse Items] [Paste Image] [Clear]
            int btnY = middlePos + 2 + 9 + 2 + 24;
            addRenderableWidget(new Button.Builder(Component.translatable("button.keybindbundles.browse_items"), b -> {
                this.cachedTitle = this.title.getValue();
                Minecraft.getInstance().setScreen(new ItemPickerScreen(this, itemStack -> {
                    this.customIconFilename = "";
                    this.selectedItemFromPicker = itemStack;
                    ResourceLocation rl = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                    if (rl != null) {
                        this.icon.setValue(rl.toString());
                    }
                }));
            }).pos(width / 2 - 120, btnY).size(78, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("tooltip.keybindbundles.browse_items"))).build());

            addRenderableWidget(new Button.Builder(Component.translatable("button.keybindbundles.paste_image"), b -> {
                pasteCustomImage();
            }).pos(width / 2 - 38, btnY).size(76, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("tooltip.keybindbundles.paste_image"))).build());

            addRenderableWidget(new Button.Builder(Component.translatable("button.keybindbundles.clear_icon"), b -> {
                this.customIconFilename = "";
                this.selectedItemFromPicker = null;
                this.icon.setValue("");
            }).pos(width / 2 + 42, btnY).size(78, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("tooltip.keybindbundles.clear_icon"))).build());

            addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, p -> onClose())
                    .pos(width / 2 - 240 / 2, btnY + 26)
                    .size(240, 20)
                    .build());

            addRenderableWidget(icon.autoComplete());
        }

        private void pasteCustomImage() {
            String registeredFilename = ImageClipboardHelper.tryPasteImage();
            if (registeredFilename != null) {
                this.customIconFilename = registeredFilename;
                this.selectedItemFromPicker = null;
                this.icon.setValue("");
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (Screen.isPaste(keyCode) && !this.title.isFocused() && !this.icon.isFocused()) {
                pasteCustomImage();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            var font = Minecraft.getInstance().font;

            guiGraphics.drawString(font, getTitle(), width / 2 - font.width(getTitle()) / 2, 30, 0xffffffff);

            var title = Component.translatable("box.keybindbundles.key_title");
            guiGraphics.drawString(Minecraft.getInstance().font, title, width / 2 - font.width(title) / 2, middlePos - 2 - 20 - 2 - 9, 0xffffffff);

            var iconLabel = Component.translatable("box.keybindbundles.key_icon");
            guiGraphics.drawString(Minecraft.getInstance().font, iconLabel, width / 2 - font.width(iconLabel) / 2, middlePos + 2, 0xffffffff);

            int previewX = this.icon.getX() + this.icon.getWidth() + 4;
            int previewY = middlePos + 1 + 9 + 2 + (20 - 16) / 2;

            if (customIconFilename != null && !customIconFilename.isBlank()) {
                var rl = CustomImageManager.getOrLoadTexture(customIconFilename);
                if (rl != null) {
                    guiGraphics.blit(rl, previewX, previewY, 0, 0, 16, 16, 16, 16);
                }
            } else if (selectedItemFromPicker != null && !selectedItemFromPicker.isEmpty()) {
                guiGraphics.renderItem(selectedItemFromPicker, previewX, previewY);
            } else if (!this.icon.getValue().isEmpty()) {
                var parsed = ResourceLocation.tryParse(this.icon.getValue());
                if (parsed != null) {
                    var item = BuiltInRegistries.ITEM.get(parsed);
                    if (item != Items.AIR) {
                        guiGraphics.renderItem(item.getDefaultInstance(), previewX, previewY);
                    }
                }
            }
        }

        @Override
        public void onClose() {
            var old = getEntry();
            ItemStack iconStack = ItemStack.EMPTY;
            if (customIconFilename.isBlank()) {
                if (selectedItemFromPicker != null && !selectedItemFromPicker.isEmpty()) {
                    iconStack = selectedItemFromPicker;
                } else if (!this.icon.getValue().isEmpty()) {
                    var parsed = ResourceLocation.tryParse(this.icon.getValue());
                    if (parsed != null) {
                        var item = BuiltInRegistries.ITEM.get(parsed);
                        if (item != Items.AIR) {
                            iconStack = item.getDefaultInstance();
                        }
                    }
                }
            }

            var newEntry = new KeyBindBundle.KeyEntry(old.key(), title.getValue(), iconStack, customIconFilename);
            entries.set(key, newEntry);
            bundle.getEntries().set(key, newEntry);

            Minecraft.getInstance().setScreen(KeyBundleModificationScreen.this);
        }

        public KeyBindBundle.KeyEntry getEntry() {
            return entries.get(key);
        }

        protected class EnterEditBox extends EditBox {
            public EnterEditBox(Font font, int x, int y, int width, int height, Component message) {
                super(font, x, y, width, height, message);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    onEnter();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            protected void onEnter() {
                onClose();
            }
        }
    }

}
