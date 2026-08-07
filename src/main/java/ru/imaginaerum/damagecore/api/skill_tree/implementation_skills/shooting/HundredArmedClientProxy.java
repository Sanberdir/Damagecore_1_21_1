package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HundredArmedClientProxy {
    public static void apply(boolean hasSkill) {
        ClientHundredArmedData.hasSkill = hasSkill;
    }
}