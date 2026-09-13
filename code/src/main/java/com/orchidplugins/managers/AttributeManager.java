package com.orchidplugins.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class AttributeManager {

    public static final NamespacedKey KEY_SCALE = key("scale");
    public static final NamespacedKey KEY_BLOCK_REACH = key("block_reach");
    public static final NamespacedKey KEY_ENTITY_REACH = key("entity_reach");
    public static final NamespacedKey KEY_SPEED = key("speed");
    public static final NamespacedKey KEY_JUMP = key("jump");
    public static final NamespacedKey KEY_HEALTH = key("health");

    private static NamespacedKey key(String name) {
        return NamespacedKey.fromString("orchid:" + name);
    }

    public void setScale(Player player, double scale) {
        addModifier(player, Attribute.SCALE, KEY_SCALE, scale - 1.0,
                AttributeModifier.Operation.ADD_NUMBER);
    }

    public void setReach(Player player, double blocks) {
        addModifier(player, Attribute.BLOCK_INTERACTION_RANGE, KEY_BLOCK_REACH, blocks,
                AttributeModifier.Operation.ADD_NUMBER);
        addModifier(player, Attribute.ENTITY_INTERACTION_RANGE, KEY_ENTITY_REACH, blocks,
                AttributeModifier.Operation.ADD_NUMBER);
    }

    public void setSpeedMultiplier(Player player, double multiplier) {
        addModifier(player, Attribute.MOVEMENT_SPEED, KEY_SPEED, multiplier - 1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    public void setJumpBoost(Player player, double amount) {
        addModifier(player, Attribute.JUMP_STRENGTH, KEY_JUMP, amount,
                AttributeModifier.Operation.ADD_NUMBER);
    }

    public void setHealth(Player player, double hearts) {
        addModifier(player, Attribute.MAX_HEALTH, KEY_HEALTH, hearts - 20.0,
                AttributeModifier.Operation.ADD_NUMBER);
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(Math.max(1.0, Math.min(hearts, max != null ? max.getValue() : hearts)));
    }

    public void reset(Player player) {
        for (Attribute attribute : new Attribute[]{
                Attribute.SCALE,
                Attribute.BLOCK_INTERACTION_RANGE,
                Attribute.ENTITY_INTERACTION_RANGE,
                Attribute.MOVEMENT_SPEED,
                Attribute.JUMP_STRENGTH,
                Attribute.MAX_HEALTH
        }) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            Collection<AttributeModifier> modifiers = instance.getModifiers();
            for (AttributeModifier modifier : modifiers) {
                if (modifier.getKey().getNamespace().equals("orchid")) {
                    instance.removeModifier(modifier);
                }
            }
        }
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(Math.min(20.0, max != null ? max.getValue() : 20.0));
    }

    private void addModifier(Player player, Attribute attribute, NamespacedKey key, double amount,
                             AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null) {
            instance.removeModifier(existing);
        }
        AttributeModifier modifier = new AttributeModifier(key, amount, operation);
        instance.addModifier(modifier);
    }
}