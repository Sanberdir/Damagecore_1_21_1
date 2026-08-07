package ru.imaginaerum.damagecore.api.skill_tree;



import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SyncNodeLevelsClientProxy {
    public static void apply(int treeId, Map<String, Integer> levels) {
        SkillTreeClientSync.applyNodeLevels(treeId, levels);
    }
}