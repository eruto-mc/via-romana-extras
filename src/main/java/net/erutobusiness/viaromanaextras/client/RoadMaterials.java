package net.erutobusiness.viaromanaextras.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 「この道は何でできているか」の宣言。<b>クライアント側だけに持つ。</b>
 *
 * <p>Via Romana の品質判定はクライアントで完結しており、サーバーは
 * {@code ChartedPathC2S} が送ってきた quality を検算しない（jar 直読で確認）。
 * だから宣言をサーバーへ送る必要がなく、同期のコードが要らない。
 *
 * <p>⚠ 裏返すと、本MODを外したクライアントは宣言を素通りできる。
 * 配布クライアントで全員同じ構成なので実用上は問題ないが、厳密な制限ではない。
 *
 * <p>枠は9つ（3×3）。マス目そのものが上限になるので、別に上限値を持たない。
 * ゲームを終了すると消える＝<b>宣言は測量のたびに行う</b>という設計どおり。
 */
public final class RoadMaterials {

    /** 3×3。ここを増やすと「宣言する」意味が薄れる（既定の1,979ブロックに近づく）。 */
    public static final int SLOTS = 9;

    private static final List<Block> slots = new ArrayList<>(SLOTS);

    static {
        for (int i = 0; i < SLOTS; i++) {
            slots.add(null);
        }
    }

    private RoadMaterials() {
    }

    public static Block get(int index) {
        return (index < 0 || index >= SLOTS) ? null : slots.get(index);
    }

    /** すでに入っているブロックは足さない。空きが無ければ false。 */
    public static boolean add(Block block) {
        if (block == null || contains(block)) {
            return false;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (slots.get(i) == null) {
                slots.set(i, block);
                return true;
            }
        }
        return false;
    }

    public static void remove(int index) {
        if (index >= 0 && index < SLOTS) {
            slots.set(index, null);
        }
    }

    public static void clear() {
        for (int i = 0; i < SLOTS; i++) {
            slots.set(i, null);
        }
    }

    public static boolean contains(Block block) {
        if (block == null) {
            return false;
        }
        for (Block b : slots) {
            if (b == block) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty() {
        for (Block b : slots) {
            if (b != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean isFull() {
        for (Block b : slots) {
            if (b == null) {
                return false;
            }
        }
        return true;
    }

    public static int count() {
        int n = 0;
        for (Block b : slots) {
            if (b != null) {
                n++;
            }
        }
        return n;
    }

    /** 道判定の本体。空気は常に道ではない（Via Romana 本体と同じ）。 */
    public static boolean isDeclared(BlockState state) {
        return state != null && !state.isAir() && contains(state.getBlock());
    }
}
