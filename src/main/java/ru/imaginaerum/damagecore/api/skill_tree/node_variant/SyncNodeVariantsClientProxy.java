package ru.imaginaerum.damagecore.api.skill_tree.node_variant;



import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeClientSync;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SyncNodeVariantsClientProxy {
    public static void apply(int treeId, Map<String, Integer> variants) {
        SkillTreeClientSync.applyVariants(treeId, variants);
    }
}
