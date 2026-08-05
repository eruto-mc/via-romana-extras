package net.erutobusiness.viaromanaextras.mixin;

import net.erutobusiness.viaromanaextras.client.RoadMaterials;
import net.erutobusiness.viaromanaextras.client.RoadMaterialsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.rasanovum.viaromana.client.gui.ChartingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 測量画面に「道の材料」ボタンを足す。
 *
 * <p>{@code ChartingScreen} は {@code Screen} を継承していて {@code init()} を上書きしている。
 * その末尾に自前のボタンを1つ足すだけ。Via Romana 側の widget（{@code MapSquareButton} 等）は
 * <b>使わない</b>——構造を知るほど上流の更新で壊れやすくなるため、素の {@code Button} を使う。
 * 見た目が地図の意匠と揃わないのは承知のうえ（必要なら後で寄せる）。
 *
 * <p>⚠ {@code @Inject} は複数MODが同じ場所に入っても共存できる。
 * {@code @Redirect} / {@code @Overwrite} は使わない。
 */
@Mixin(value = ChartingScreen.class, remap = false)
public abstract class ChartingScreenMixin extends Screen {

    protected ChartingScreenMixin(Component title) {
        super(title);
    }

    // ⚠ 名前は SRG のまま書く。ChartingScreen は Via Romana の jar の中で既に
    //    protected void m_7856_() （= Screen.init の SRG 名）になっており、
    //    本番の Forge も Minecraft のメンバを SRG 名で動かすため、ここは remap してはいけない。
    //    "init" と書くと注釈処理器が記述子を解決できず（ビルド時に警告）、本番で
    //    ターゲットを見失って起動時に落ちる。
    @Inject(method = "m_7856_", at = @At("TAIL"), remap = false)
    private void via_romana_extras$addMaterialsButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.via_romana_extras.materials.button")
                                .append(" (" + RoadMaterials.count() + "/" + RoadMaterials.SLOTS + ")"),
                        b -> Minecraft.getInstance().setScreen(new RoadMaterialsScreen(self)))
                .bounds(this.width / 2 - 60, this.height - 26, 120, 20)
                .build());
    }
}
