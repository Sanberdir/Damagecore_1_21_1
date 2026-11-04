package ru.imaginaerum.damagecore.mixin.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;
import ru.imaginaerum.damagecore.Init.fishing.weight.BendConsumer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique
    private static boolean df$active;

    @Inject(method = "render", at = @At("HEAD"))
    private void df$begin(ItemStack stack, ItemDisplayContext ctx, boolean left, PoseStack ps,
                          MultiBufferSource buf, int light, int overlay, BakedModel model, CallbackInfo ci) {
        // только своя удочка в руке от первого лица
        df$active = ctx.firstPerson()
                && stack.getItem() instanceof FishingRodItem
                && Math.abs(FightClient.rodBend) > 0.002f;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void df$end(ItemStack stack, ItemDisplayContext ctx, boolean left, PoseStack ps,
                        MultiBufferSource buf, int light, int overlay, BakedModel model, CallbackInfo ci) {
        df$active = false;
    }
    @Inject(method = "renderModelLists", at = @At("HEAD"))
    private void df$tip(BakedModel model, ItemStack stack, int light, int overlay,
                        PoseStack ps, VertexConsumer c, CallbackInfo ci) {
        if (!df$active) { FightClient.tipDx = FightClient.tipDy = FightClient.tipDz = 0f; return; }
        float tu = 0.94f, tv = 0.94f;                 // позиция кончика на спрайте, где крепится леска
        float off = BendConsumer.offsetAt(FightClient.rodBend, tu, tv);
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Vector3f p0 = m.transformPosition(tu, tv, 0.5f, new org.joml.Vector3f());
        org.joml.Vector3f p1 = m.transformPosition(tu + off, tv - off, 0.5f, new org.joml.Vector3f());
        p1.sub(p0);   // смещение в пространстве вида
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        cam.rotation().transform(p1);                 // вид -> мир
        FightClient.tipDx = p1.x; FightClient.tipDy = p1.y; FightClient.tipDz = p1.z;
    }
    @ModifyVariable(method = "renderModelLists", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer df$wrap(VertexConsumer c) {
        return df$active ? new BendConsumer(c, FightClient.rodBend) : c;
    }
}