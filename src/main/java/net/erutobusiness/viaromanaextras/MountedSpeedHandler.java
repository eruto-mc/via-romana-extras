package net.erutobusiness.viaromanaextras;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.PathGraph;

/**
 * 騎乗中の加速。Via Romana の {@code SpeedHandler.onPlayerTick} と同じ形で、
 * 付ける先だけを「プレイヤー」から「乗っているエンティティ」に変えたもの。
 *
 * <p>発動条件も本体と揃える: {@code PathGraph.getNearestNode(pos, node_distance_minimum)}
 * が存在すること。＝<b>記録した経路のノードから4ブロック以内</b>。
 *
 * <p>⚠ <b>必ず transient な修飾子を使う。</b> 永続の修飾子を付けると馬の NBT に焼き付き、
 * 本MODを外しても速いままの馬が残る。{@code addTransientModifier} なら保存されない。
 *
 * <p>⚠ プレイヤー自身に付く本体の修飾子は<b>外さない</b>。騎乗中は移動に効かないが、
 * 画角（{@code FieldOfViewHelper}）には効いており、騎乗でも速くなる以上その見え方は正しい。
 */
@Mod.EventBusSubscriber(modid = ViaRomanaExtras.MOD_ID)
public final class MountedSpeedHandler {

    /** 本体の {@code viaromana:node_proximity_speed} と衝突しない別のUUIDを使う。 */
    public static final UUID MOUNTED_SPEED_ID =
            UUID.nameUUIDFromBytes("via_romana_extras:mounted_proximity_speed"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private static final String MOUNTED_SPEED_NAME = "mounted_proximity_speed";

    /**
     * 「このプレイヤーのために、どのエンティティへ修飾子を付けたか」。
     * 降りた瞬間に外すために要る（降りたあとは相手をたどれなくなるため）。
     */
    private static final Map<UUID, Entity> boosted = new HashMap<>();

    private MountedSpeedHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity current = player.getVehicle();
        Entity previous = boosted.get(player.getUUID());

        // 乗り換えた・降りたなら、前の相手から必ず外す
        if (previous != null && previous != current) {
            removeFrom(previous);
            boosted.remove(player.getUUID());
        }

        if (current == null || !ViaRomanaExtras.MOUNTED_SPEED_ENABLED.get()) {
            if (current != null) {
                removeFrom(current);
            }
            return;
        }

        boolean near = isNearChartedPath(level, player.blockPosition());
        if (near) {
            applyTo(current);
            boosted.put(player.getUUID(), current);
        } else {
            removeFrom(current);
            boosted.put(player.getUUID(), current);
        }
    }

    /** ログアウト時に取り残さない。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Entity prev = boosted.remove(event.getEntity().getUUID());
        if (prev != null) {
            removeFrom(prev);
        }
    }

    /**
     * 本体 {@code SpeedHandler} と同じ判定。
     * {@code PathGraph} がまだ無い次元では {@code null} が返るので、そのときは効かせない。
     */
    private static boolean isNearChartedPath(ServerLevel level, BlockPos pos) {
        PathGraph graph = PathGraph.getInstance(level);
        if (graph == null) {
            return false;
        }
        return graph.getNearestNode(pos, CommonConfig.node_distance_minimum).isPresent();
    }

    private static void applyTo(Entity vehicle) {
        AttributeInstance attr = movementSpeedOf(vehicle);
        if (attr == null || attr.getModifier(MOUNTED_SPEED_ID) != null) {
            return;
        }
        attr.addTransientModifier(new AttributeModifier(
                MOUNTED_SPEED_ID,
                MOUNTED_SPEED_NAME,
                ViaRomanaExtras.MOUNTED_SPEED_BONUS.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeFrom(Entity vehicle) {
        AttributeInstance attr = movementSpeedOf(vehicle);
        if (attr != null && attr.getModifier(MOUNTED_SPEED_ID) != null) {
            attr.removeModifier(MOUNTED_SPEED_ID);
        }
    }

    /** 移動速度の属性を持たない乗り物（ボート・トロッコ）は対象外になる。 */
    private static AttributeInstance movementSpeedOf(Entity vehicle) {
        if (!(vehicle instanceof LivingEntity living)) {
            return null;
        }
        return living.getAttribute(Attributes.MOVEMENT_SPEED);
    }
}
