package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SyncLearnedNodesClientProxy {
    public static void apply(int treeId, List<String> learnedIds) {
        if (Minecraft.getInstance().player == null) return;
        SkillTreeClientSync.applyLearnedNodes(treeId, learnedIds);
    }
}