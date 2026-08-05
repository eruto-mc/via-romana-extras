package net.erutobusiness.viaromanaextras;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.PathGraph;

/**
 * 検証用のコマンド（OP2 以上）。<b>遊びの機能ではない。</b>
 *
 * <p>なぜ要るか: 「道の近くだと騎乗が速くなる」を実測するには
 * <b>登録済みの経路ノードが要る</b>が、ノードは測量の一連の操作
 * （地図を開く → 記録開始 → 歩く → 記録終了）でしか作れず、
 * Via Romana 側にノードを作るコマンドが無い（`/viaromana nodes clear` は消すだけ）。
 * そのため自動確認の台本から道を用意できず、<b>2026-08-05 の初回の実測は
 * 「道の無い場所の馬」しか測れていなかった</b>（＝この機能の肝を測り損ねていた）。
 *
 * <p>{@code PathGraph.getOrCreateNode} は public なので、そこだけ叩く小さな窓を開ける。
 *
 * <ul>
 *   <li>{@code /viaromanaextras node} … 足元に経路ノードを1つ作る</li>
 *   <li>{@code /viaromanaextras speed} … いま乗っている相手の移動速度と、
 *       当部の修飾子が付いているかを読む</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ViaRomanaExtras.MOD_ID)
public final class DebugCommand {

    private DebugCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("viaromanaextras")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("node").executes(c -> makeNode(c.getSource())))
                .then(Commands.literal("speed").executes(c -> readSpeed(c.getSource()))));
    }

    /** 足元に経路ノードを作る。品質と余裕は測量が通る値を入れておく。 */
    private static int makeNode(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null || !(p.level() instanceof ServerLevel level)) {
            src.sendFailure(Component.literal("[vrextras] プレイヤーが居ない"));
            return 0;
        }
        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) {
            src.sendFailure(Component.literal("[vrextras] PathGraph が無い次元"));
            return 0;
        }
        BlockPos pos = p.blockPosition();
        int id = graph.getOrCreateNode(pos, 1.0F, 4.0F);
        src.sendSuccess(() -> Component.literal(
                "[vrextras] node id=" + id + " at " + pos.toShortString()), false);
        return 1;
    }

    /** 乗っている相手の速度属性と、当部の修飾子の有無を読む。 */
    private static int readSpeed(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null || !(p.level() instanceof ServerLevel level)) {
            src.sendFailure(Component.literal("[vrextras] プレイヤーが居ない"));
            return 0;
        }
        Entity v = p.getVehicle();
        boolean near = false;
        PathGraph graph = PathGraph.getInstance(level);
        if (graph != null) {
            near = graph.getNearestNode(p.blockPosition(), CommonConfig.node_distance_minimum).isPresent();
        }
        String body;
        if (v instanceof LivingEntity le) {
            AttributeInstance attr = le.getAttribute(Attributes.MOVEMENT_SPEED);
            boolean boosted = attr != null && attr.getModifier(MountedSpeedHandler.MOUNTED_SPEED_ID) != null;
            body = String.format("vehicle=%s base=%.4f value=%.4f boosted=%s near=%s",
                    v.getType().toString(),
                    attr == null ? -1 : attr.getBaseValue(),
                    attr == null ? -1 : attr.getValue(),
                    boosted, near);
        } else {
            body = "vehicle=none near=" + near;
        }
        final String out = "[vrextras] " + body;
        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }
}
