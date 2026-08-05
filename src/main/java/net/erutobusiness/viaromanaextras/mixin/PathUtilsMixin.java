package net.erutobusiness.viaromanaextras.mixin;

import net.erutobusiness.viaromanaextras.ViaRomanaExtras;
import net.erutobusiness.viaromanaextras.client.RoadMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.util.PathUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 道判定を「宣言したブロックだけ」に置き換える。
 *
 * <p>Via Romana 側の {@code isBlockValidPath} は
 * 「空気でなく、かつ {@code via_romana:path_block} タグに入っているか」の1行。
 * このタグは config の部分一致24語などから作られ、当部の構成では
 * <b>7,783 ブロック中 1,979 件</b>が該当する。砂漠の砂岩・深層岩帯・玄武岩デルタといった
 * <b>自然地形まで道扱いになる</b>のが問題だった。
 *
 * <p>ここを宣言（最大9種類）に差し替えると、config を1文字も絞らずにその問題が消える。
 *
 * <p><b>クライアント側だけに当てる</b>（mixins.json の {@code client} 配列）。
 * 品質の計算は {@code ChartingHandler}（client パッケージ）が行い、
 * サーバーは {@code ChartedPathC2S} で受け取った値を検算しないので、これで足りる。
 * 測量中の色付き表示（{@code InvalidBlockRenderer}）も同じメソッドを呼ぶので、
 * <b>宣言外のブロックがその場で色付きで見える</b>。
 */
@Mixin(value = PathUtils.class, remap = false)
public abstract class PathUtilsMixin {

    @Inject(method = "isBlockValidPath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void via_romana_extras$onlyDeclaredBlocks(
            LevelAccessor level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {

        if (RoadMaterials.isEmpty()) {
            // 何も宣言していない。必須なら「どれも道ではない」、そうでなければ本体の判定に任せる
            if (ViaRomanaExtras.REQUIRE_DECLARATION.get()) {
                cir.setReturnValue(false);
            }
            return;
        }
        cir.setReturnValue(RoadMaterials.isDeclared(level.getBlockState(pos)));
    }
}
