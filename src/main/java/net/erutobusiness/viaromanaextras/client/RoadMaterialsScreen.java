package net.erutobusiness.viaromanaextras.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 道の材料を決める画面。3×3 の枠に最大9種類まで入れる。
 *
 * <p><b>枠は「見本を写す」ものであって、アイテムを預けるスロットではない。</b>
 * 理由: {@code minecraft:dirt_path} は<b>サバイバルでアイテムとして手に入らない</b>
 * （ルートテーブルが {@code minecraft:dirt} しか落とさず、レシピも1件も無い。vanilla jar で確認）。
 * これは Via Romana の既定リストの筆頭で、シャベル整地の到達点でもある。
 * 実アイテムを入れる作りにすると<b>この世界で一番中心にある道ブロックを登録できない</b>。
 * だから「足元のブロックを入れる」ボタンを併設し、アイテムの出し入れはしない
 * （画面を閉じてアイテムが消える事故も起きない）。
 */
public class RoadMaterialsScreen extends Screen {

    private static final int SLOT = 20;
    private static final int GAP = 2;
    private static final int GRID = 3;

    private final Screen parent;

    private int gridX;
    private int gridY;
    private int invX;
    private int invY;

    public RoadMaterialsScreen(Screen parent) {
        super(Component.translatable("gui.via_romana_extras.materials.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int gridW = GRID * SLOT + (GRID - 1) * GAP;
        this.gridX = (this.width - gridW) / 2;
        this.gridY = 46;

        int invW = 9 * SLOT + 8 * GAP;
        this.invX = (this.width - invW) / 2;
        this.invY = this.gridY + GRID * (SLOT + GAP) + 22;

        int btnY = this.invY + 4 * (SLOT + GAP) + 10;
        int btnW = 150;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.via_romana_extras.materials.from_ground"),
                b -> addBlockUnderFeet())
                .bounds(this.width / 2 - btnW - 4, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.via_romana_extras.materials.clear"),
                b -> RoadMaterials.clear())
                .bounds(this.width / 2 + 4, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.via_romana_extras.materials.done"),
                b -> this.onClose())
                .bounds(this.width / 2 - 50, btnY + 24, 100, 20).build());
    }

    /** 持ち物に入らないブロック（dirt_path など）のための入口。 */
    private void addBlockUnderFeet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos below = mc.player.blockPosition().below();
        BlockState state = mc.level.getBlockState(below);
        if (!state.isAir()) {
            RoadMaterials.add(state.getBlock());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);

        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("gui.via_romana_extras.materials.hint"),
                this.width / 2, 26, 0xA0A0A0);

        // 3×3 の枠
        for (int i = 0; i < RoadMaterials.SLOTS; i++) {
            int x = gridX + (i % GRID) * (SLOT + GAP);
            int y = gridY + (i / GRID) * (SLOT + GAP);
            g.fill(x, y, x + SLOT, y + SLOT, 0xFF3A3A3A);
            g.renderOutline(x, y, SLOT, SLOT, 0xFF8B8B8B);
            Block b = RoadMaterials.get(i);
            if (b != null) {
                g.renderItem(new ItemStack(b), x + 2, y + 2);
            }
        }

        String status = RoadMaterials.isEmpty()
                ? Component.translatable("gui.via_romana_extras.materials.empty").getString()
                : Component.translatable("gui.via_romana_extras.materials.slot_hint").getString();
        int statusColor = RoadMaterials.isEmpty() ? 0xFFFF6060 : 0xFFA0A0A0;
        g.drawCenteredString(this.font, status, this.width / 2, invY - 14, statusColor);

        // 持ち物（36枠）。クリックで材料に写す。アイテムは動かない
        Inventory inv = Minecraft.getInstance().player == null
                ? null : Minecraft.getInstance().player.getInventory();
        if (inv != null) {
            for (int i = 0; i < 36; i++) {
                int x = invX + (i % 9) * (SLOT + GAP);
                int y = invY + (i / 9) * (SLOT + GAP);
                g.fill(x, y, x + SLOT, y + SLOT, 0xFF2A2A2A);
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                    g.renderItem(stack, x + 2, y + 2);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 枠を右クリック → 取り消し
        for (int i = 0; i < RoadMaterials.SLOTS; i++) {
            int x = gridX + (i % GRID) * (SLOT + GAP);
            int y = gridY + (i / GRID) * (SLOT + GAP);
            if (inside(mouseX, mouseY, x, y) && button == 1) {
                RoadMaterials.remove(i);
                return true;
            }
        }
        // 持ち物を左クリック → 追加（アイテムは減らない）
        if (button == 0 && Minecraft.getInstance().player != null) {
            Inventory inv = Minecraft.getInstance().player.getInventory();
            for (int i = 0; i < 36; i++) {
                int x = invX + (i % 9) * (SLOT + GAP);
                int y = invY + (i / 9) * (SLOT + GAP);
                if (inside(mouseX, mouseY, x, y)) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                        RoadMaterials.add(blockItem.getBlock());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT && my >= y && my < y + SLOT;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
