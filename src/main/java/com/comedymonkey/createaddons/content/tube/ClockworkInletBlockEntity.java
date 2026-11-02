package com.comedymonkey.createaddons.content.tube;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What an Inlet takes and when (design/specs/clockwork-tubes.md D15, D19, D21, D22, D28). Its network's Receiver asks it
 * for a load on its turn; it takes from whatever is in front of its mouth: an item handler (a chest, barrel, vault, a
 * depot or a belt), or items lying in the block space there. One item, or for the brass one up to its filter's count
 * (a stack with no filter), only what its filter passes. The gulp is sent to the client as a block event.
 */
public class ClockworkInletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** Block event: it took something (the client plays the gulp). */
	public static final int GULP = 1;

	@Nullable
	private FilteringBehaviour filtering;
	/** Server: the game time it last took a load (it waits the cooldown after each). */
	private long lastPull = Long.MIN_VALUE / 2;
	/** Server: the game time a Receiver last counted it in its network; the goggles say when nobody has lately. */
	private long lastSeen = Long.MIN_VALUE / 2;
	/** Synced: on a network with a Receiver. */
	private boolean onNetwork;
	/** Client: the animation time of its last gulp. */
	public double gulpAt = -100;

	public ClockworkInletBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CLOCKWORK_INLET.get(), pos, state);
	}

	public boolean isBrass() {
		return getBlockState().getBlock() instanceof ClockworkInletBlock inlet && inlet.isBrass();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		if (isBrass()) {
			filtering = new FilteringBehaviour(this, new InletFilterSlot()).showCount();
			behaviours.add(filtering);
		}
	}

	public Direction mouth() {
		return getBlockState().getValue(ClockworkInletBlock.FACING);
	}

	public boolean isPowered() {
		return getBlockState().getValue(ClockworkInletBlock.POWERED);
	}

	/** Ready for its turn: not switched off, its cooldown over. */
	public boolean ready(long now, int cooldown) {
		return !isPowered() && now - lastPull >= cooldown;
	}

	/** A Receiver counted it in its network now. */
	void seen(long now) {
		lastSeen = now;
		if (!onNetwork) {
			onNetwork = true;
			sendData();
		}
	}

	/** Its chunk loaded: Receivers read their networks again. */
	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide)
			TubeJoints.changed(level, worldPosition);
	}

	@Override
	public void tick() {
		super.tick();
		// nobody has counted it for two seconds: it's on no Receiver's network
		if (level != null && !level.isClientSide && onNetwork && level.getGameTime() - lastSeen > 40) {
			onNetwork = false;
			sendData();
		}
	}

	/** Most it takes at once: one item, or the brass one's filter count (a stack with no count set). */
	int amount() {
		if (filtering == null)
			return 1;
		return filtering.anyAmount() ? 64 : Math.max(1, filtering.getAmount());
	}

	boolean passes(ItemStack stack) {
		return filtering == null || filtering.test(stack);
	}

	/** The brass one's filter set to "exactly" (as a Brass Funnel's): it takes only when the whole count is there. */
	boolean exactly() {
		return filtering != null && !filtering.upTo && !filtering.anyAmount();
	}

	/**
	 * Takes a load from in front of its mouth, if there's anything it may take (server). Empty: nothing taken. A real
	 * take: whatever it returns is gone from where it was.
	 */
	public ItemStack pull(long now) {
		if (level == null)
			return ItemStack.EMPTY;
		Direction mouth = mouth();
		BlockPos front = worldPosition.relative(mouth);
		if (!level.isLoaded(front))
			return ItemStack.EMPTY;
		int amount = amount();
		ItemStack taken = ItemStack.EMPTY;
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, front, mouth.getOpposite());
		if (handler != null)
			taken = takeFrom(handler, amount);
		else if (level.getBlockState(front).getCollisionShape(level, front).isEmpty())
			taken = takeLying(level, front, amount);
		if (!taken.isEmpty()) {
			lastPull = now;
			level.blockEvent(worldPosition, getBlockState().getBlock(), GULP, 0);
		}
		return taken;
	}

	/** Up to {@code amount} of the first stack its filter passes. */
	private ItemStack takeFrom(IItemHandler handler, int amount) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack there = handler.getStackInSlot(slot);
			if (there.isEmpty() || !passes(there))
				continue;
			ItemStack test = handler.extractItem(slot, amount, true);
			if (test.isEmpty() || !passes(test) || exactly() && test.getCount() < amount)
				continue;
			return handler.extractItem(slot, test.getCount(), false);
		}
		return ItemStack.EMPTY;
	}

	/** Up to {@code amount} from the first item lying in the block space in front of its mouth that its filter passes. */
	private ItemStack takeLying(Level level, BlockPos front, int amount) {
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(front), e -> e.isAlive() && !e.getItem().isEmpty())) {
			ItemStack there = item.getItem();
			if (!passes(there) || exactly() && there.getCount() < amount)
				continue;
			ItemStack rest = there.copy();
			ItemStack taken = rest.split(amount);
			if (rest.isEmpty())
				item.discard();
			else
				item.setItem(rest);
			return taken;
		}
		return ItemStack.EMPTY;
	}

	/** Client: the gulp. */
	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == GULP) {
			if (level != null && level.isClientSide)
				gulpAt = net.createmod.catnip.animation.AnimationTickHolder.getTicks(level)
					+ net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks(level);
			return true;
		}
		return super.triggerEvent(id, param);
	}

	// ---------- goggles and saving ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate(isBrass() ? "gui.goggles.brass_clockwork_inlet" : "gui.goggles.clockwork_inlet").forGoggles(tooltip);
		if (isPowered())
			lang().translate("gui.goggles.clockwork_inlet.powered").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		else if (!onNetwork)
			lang().translate("gui.goggles.clockwork_inlet.no_receiver").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		else
			lang().translate("gui.goggles.clockwork_inlet.taking", amount()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!clientPacket)
			tag.putLong("LastPull", lastPull);
		tag.putBoolean("OnNetwork", onNetwork);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (!clientPacket && tag.contains("LastPull"))
			lastPull = tag.getLong("LastPull");
		onNetwork = tag.getBoolean("OnNetwork");
	}
}
