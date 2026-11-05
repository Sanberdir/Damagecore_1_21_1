/*
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package ru.imaginaerum.damagecore.Init.blocks.custom.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IDelegateProvider {

    /**
     * @param blockState состояние блока
     * @param blockPos позиция блока
     * @return позицию блока для делегирования
     */
    BlockPos getDelegatePos(final BlockState blockState, final BlockPos blockPos);
}
