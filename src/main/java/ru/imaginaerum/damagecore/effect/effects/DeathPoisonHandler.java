package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.effect.DCEffects;

import java.util.Random;

@EventBusSubscriber(value = Dist.CLIENT)
public class DeathPoisonHandler {
    private static final Random random = new Random();
    private static float shakeOffsetX = 0;
    private static float shakeOffsetY = 0;

    // Добавляем переменные для анимации урона
    private static int damageAnimationTicks = 0;
    private static final int DAMAGE_ANIMATION_DURATION = 2; // 2 тика
    private static float lastHealth = 0;

    // --- Спрайты сердец (замена старого icons.png с фиксированными UV) ---
    // В 1.21.x ванильный Gui рисует сердца через GuiGraphics#blitSprite,
    // используя текстуры textures/gui/sprites/hud/heart/*.png
    private static final ResourceLocation SPRITE_WITHERED_CONTAINER =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_container");
    private static final ResourceLocation SPRITE_WITHERED_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_half");
    private static final ResourceLocation SPRITE_WITHERED_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_full");

    private static final ResourceLocation SPRITE_ABSORBING_CONTAINER =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_container");
    private static final ResourceLocation SPRITE_ABSORBING_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half");
    private static final ResourceLocation SPRITE_ABSORBING_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full");

    // Спрайты "вспышки" урона (blink-состояние) — используются для анимации получения урона
    private static final ResourceLocation SPRITE_WITHERED_HALF_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_half_blinking");
    private static final ResourceLocation SPRITE_WITHERED_FULL_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_full_blinking");
    private static final ResourceLocation SPRITE_ABSORBING_HALF_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking");
    private static final ResourceLocation SPRITE_ABSORBING_FULL_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking");

    @SubscribeEvent
    public static void onRenderHealthOverlay(RenderGuiLayerEvent.Pre event) {
        // В 1.21.1 старой системы GuiOverlay больше нет — рендер идёт через слои
        // LayeredDraw, и слой идентифицируется через ResourceLocation (getName()),
        // а не через enum-константу с .type(), как раньше.
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            // Если у игрока есть эффект, отменяем стандартную отрисовку
            if (player != null && player.hasEffect(DCEffects.DEATH_POISON)) {
                event.setCanceled(true); // Pre-событие кансельно (ICancellableEvent)
                // И сразу рисуем наши сердца
                renderPoisonHeartsOverlay(event.getGuiGraphics(), mc, player);
            }
        }
    }

    private static void renderPoisonHeartsOverlay(GuiGraphics guiGraphics, Minecraft mc, Player player) {
        int heartWidth = 9;
        int heartHeight = 9;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int absorption = (int) player.getAbsorptionAmount();

        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = mc.getWindow().getGuiScaledHeight() - 39;

        int totalHealthWithAbsorption = (int) (maxHealth + absorption);
        int healthRows = (int) Math.ceil(totalHealthWithAbsorption / 20.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        int currentHealth = (int) health;
        int heartCount = (totalHealthWithAbsorption + 1) / 2;

        // Определяем, нужно ли анимировать сердца (здоровье <= 5)
        boolean shouldAnimate = (currentHealth <= 5 && currentHealth > 0);
        // Проверяем, активна ли анимация урона
        boolean showDamageAnimation = damageAnimationTicks > 0;

        for (int i = 0; i < heartCount; i++) {
            int row = i / 10;
            int col = i % 10;
            int x = left + col * 8;
            int y = top - row * rowHeight;

            int heartIndex = i * 2;

            // 1. Определяем, какой тип сердца должен быть на этой позиции
            boolean isAbsorption = false;
            boolean renderFull = false;
            boolean renderHalf = false;

            // Проверяем поглощение (рисуется поверх здоровья)
            if (heartIndex < absorption) {
                isAbsorption = true;
                if (heartIndex + 1 <= absorption) {
                    renderFull = true;
                } else if (heartIndex == absorption) {
                    renderHalf = true;
                }
            }

            // Если это не поглощение, проверяем здоровье
            if (!isAbsorption) {
                int healthOffset = heartIndex;
                if (healthOffset + 1 <= currentHealth) {
                    renderFull = true;
                } else if (healthOffset == currentHealth) {
                    renderHalf = true;
                }
                // иначе — сердце пустое (isEmpty), отдельный флаг не нужен
            }

            // 2. Применяем анимацию дрожания, если здоровье низкое
            float animatedX = x;
            float animatedY = y;
            if (shouldAnimate && !isAbsorption) {
                animatedX += shakeOffsetX;
                animatedY += shakeOffsetY;
            }

            // 3. Рисуем сердце через blitSprite (замена старого blit с UV из icons.png)
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.98F);

            ResourceLocation containerSprite = isAbsorption ? SPRITE_ABSORBING_CONTAINER : SPRITE_WITHERED_CONTAINER;
            ResourceLocation fullSprite;
            ResourceLocation halfSprite;

            if (showDamageAnimation) {
                fullSprite = isAbsorption ? SPRITE_ABSORBING_FULL_BLINKING : SPRITE_WITHERED_FULL_BLINKING;
                halfSprite = isAbsorption ? SPRITE_ABSORBING_HALF_BLINKING : SPRITE_WITHERED_HALF_BLINKING;
            } else {
                fullSprite = isAbsorption ? SPRITE_ABSORBING_FULL : SPRITE_WITHERED_FULL;
                halfSprite = isAbsorption ? SPRITE_ABSORBING_HALF : SPRITE_WITHERED_HALF;
            }

            // Пустой контейнер сердца рисуется всегда как фон
            guiGraphics.blitSprite(containerSprite, (int) animatedX, (int) animatedY, heartWidth, heartHeight);

            // Поверх — полное или половинчатое сердце, если нужно
            if (renderFull) {
                guiGraphics.blitSprite(fullSprite, (int) animatedX, (int) animatedY, heartWidth, heartHeight);
            } else if (renderHalf) {
                guiGraphics.blitSprite(halfSprite, (int) animatedX, (int) animatedY, heartWidth, heartHeight);
            }

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Сбрасываем цвет
        }
    }

    // Объединённый метод для всех тиковых событий на клиенте
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Обновляем анимацию урона
        if (damageAnimationTicks > 0) {
            damageAnimationTicks--;
        }

        // Проверяем изменения здоровья для запуска анимации
        float currentHealth = player.getHealth();
        if (currentHealth < lastHealth) {
            // Игрок получил урон - запускаем анимацию
            damageAnimationTicks = DAMAGE_ANIMATION_DURATION;
        }
        lastHealth = currentHealth;

        // Генерируем новое резкое смещение для дрожания каждый тик
        shakeOffsetX = (random.nextFloat() - 0.5f) * 1.5f;
        shakeOffsetY = (random.nextFloat() - 0.5f) * 1.5f;
    }

    // Дополнительный обработчик для гарантированного отслеживания урона
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && player == Minecraft.getInstance().player) {
            // Если игрок получил урон и у него есть эффект смерти-яда
            if (player.hasEffect(DCEffects.DEATH_POISON)) {
                // Запускаем анимацию урона
                damageAnimationTicks = DAMAGE_ANIMATION_DURATION;
            }
        }
    }
}