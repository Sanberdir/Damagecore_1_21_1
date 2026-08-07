package ru.imaginaerum.damagecore.armor;

import java.util.HashMap;
import java.util.Map;

public class ArmorMaterialConfig {
    public String material;

    // Храним теперь две карты для каждого типа брони
    public Map<String, Float> helmet_flat = new HashMap<>();  // Абсолютная защита
    public Map<String, Float> helmet_percent = new HashMap<>(); // Процентная защита

    public Map<String, Float> chestplate_flat = new HashMap<>();
    public Map<String, Float> chestplate_percent = new HashMap<>();

    public Map<String, Float> leggings_flat = new HashMap<>();
    public Map<String, Float> leggings_percent = new HashMap<>();

    public Map<String, Float> boots_flat = new HashMap<>();
    public Map<String, Float> boots_percent = new HashMap<>();

    // Глобальные модификаторы для всего набора
    public Map<String, Float> all_flat = new HashMap<>();
    public Map<String, Float> all_percent = new HashMap<>();
}