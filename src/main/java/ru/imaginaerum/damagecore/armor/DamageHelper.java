package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.damagesource.DamageSource;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public class DamageHelper {

    public static DamageType getDamageType(DamageSource source) {

//        // ОГОНЬ
//        if (source.is(DamageTypes.IN_FIRE)
//                || source.is(DamageTypes.ON_FIRE)
//                || source.is(DamageTypes.LAVA)
//                || source.is(DamageTypes.HOT_FLOOR)
//                || source.is(DamageTypes.FIREBALL)) {
//            return DamageType.FIRE;
//        }
//
//        // ВЗРЫВЫ / СИЛОВОЙ
//        if (source.is(DamageTypes.EXPLOSION)
//                || source.is(DamageTypes.PLAYER_EXPLOSION)) {
//            return DamageType.FORCE;
//        }
//
//        // МОЛНИЯ
//        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
//            return DamageType.LIGHTNING;
//        }
//
//        // ЯД
//        if (source.is(DamageTypes.MAGIC)
//                || source.is(DamageTypes.WITHER)) {
//            return DamageType.POISON;
//        }
//
//        // ПАДЕНИЕ — дробящий
//        if (source.is(DamageTypes.FALL)
//                || source.is(DamageTypes.FLY_INTO_WALL)
//                || source.is(DamageTypes.CRAMMING)) {
//            return DamageType.BLUDGEONING;
//        }
//
//        // СНАРЯДЫ — колющий
//        if (source.is(DamageTypes.ARROW)
//                || source.is(DamageTypes.TRIDENT)
//                || source.is(DamageTypes.MOB_PROJECTILE)) {
//            return DamageType.PIERCING;
//        }

        // БЛИЖНИЙ БОЙ — режущий (дефолт)
        return DamageType.SLASHING;
    }
}
