package ru.imaginaerum.damagecore.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Damagecore_1_21_1_neo.MODID, helper);
    }

    @Override
    protected void registerModels() {
        var planks = modLoc("block/sunken_planks");

        // Блоки, у которых предмет = модель блока
        blockItem("sunken_stairs");
        blockItem("sunken_slab");
        blockItem("sunken_fence_gate");
        blockItem("sunken_pressure_plate");

        // Забор и кнопка используют инвентарные модели
        withExistingParent("sunken_fence", mcLoc("block/fence_inventory"))
                .texture("texture", planks);
        withExistingParent("sunken_button", mcLoc("block/button_inventory"))
                .texture("texture", planks);
    }

    private void blockItem(String name) {
        withExistingParent(name, modLoc("block/" + name));
    }
}