package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ru.imaginaerum.damagecore.effect.DCEffects;
import ru.imaginaerum.damagecore.particle.DCParticles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(value = Dist.CLIENT)
public class StunClientHandler {
    // 4 частицы - 2 на каждое кольцо
    private static final int PARTICLE_COUNT = 4;
    private static final int PARTICLES_PER_RING = 2;
    private static final float RADIUS = 0.4f;
    private static final float HEIGHT_OFFSET = 0.1f;
    private static final int RESPAWN_INTERVAL = 2;

    // Угол наклона колец в радианах (15 градусов)
    private static final float TILT_ANGLE = (float) Math.toRadians(15);
    private static final float COS_TILT = Mth.cos(TILT_ANGLE);
    private static final float SIN_TILT = Mth.sin(TILT_ANGLE);

    // Разные скорости мелькания для каждой частицы
    private static final float[] FLASH_SPEEDS = {2.0f, 2.5f, 3.0f, 3.5f};
    private static final Map<UUID, Integer> playerTickCounter = new HashMap<>();

    // Для хранения исходного состояния камеры
    private static float originalXRot = 0;
    private static float originalYRot = 0;
    private static boolean isCameraFixed = false;

    // Флаг для отслеживания приседания
    private static boolean wasCrouching = false;

    // Параметры шатания (качки)
    private static final float SWAY_AMPLITUDE_X = 2.0f;  // Амплитуда по X (градусы)
    private static final float SWAY_AMPLITUDE_Y = 1.5f;  // Амплитуда по Y (градусы)
    private static final float SWAY_SPEED_X = 0.8f;      // Скорость по X
    private static final float SWAY_SPEED_Y = 1.2f;      // Скорость по Y
    private static final float SWAY_NOISE_AMPLITUDE = 0.5f; // Амплитуда случайных рывков
    private static final float SWAY_DECAY = 0.1f;        // Затухание шатания

    // Текущее смещение камеры
    private static float currentSwayX = 0;
    private static float currentSwayY = 0;
    private static float swayPhaseX = 0;
    private static float swayPhaseY = 0;

    // Для плавного восстановления камеры
    private static boolean isRestoringCamera = false;
    private static float restoreStartXRot = 0;
    private static float restoreStartYRot = 0;
    private static int restoreTicks = 0;
    private static final int RESTORE_DURATION = 20; // 1 секунда (20 тиков) на восстановление

    @SubscribeEvent
    public static void onClientTickStunning(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        UUID playerId = player.getUUID();
        boolean hasEffect = player.hasEffect(DCEffects.STUNNING);

        if (hasEffect) {
            // --- ЭФФЕКТ АКТИВЕН ---
            // Отменяем восстановление камеры, если эффект вернулся
            isRestoringCamera = false;

            // Блокировка управления
            disablePlayerControls(mc);

            // Принудительное приседание
            forceCrouching(player);

            // Фиксация камеры с шатанием
            fixCameraWithSway(mc, player);

            // Считаем тики для этого игрока
            int tickCount = playerTickCounter.getOrDefault(playerId, 0);
            tickCount++;

            // Создаем/обновляем частицы
            if (tickCount % RESPAWN_INTERVAL == 0) {
                createStunParticles(mc.level, player, tickCount);
            }
            playerTickCounter.put(playerId, tickCount);

        } else {
            // --- ЭФФЕКТ ОТСУТСТВУЕТ ---
            // Этот блок выполняется только когда эффекта НЕТ.

            // Если камера была зафиксирована, но восстановление еще не началось
            if (isCameraFixed && !isRestoringCamera) {
                // Начинаем процесс восстановления
                startCameraRestoration(player);
            }

            // Если идет процесс восстановления камеры
            if (isRestoringCamera) {
                restoreCameraSmoothly(mc, player);
            } else if (isCameraFixed) {
                // Если камера была зафиксирована, но восстановление не началось (сбой состояния),
                // принудительно сбрасываем все.
                // Это защита от застревания в состоянии оглушения.
                finishStunEffect(player, mc);
            }
        }
    }
    private static void finishStunEffect(Player player, Minecraft mc) {
        // Восстанавливаем исходное состояние приседания
        restoreCrouching(player);

        // Сбрасываем все флаги и счетчики, связанные с оглушением
        isCameraFixed = false;
        isRestoringCamera = false;
        currentSwayX = 0;
        currentSwayY = 0;

        // Сбрасываем счетчик тиков, когда эффект полностью завершен
        playerTickCounter.remove(player.getUUID());
    }
    private static void disablePlayerControls(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);

