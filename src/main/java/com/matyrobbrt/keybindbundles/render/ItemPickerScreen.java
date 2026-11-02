package com.matyrobbrt.keybindbundles.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<ItemStack> onSelect;
    private EditBox searchBox;
    private List<ItemStack> allItems = List.of();
    private List<ItemStack> filteredItems = List.of();

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 20;

    private int currentPage = 0;
    private Button prevButton;
    private Button nextButton;

    public ItemPickerScreen(Screen parent, Consumer<ItemStack> onSelect) {
        super(Component.literal("Select Item Icon"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
        if (allItems.isEmpty()) {
            List<ItemStack> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    list.add(item.getDefaultInstance());
                }
            }
            this.allItems = list;
            this.filteredItems = new ArrayList<>(list);
        }

        int startX = width / 2 - (COLUMNS * SLOT_SIZE) / 2;
        int startY = 32;

        searchBox = new EditBox(font, startX, startY, COLUMNS * SLOT_SIZE, 18, Component.literal("Search Items..."));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        int gridBottom = startY + 24 + ROWS * SLOT_SIZE;

        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) {
                currentPage--;
                updatePageButtons();
            }
        }).pos(startX, gridBottom + 4).size(40, 20).build());

        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if ((currentPage + 1) * PAGE_SIZE < filteredItems.size()) {
                currentPage++;
                updatePageButtons();
            }
        }).pos(startX + COLUMNS * SLOT_SIZE - 40, gridBottom + 4).size(40, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .pos(width / 2 - 50, gridBottom + 4)
                .size(100, 20)
                .build());

        updatePageButtons();
    }

    private void onSearchChanged(String query) {
        if (query == null || query.isBlank()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            String lower = query.toLowerCase(Locale.ROOT).trim();
            filteredItems = allItems.stream().filter(stack -> {
                String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String id = rl != null ? rl.toString().toLowerCase(Locale.ROOT) : "";
                return name.contains(lower) || id.contains(lower);
            }).toList();
        }
        currentPage = 0;
        updatePageButtons();
    }

    private int maxPages() {
        return Math.max(1, (int) Math.ceil((double) filteredItems.size() / PAGE_SIZE));
    }

    private void updatePageButtons() {
        if (prevButton != null) {
            prevButton.active = currentPage > 0;
        }
        if (nextButton != null) {
            nextButton.active = (currentPage + 1) * PAGE_SIZE < filteredItems.size();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, getTitle(), width / 2, 14, 0xFFFFFFFF);

        String pageInfo = (currentPage + 1) + " / " + maxPages() + " (" + filteredItems.size() + ")";
        int startX = width / 2 - (COLUMNS * SLOT_SIZE) / 2;
        int gridTop = 58;
        int gridBottom = gridTop + ROWS * SLOT_SIZE;
        guiGraphics.drawCenteredString(font, pageInfo, width / 2, gridBottom - 10, 0xFFAAAAAA);

        int startIndex = currentPage * PAGE_SIZE;
        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = startIndex + row * COLUMNS + col;
                int slotX = startX + col * SLOT_SIZE;
                int slotY = gridTop + row * SLOT_SIZE;

                boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, hovered ? 0x80555555 : 0x40000000);

                if (index < filteredItems.size()) {
                    ItemStack stack = filteredItems.get(index);
                    guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                    if (hovered) {
                        hoveredStack = stack;
                    }
                }
            }
        }

        if (!hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int startX = width / 2 - (COLUMNS * SLOT_SIZE) / 2;
            int gridTop = 58;

            int startIndex = currentPage * PAGE_SIZE;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    int slotX = startX + col * SLOT_SIZE;
                    int slotY = gridTop + row * SLOT_SIZE;
                    if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                        int index = startIndex + row * COLUMNS + col;
                        if (index < filteredItems.size()) {
                            ItemStack chosen = filteredItems.get(index);
                            // Call callback BEFORE setting screen to parent
                            onSelect.accept(chosen);
                            Minecraft.getInstance().setScreen(parent);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && currentPage > 0) {
            currentPage--;
            updatePageButtons();
            return true;
        } else if (scrollY < 0 && (currentPage + 1) * PAGE_SIZE < filteredItems.size()) {
            currentPage++;
            updatePageButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
