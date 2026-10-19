package com.comedymonkey.createaddons.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One advancement trigger for everything the mod does that vanilla triggers can't see: fitting upgrades, pressing
 * in the Crucible, piping fuel... Each use names an {@code action}, optionally an {@code item} and a minimum
 * {@code value} (such as how many upgrades are now fitted).
 *
 * <pre>{"trigger": "comedymonkeysaddons:action",
 *  "conditions": {"action": "wellspring_upgrade", "item": {"items": "comedymonkeysaddons:water_upgrade"}, "min_value": 3}}</pre>
 */
public class CMActionTrigger extends SimpleCriterionTrigger<CMActionTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, String action, ItemStack stack, int value) {
		trigger(player, instance -> instance.matches(action, stack, value));
	}

	public record Instance(Optional<ContextAwarePredicate> player, String action, Optional<ItemPredicate> item,
						   Optional<Integer> minValue) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
			EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
			Codec.STRING.fieldOf("action").forGetter(Instance::action),
			ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Instance::item),
			Codec.INT.optionalFieldOf("min_value").forGetter(Instance::minValue)
		).apply(i, Instance::new));

		boolean matches(String action, ItemStack stack, int value) {
			return this.action.equals(action)
				&& item.map(predicate -> predicate.test(stack)).orElse(true)
				&& minValue.map(min -> value >= min).orElse(true);
		}
	}
}