        // Блокируем переключение перспективы
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void forceCrouching(Player player) {
        // Сохраняем исходное состояние приседания
        if (!wasCrouching) {
            wasCrouching = player.isCrouching();
        }

        // Принудительно ставим в присед через сеттер позиции
        if (!player.isCrouching()) {
            player.setShiftKeyDown(true);
            // Устанавливаем позу приседания
            player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        }
    }

    private static void restoreCrouching(Player player) {
        // Восстанавливаем исходное состояние приседания
        player.setShiftKeyDown(wasCrouching);

        // Если игрок не должен был приседать, сбрасываем позу
        if (!wasCrouching) {
            // Проверяем, находится ли игрок в блоке (при приседании в блоке поза остается CROUCHING)
            if (!player.isPassenger() && !player.isSleeping()) {
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
        wasCrouching = false;
    }

    private static void fixCameraWithSway(Minecraft mc, Player player) {
        if (!isCameraFixed) {
            // Сохраняем исходное вращение камеры
            originalXRot = player.getXRot();
            originalYRot = player.getYRot();
            isCameraFixed = true;

            // Сбрасываем фазы шатания
            swayPhaseX = (float) (Math.random() * 2 * Math.PI);
            swayPhaseY = (float) (Math.random() * 2 * Math.PI);
        }

        // Применяем шатание к камере
        applySwayToCamera(mc, player, playerTickCounter.getOrDefault(player.getUUID(), 0));

        // Обновляем фазы шатания
        updateSwayPhases();
    }

    private static void applySwayToCamera(Minecraft mc, Player player, int tickCount) {
        if (!isCameraFixed) return;

        float time = tickCount * 0.05f;

        // Основное шатание - синусоидальные волны
        float swayX = Mth.sin(time * SWAY_SPEED_X + swayPhaseX) * SWAY_AMPLITUDE_X;
        float swayY = Mth.sin(time * SWAY_SPEED_Y + swayPhaseY) * SWAY_AMPLITUDE_Y;

        // Добавляем немного случайности для более реалистичного эффекта
        float randomJerkX = (float) ((Math.random() - 0.5) * 2 * SWAY_NOISE_AMPLITUDE);
        float randomJerkY = (float) ((Math.random() - 0.5) * 2 * SWAY_NOISE_AMPLITUDE);

        // Накладываем случайные рывки
        if (Math.random() < 0.1) { // 10% шанс на рывок каждый тик
            currentSwayX = swayX + randomJerkX;
            currentSwayY = swayY + randomJerkY;
        } else {
            // Плавное изменение
            currentSwayX += (swayX - currentSwayX) * 0.1f;
            currentSwayY += (swayY - currentSwayY) * 0.1f;
        }

        // Применяем смещение к камере
        float targetXRot = originalXRot + currentSwayX;
        float targetYRot = originalYRot + currentSwayY;

        // Плавное изменение углов камеры
        float currentXRot = player.getXRot();
        float currentYRot = player.getYRot();

        player.setXRot(currentXRot + (targetXRot - currentXRot) * 0.3f);
        player.setYRot(currentYRot + (targetYRot - currentYRot) * 0.3f);

        // Также фиксируем камеру игрока в Minecraft
        if (mc.cameraEntity != null) {
            mc.cameraEntity.setXRot(player.getXRot());
            mc.cameraEntity.setYRot(player.getYRot());
        }
    }

    private static void updateSwayPhases() {
        // Медленно изменяем фазы для разнообразия шатания
        swayPhaseX += 0.01f;
        swayPhaseY += 0.015f;

        // Ограничиваем фазы в пределах 2π
        if (swayPhaseX > 2 * Math.PI) swayPhaseX -= 2 * Math.PI;
        if (swayPhaseY > 2 * Math.PI) swayPhaseY -= 2 * Math.PI;
    }

    private static void applySwayDecay() {
        // Плавное затухание шатания
        currentSwayX *= (1 - SWAY_DECAY);
        currentSwayY *= (1 - SWAY_DECAY);

        // Обнуляем очень маленькие значения
        if (Math.abs(currentSwayX) < 0.01f) currentSwayX = 0;
        if (Math.abs(currentSwayY) < 0.01f) currentSwayY = 0;
    }

    private static void startCameraRestoration(Player player) {
        isRestoringCamera = true;
        restoreStartXRot = player.getXRot();
        restoreStartYRot = player.getYRot();
        restoreTicks = 0;
    }

    private static void restoreCameraSmoothly(Minecraft mc, Player player) {
        if (!isRestoringCamera) return;

        restoreTicks++;

        // Вычисляем прогресс восстановления (от 0 до 1)
        float progress = Math.min((float) restoreTicks / RESTORE_DURATION, 1.0f);

        // Плавная интерполяция с использованием функции easeOutCubic
        float easeProgress = 1 - (float) Math.pow(1 - progress, 3);

        // Интерполируем от текущего положения камеры к оригинальному
        float targetXRot = restoreStartXRot + (originalXRot - restoreStartXRot) * easeProgress;
        float targetYRot = restoreStartYRot + (originalYRot - restoreStartYRot) * easeProgress;

        // Применяем интерполированные значения
        player.setXRot(targetXRot);
        player.setYRot(targetYRot);

        // Также фиксируем камеру игрока в Minecraft
        if (mc.cameraEntity != null) {
            mc.cameraEntity.setXRot(player.getXRot());
            mc.cameraEntity.setYRot(player.getYRot());
        }

        // Если восстановление завершено
        if (progress >= 1.0f) {
            isRestoringCamera = false;
            isCameraFixed = false;
            currentSwayX = 0;
            currentSwayY = 0;

            // Разблокируем камеру
            if (Math.abs(player.getXRot() - originalXRot) < 1.0f) {
                player.setXRot(originalXRot);
            }
            if (Math.abs(player.getYRot() - originalYRot) < 1.0f) {
                player.setYRot(originalYRot);
            }
        }
    }

    private static void createStunParticles(ClientLevel level, Player player, int tickCount) {
        if (level == null) return;

        // Ускоренное вращение
        float time = tickCount * 0.3f;
        double headY = player.getY() + player.getEyeHeight() + HEIGHT_OFFSET;
        double centerX = player.getX();
        double centerZ = player.getZ();

        // Создаем 4 частицы - по 2 на каждое кольцо
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Определяем к какому кольцу относится частица
            boolean isFirstRing = i < PARTICLES_PER_RING; // частицы 0,1 - первое кольцо, 2,3 - второе

            // Направление вращения: первое кольцо - по часовой, второе - против часовой
            float direction = isFirstRing ? 1.0f : -1.0f;

            // Угол для частицы на кольце
            int ringParticleIndex = i % PARTICLES_PER_RING;
            float angle = (float) (2 * Math.PI * ringParticleIndex / PARTICLES_PER_RING) + (time * direction);

            // Координаты на плоском горизонтальном кольце
            double baseX = Math.cos(angle) * RADIUS;
            double baseY = 0; // изначально на одной высоте
            double baseZ = Math.sin(angle) * RADIUS;

            // Наклоняем кольца в противоположные стороны
            double offsetX, offsetY, offsetZ;
            if (isFirstRing) {
                // Первое кольцо: наклон на +15 градусов вокруг оси Z
                offsetX = baseX * COS_TILT - baseY * SIN_TILT;
                offsetY = baseX * SIN_TILT + baseY * COS_TILT;
                offsetZ = baseZ;
            } else {
                // Второе кольцо: наклон на -15 градусов вокруг оси Z
                // cos(-a) = cos(a), sin(-a) = -sin(a)
                offsetX = baseX * COS_TILT - baseY * (-SIN_TILT); // baseX * COS_TILT + baseY * SIN_TILT
                offsetY = baseX * (-SIN_TILT) + baseY * COS_TILT; // -baseX * SIN_TILT + baseY * COS_TILT
                offsetZ = baseZ;
            }

            // Вертикальное колебание
            float verticalOffset = Mth.sin(time * 18 + i) * 0.05f;

            double x = centerX + offsetX;
            double y = headY + offsetY + verticalOffset;
            double z = centerZ + offsetZ;

            // Цвет на основе индекса (hue)
            float hue = i / (float) PARTICLE_COUNT;
            // Скорость мелькания для этой частицы
            float flashSpeed = FLASH_SPEEDS[i % FLASH_SPEEDS.length];

            // Создаем частицы с мельканием
            level.addParticle(
                    DCParticles.STUN.get(),
                    x, y, z,
                    hue,      // Цвет через xSpeed
                    flashSpeed, // Скорость мелькания через ySpeed
                    direction  // Направление вращения через zSpeed
            );
        }
    }
}