package dev.vapemace.client.module.impl;

import dev.vapemace.client.module.Category;
import dev.vapemace.client.module.Module;
import dev.vapemace.client.setting.BooleanSetting;
import dev.vapemace.client.setting.NumberSetting;
import dev.vapemace.client.util.MaceMathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class SilentAimModule extends Module {
    private final NumberSetting fov = new NumberSetting("FOV Radius", "Target acquisition FOV", 90.0, 15.0, 180.0, 5.0);
    private final NumberSetting range = new NumberSetting("Range", "Max target lock distance", 4.5, 2.5, 6.0, 0.1);
    private final NumberSetting smoothSpeed = new NumberSetting("Smooth Speed", "Rotation interpolation speed", 0.35, 0.05, 1.0, 0.05);
    private final BooleanSetting practiceAutoAim = new BooleanSetting("Practice Auto-Rotate", "Applies client-side camera aim in singleplayer/practice", false);

    private float targetYawDelta = 0.0f;
    private float targetPitchDelta = 0.0f;
    private String lockedTargetName = "None";

    public SilentAimModule() {
        super("SilentAim", "UI prototype & aim vector calculator", Category.COMBAT, true);
        addSetting(fov);
        addSetting(range);
        addSetting(smoothSpeed);
        addSetting(practiceAutoAim);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        PlayerEntity target = MaceMathUtil.findNearestTarget(mc, range.getValue());
        if (target == null) {
            lockedTargetName = "None";
            targetYawDelta = 0.0f;
            targetPitchDelta = 0.0f;
            return;
        }

        float[] desired = MaceMathUtil.calculateAngles(mc.player.getEyePos(), target.getBoundingBox().getCenter());
        float yawDiff = MathHelper.wrapDegrees(desired[0] - mc.player.getYaw());
        float pitchDiff = MathHelper.wrapDegrees(desired[1] - mc.player.getPitch());

        if (Math.hypot(yawDiff, pitchDiff) <= fov.getValue()) {
            lockedTargetName = target.getName().getString();
            targetYawDelta = yawDiff;
            targetPitchDelta = pitchDiff;

            if (practiceAutoAim.getValue()) {
                float factor = smoothSpeed.getValue().floatValue();
                mc.player.setYaw(mc.player.getYaw() + yawDiff * factor);
                mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + pitchDiff * factor, -90.0f, 90.0f));
            }
        } else {
            lockedTargetName = "Out of FOV";
        }
    }

    @Override
    public String getHudStatus() {
        return practiceAutoAim.getValue() ? "Assist [" + lockedTargetName + "]" : "UI-Proto [" + lockedTargetName + "]";
    }

    public float getTargetYawDelta() { return targetYawDelta; }
    public float getTargetPitchDelta() { return targetPitchDelta; }
}
