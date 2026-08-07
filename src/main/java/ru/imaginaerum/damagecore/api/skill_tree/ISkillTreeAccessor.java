package ru.imaginaerum.damagecore.api.skill_tree;

public interface ISkillTreeAccessor {
    boolean damagecore$isSkillTreeVisible();
    void damagecore$scrollList(double delta); // ← новый метод
}