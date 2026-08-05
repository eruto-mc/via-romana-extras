package net.erutobusiness.viaromanaextras;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Via Romana Extras — Via Romana に2つ足す。
 *
 * <p><b>1. 騎乗中も道の近くで速くなる。</b> 本体の {@code SpeedHandler.onPlayerTick} は
 * {@code ServerPlayer} 自身の {@code MOVEMENT_SPEED} にしか修飾子を付けない。
 * 騎乗中の移動速度は馬側の属性が決めるので、本体の +40% は乗馬中まったく効かない
 * （全クラスを走査して確認済み。{@code AbstractHorse} を参照するクラスは0件）。
 * ここでは乗っているエンティティ側に同じ修飾子を付ける。
 *
 * <p><b>2. 道の材料を宣言させる。</b> 測量画面から最大9種類のブロックを登録し、
 * 登録したものだけを道判定に使う（既定の部分一致24語＝1,979ブロックを置き換える）。
 * これで砂漠の砂岩・深層岩帯・玄武岩デルタといった自然地形で品質を稼げなくなる。
 *
 * <p><b>速度の設計。</b> 当部の馬は 0.35（15.1 b/s）／スケ・ゾンビ馬は 0.38（16.4 b/s）で、
 * ここで +40% すると 21.2 / 23.0 b/s。銅レール幹線の 24 b/s の下に収まる。
 * <b>馬の基礎値を上げるときは必ず {@code 基礎 × 1.4 × 43.17 < 24} を確かめること。</b>
 * 数字の一覧は {@code selection/travel-speed-table.md}。
 *
 * <p><b>fork ではない。</b> Via Romana の公開クラスを呼ぶだけで、jar の同梱も再配布もしない。
 * どちらの機能も上流本来の役割なので issue を出す。取り込まれたら本MODは役目を終える。
 */
@Mod(ViaRomanaExtras.MOD_ID)
public class ViaRomanaExtras {

    public static final String MOD_ID = "via_romana_extras";

    public static final ForgeConfigSpec SPEC;

    /** 騎乗中の加速を有効にするか。 */
    public static final ForgeConfigSpec.BooleanValue MOUNTED_SPEED_ENABLED;
    /**
     * 騎乗中の速度倍率（{@code MULTIPLY_TOTAL} に渡す値）。
     * 0.4 ＝ +40% で、Via Romana 本体の {@code fast_movement_speed} と同じ。
     */
    public static final ForgeConfigSpec.DoubleValue MOUNTED_SPEED_BONUS;

    /**
     * 道の材料を宣言しないと測量できなくするか。
     * false にすると、宣言が空のあいだは Via Romana 既定の判定に戻る。
     */
    public static final ForgeConfigSpec.BooleanValue REQUIRE_DECLARATION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("騎乗中も、記録した道の近くで速くなる").push("mounted_speed");
        MOUNTED_SPEED_ENABLED = b
                .comment("有効にすると、道のノードから node_distance_minimum 以内にいる騎乗物が速くなる")
                .define("enabled", true);
        MOUNTED_SPEED_BONUS = b
                .comment("倍率（MULTIPLY_TOTAL）。0.4 = +40%。",
                         "⚠ 上げるときは 馬の基礎値 x (1+この値) x 43.17 が銅レールの速さを超えないか確かめること")
                .defineInRange("bonus", 0.4D, 0.0D, 4.0D);
        b.pop();

        b.comment("道の材料の宣言").push("road_materials");
        REQUIRE_DECLARATION = b
                .comment("true なら、材料を1つも登録していない間はどのブロックも道と認めない（登録を必須にする）",
                         "false なら、登録が空のときだけ Via Romana 既定の判定に戻る")
                .define("required", true);
        b.pop();

        SPEC = b.build();
    }

    public ViaRomanaExtras() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
