package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreSource;

@Mixin(DamageSource.class)
public class DamageSourceMixin implements IDamageCoreSource {

    private DamageType damagecore$type;

    @Override
    public DamageType damagecore$getDamageType() {
        return damagecore$type;
    }

    @Override
    public void damagecore$setDamageType(DamageType type) {
        this.damagecore$type = type;
    }

    @Override
    public DamageType getDamageType() {
        return null;
    }
}
