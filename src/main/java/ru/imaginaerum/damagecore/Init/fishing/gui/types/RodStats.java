package ru.imaginaerum.damagecore.Init.fishing.gui.types;

/** Итоговые модификаторы удочки. 1.0 = без изменений. */
public final class RodStats {
    public float biteChance   = 1f;  // шанс клёва на каждую проверку (4 раза/с)
    public float biteRadius   = 1f;  // множитель радиуса поиска рыбы
    public float fightTime    = 1f;  // <1 = бой короче
    public float reelSpeed    = 1f;  // скорость подмотки
    public float lineStrength = 1f;  // >1 = леска прочнее
    public int   hookWear     = 0;   // прочность крюка за один бой
}