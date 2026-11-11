package com.example.biomeatmosphere;

import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Динамический свет от предметов в руках.
 *
 * Как работает: у головы игрока в КЛИЕНТСКОМ мире ставится невидимый блок Light нужной яркости.
 * Сервер о нём ничего не знает, в сохранение мира он не попадает. Дальше свет считает обычный
 * движок освещения, поэтому есть тени от стен, а Sodium и шейдеры видят его как обычный свет.
 * Модель предмета в руке освещается окружающим светом, так что факел в руке тоже "горит".
 */
final class HeldLight {
    /** Только клиенту и без обновления соседей и дропа. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** Предметы, которые не являются блоками, но светятся. */
    private static final Map<Item, Integer> SPECIAL = Map.of(
            Items.LAVA_BUCKET, 15,
            Items.NETHER_STAR, 12,
            Items.BLAZE_ROD, 10,
            Items.FIRE_CHARGE, 10,
            Items.GLOW_BERRIES, 10,
            Items.GLOWSTONE_DUST, 8,
            Items.BLAZE_POWDER, 8,
            Items.GLOW_INK_SAC, 8,
            Items.SPECTRAL_ARROW, 8);

    private static ClientLevel trackedLevel;
    private static BlockPos placedPos;
    private static int refresh;

    private HeldLight() {}

    /** Забыть всё без изменения мира (выход из мира, смена мира). */
    static void reset() {
        trackedLevel = null;
        placedPos = null;
        refresh = 0;
    }

    static void tick(ClientLevel level, LocalPlayer player) {
        if (trackedLevel != level) {
            trackedLevel = level;
            placedPos = null; // старого мира уже нет, чистить нечего
        }

        int want = Config.DYNAMIC_LIGHTS_ENABLED.get() ? heldLevel(player) : 0;
        BlockPos target = want > 0 ? pickPosition(level, player) : null;

        if (target == null) {
            removePlaced(level);
            return;
        }

        BlockState wanted = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, want);

        if (target.equals(placedPos)) {
            if (level.getBlockState(target).equals(wanted)) {
                // всё на месте; раз в полсекунды подталкиваем движок света:
                // сервер мог прислать свои данные освещения поверх наших
                if (++refresh >= 10) {
                    refresh = 0;
                    level.getLightEngine().checkBlock(target);
                }
            } else {
                level.setBlock(target, wanted, FLAGS); // сменилась яркость или блок пропал после ресинка
            }
            return;
        }

        // сначала ставим новый свет, потом убираем старый: без мерцания
        if (level.setBlock(target, wanted, FLAGS)) {
            BlockPos old = placedPos;
            placedPos = target;
            refresh = 0;
            if (old != null) removeAt(level, old);
        }
    }

    // ---------------------------------------------------------------- выбор позиции и яркости

    private static BlockPos pickPosition(ClientLevel level, LocalPlayer player) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (usable(level, eye)) return eye;
        BlockPos feet = player.blockPosition();
        if (usable(level, feet)) return feet;
        return null; // голова и ноги в воде или в блоках: света нет
    }

    /** Ставим только в воздух или поверх собственного блока света. Чужие блоки не трогаем. */
    private static boolean usable(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || (pos.equals(placedPos) && state.is(Blocks.LIGHT));
    }

    private static int heldLevel(LocalPlayer player) {
        if (player.isSpectator() || !player.isAlive()) return 0;
        int level = Math.max(itemLight(player.getMainHandItem()), itemLight(player.getOffhandItem()));
        return Math.min(level, Config.DYNAMIC_LIGHTS_MAX.get());
    }

    private static int itemLight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            int emission = blockItem.getBlock().defaultBlockState().getLightEmission();
            if (emission > 0) return emission; // факелы, фонари, глоустоун, модовые блоки и т.д.
        }
        return SPECIAL.getOrDefault(item, 0);
    }

    // ---------------------------------------------------------------- уборка

    private static void removePlaced(ClientLevel level) {
        if (placedPos != null) {
            removeAt(level, placedPos);
            placedPos = null;
        }
    }

    private static void removeAt(ClientLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }
}
