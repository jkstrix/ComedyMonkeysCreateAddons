package com.comedymonkey.createaddons.gametest;

import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.comedymonkey.createaddons.content.helperbot.HelperBotDyeRecipe;
import com.comedymonkey.createaddons.content.helperbot.HelperBotMovement;
import com.comedymonkey.createaddons.content.helperbot.HelperBotTestSetup;
import com.comedymonkey.createaddons.content.helperbot.HelperBots;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Helper Bot in a real world (./gradlew runGameTestServer): dyeing and washing him, his colour kept when he's broken,
 * ducking, turning to the controls, and, on a short version of the /helperbot testsetup line, Create assembling a
 * train with him as its conductor at both ends and a third bot riding as a passenger.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperBotGameTests {
	/** 24 x 12 x 24 of air (the Battery tests' course). */
	private static final String COURSE = "battery_course";

	private HelperBotGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(HelperBotGameTests.class);
	}

	private static ItemStack bot(DyeColor dye) {
		ItemStack stack = new ItemStack(ModItems.HELPER_BOT.get());
		if (dye != null)
			stack.set(ModDataComponents.HELPER_BOT_DYE.get(), dye);
		return stack;
	}

	/** Places a bot the way a player does, from the item (so its components go through the real path). */
	private static void placeFromItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos abs = helper.absolutePos(pos);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
	}

	private static void placeBot(GameTestHelper helper, BlockPos pos, Direction facing, DyeColor dye) {
		helper.setBlock(pos, ModBlocks.HELPER_BOT.get().defaultBlockState().setValue(HelperBotBlock.FACING, facing));
		if (dye != null && helper.getBlockEntity(pos) instanceof HelperBotBlockEntity be) {
			CompoundTag nbt = new CompoundTag();
			nbt.putString(HelperBotBlockEntity.DYE, dye.getName());
			be.loadWithComponents(nbt, helper.getLevel().registryAccess());
		}
	}

	/** One dye in a crafting table paints a bot, plain or already dyed (D5, D16). */
	@GameTest(template = COURSE)
	public static void dyeingPaintsHim(GameTestHelper helper) {
		for (ItemStack from : List.of(bot(null), bot(DyeColor.BLUE))) {
			from.set(DataComponents.CUSTOM_NAME, Component.literal("Bolt"));
			CraftingInput input = CraftingInput.of(2, 1, List.of(from, new ItemStack(Items.RED_DYE)));
			var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
			if (recipe.isEmpty())
				helper.fail("no dye recipe for " + from.get(ModDataComponents.HELPER_BOT_DYE.get()));
			ItemStack out = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
			helper.assertTrue(out.is(ModItems.HELPER_BOT.get()) && out.getCount() == 1, "dyeing should make one Helper Bot, made " + out);
			helper.assertTrue(out.get(ModDataComponents.HELPER_BOT_DYE.get()) == DyeColor.RED, "dyed bot should be red, is " + out.get(ModDataComponents.HELPER_BOT_DYE.get()));
			helper.assertTrue(Component.literal("Bolt").equals(out.get(DataComponents.CUSTOM_NAME)), "dyeing should keep his name, has " + out.get(DataComponents.CUSTOM_NAME));
		}
		helper.succeed();
	}

	/** A dedicated server sends the dye recipes to players: they arrive whole, and still keep his name. */
	@GameTest(template = COURSE)
	public static void dyeRecipesSurviveTheTripToAPlayer(GameTestHelper helper) {
		RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
			.byKey(ResourceLocation.fromNamespaceAndPath(ComedyMonkeysCreateAddons.MOD_ID, "dyed/red_helper_bot")).orElse(null);
		if (holder == null || !(holder.value() instanceof HelperBotDyeRecipe recipe))
			throw new GameTestAssertException("red_helper_bot isn't a Helper Bot dye recipe: " + holder);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		HelperBotDyeRecipe.Serializer serializer = (HelperBotDyeRecipe.Serializer) recipe.getSerializer();
		serializer.streamCodec().encode(buf, recipe);
		HelperBotDyeRecipe sent = serializer.streamCodec().decode(buf);
		helper.assertTrue(buf.readableBytes() == 0, "the recipe should be read back whole");
		ItemStack named = bot(null);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Bolt"));
		CraftingInput input = CraftingInput.of(2, 1, List.of(named, new ItemStack(Items.RED_DYE)));
		helper.assertTrue(sent.matches(input, helper.getLevel()), "the player's copy should match a bot and red dye");
		ItemStack out = sent.assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(out.get(ModDataComponents.HELPER_BOT_DYE.get()) == DyeColor.RED, "the player's copy should dye him red");
		helper.assertTrue(Component.literal("Bolt").equals(out.get(DataComponents.CUSTOM_NAME)), "and keep his name");
		helper.succeed();
	}

	/** Bulk washing takes every colour off, and leaves a plain bot alone (D11). */
	@GameTest(template = COURSE)
	public static void washingMakesHimPlain(GameTestHelper helper) {
		RecipeHolder<?> washing = helper.getLevel().getRecipeManager()
			.byKey(ResourceLocation.fromNamespaceAndPath(ComedyMonkeysCreateAddons.MOD_ID, "washing/helper_bot")).orElse(null);
		if (washing == null)
			helper.fail("no washing recipe for Helper Bot");
		Recipe<?> recipe = washing.value();
		for (DyeColor dye : DyeColor.values())
			helper.assertTrue(recipe.getIngredients().get(0).test(bot(dye)), "washing should take " + dye + " off");
		helper.assertFalse(recipe.getIngredients().get(0).test(bot(null)), "a plain bot has nothing to wash off");
		ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
		helper.assertTrue(result.is(ModItems.HELPER_BOT.get()) && !result.has(ModDataComponents.HELPER_BOT_DYE.get()), "washing should give a plain bot, gave " + result);
		ItemStack named = bot(DyeColor.RED);
		named.setCount(3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Bolt"));
		List<ItemStack> washed = RecipeApplier.applyRecipeOn(helper.getLevel(), named, recipe, false);
		helper.assertTrue(washed.size() == 1 && washed.get(0).getCount() == 3 && !washed.get(0).has(ModDataComponents.HELPER_BOT_DYE.get()),
			"the fan should wash 3 bots into 3 plain bots, gave " + washed);
		helper.assertTrue(Component.literal("Bolt").equals(washed.get(0).get(DataComponents.CUSTOM_NAME)), "washing should keep his name");
		helper.succeed();
	}

	/** A dyed, named bot placed from the item keeps his colour and his name when he's broken (D11). */
	@GameTest(template = COURSE)
	public static void keepsHisColourWhenBroken(GameTestHelper helper) {
		BlockPos pos = new BlockPos(5, 1, 5);
		ItemStack stack = bot(DyeColor.LIME);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Bolt"));
		placeFromItem(helper, pos, stack);
		helper.assertBlockPresent(ModBlocks.HELPER_BOT.get(), pos);
		BlockPos abs = helper.absolutePos(pos);
		BlockState state = helper.getLevel().getBlockState(abs);
		List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), abs, helper.getLevel().getBlockEntity(abs),
			null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.HELPER_BOT.get()), "should drop one Helper Bot, dropped " + drops);
		helper.assertTrue(drops.get(0).get(ModDataComponents.HELPER_BOT_DYE.get()) == DyeColor.LIME, "should keep his lime, has " + drops.get(0).get(ModDataComponents.HELPER_BOT_DYE.get()));
		helper.assertTrue(Component.literal("Bolt").equals(drops.get(0).get(DataComponents.CUSTOM_NAME)), "should keep his name, has " + drops.get(0).get(DataComponents.CUSTOM_NAME));
		helper.succeed();
	}

	/** Something solid above makes him duck, and he stands again when it goes (D17). */
	@GameTest(template = COURSE)
	public static void ducksUnderARoof(GameTestHelper helper) {
		BlockPos pos = new BlockPos(5, 1, 5);
		placeBot(helper, pos, Direction.NORTH, null);
		helper.assertBlockProperty(pos, HelperBotBlock.DUCKED, false);
		helper.setBlock(pos.above(), Blocks.STONE);
		helper.assertBlockProperty(pos, HelperBotBlock.DUCKED, true);
		helper.setBlock(pos.above(), Blocks.TORCH);
		helper.assertBlockProperty(pos, HelperBotBlock.DUCKED, false);
		helper.setBlock(pos.above(), Blocks.SCAFFOLDING);
		helper.assertBlockProperty(pos, HelperBotBlock.DUCKED, true);
		// a closed top trapdoor sits above his caps
		helper.setBlock(pos.above(), Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP));
		helper.assertBlockProperty(pos, HelperBotBlock.DUCKED, false);
		// placed under a roof he's ducked from the start
		BlockPos under = new BlockPos(8, 1, 5);
		helper.setBlock(under.above(), Blocks.STONE);
		placeFromItem(helper, under, bot(null));
		helper.assertBlockProperty(under, HelperBotBlock.DUCKED, true);
		helper.succeed();
	}

	/** Train Controls placed beside him facing him turn him to face them; controls facing away don't (D21). */
	@GameTest(template = COURSE)
	public static void turnsToFaceTheControls(GameTestHelper helper) {
		BlockPos pos = new BlockPos(5, 1, 5);
		placeBot(helper, pos, Direction.NORTH, null);
		BlockState controls = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "controls")).defaultBlockState();
		helper.setBlock(pos.west(), controls.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		helper.assertBlockProperty(pos, HelperBotBlock.FACING, Direction.NORTH);
		helper.setBlock(pos.east(), controls.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		helper.assertBlockProperty(pos, HelperBotBlock.FACING, Direction.EAST);
		helper.succeed();
	}

	/** A Helper Bot beside a Stock Ticker, on its level, keeps shop for it; one a level up doesn't (D35). */
	@GameTest(template = COURSE)
	public static void keepsShopBesideATicker(GameTestHelper helper) {
		BlockPos ticker = new BlockPos(5, 1, 5);
		helper.setBlock(ticker, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "stock_ticker")).defaultBlockState());
		if (!(helper.getBlockEntity(ticker) instanceof StockTickerBlockEntity be))
			throw new GameTestAssertException("no Stock Ticker");
		placeBot(helper, ticker.above().east(), Direction.NORTH, null);
		helper.assertFalse(be.isKeeperPresent(), "a bot a level up shouldn't keep shop");
		placeBot(helper, ticker.east(), Direction.NORTH, null);
		helper.assertTrue(be.isKeeperPresent(), "a bot beside the ticker should keep shop");
		helper.succeed();
	}

	/**
	 * Create assembles the short test line's train with Helper Bots as its conductors: double-ended, a conductor each
	 * way, and of the three bots on board the two behind controls are in control and the middle one isn't (D7, D28).
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void conductsATrain(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 4));
		HelperBotTestSetup.trains(helper.getLevel(), origin, 22, 3, 19);
		helper.succeedWhen(() -> {
			Train train = trainNear(helper, origin);
			if (train == null)
				throw new GameTestAssertException("no train assembled yet");
			helper.assertTrue(train.doubleEnded, "the train should be double-ended");
			helper.assertTrue(train.hasForwardConductor() && train.hasBackwardConductor(), "a Helper Bot should conduct each way");
			int driving = 0, riding = 0;
			for (Carriage carriage : train.carriages) {
				CarriageContraptionEntity entity = carriage.anyAvailableEntity();
				if (entity == null)
					throw new GameTestAssertException("carriage not loaded yet");
				for (var actor : entity.getContraption().getActors())
					if (actor.left.state().getBlock() instanceof HelperBotBlock) {
						if (HelperBotMovement.inControl(actor.right))
							driving++;
						else
							riding++;
					}
			}
			helper.assertTrue(driving == 2 && riding == 1, "two bots should drive and one ride; " + driving + " drive, " + riding + " ride");
			// what his clients are told: a paused schedule but not a finished one (D29), a player at any carriage's controls
			train.runtime.setSchedule(new Schedule(), true);
			train.runtime.paused = true;
			train.runtime.completed = false;
			helper.assertTrue((HelperBotMovement.status(train) & HelperBotMovement.PAUSED) != 0, "a paused schedule should show");
			train.runtime.completed = true;
			helper.assertTrue((HelperBotMovement.status(train) & HelperBotMovement.PAUSED) == 0,
				"a finished schedule can't be resumed by a right-click: he shouldn't point at it");
			CarriageContraptionEntity last = train.carriages.get(train.carriages.size() - 1).anyAvailableEntity();
			last.setControllingPlayer(UUID.randomUUID());
			helper.assertTrue((HelperBotMovement.status(train) & HelperBotMovement.MANUAL) != 0,
				"a player at the far carriage's controls is driving the whole train");
			last.setControllingPlayer(null);
			// done: take the train off the network, so it doesn't outlive the test
			train.carriages.forEach(c -> c.forEachPresentEntity(Entity::discard));
			Create.RAILWAYS.removeTrain(train.id);
		});
	}

	/** Contraption Controls are Train Controls in Create's code, but nobody drives from them: he doesn't turn to them. */
	@GameTest(template = COURSE)
	public static void ignoresContraptionControls(GameTestHelper helper) {
		BlockPos pos = new BlockPos(5, 1, 5);
		placeBot(helper, pos, Direction.NORTH, null);
		BlockState controls = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "contraption_controls")).defaultBlockState();
		helper.setBlock(pos.east(), controls.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		helper.assertBlockProperty(pos, HelperBotBlock.FACING, Direction.NORTH);
		helper.assertTrue(HelperBots.controlsSide(helper.getLevel(), helper.absolutePos(pos)) == null, "Contraption Controls aren't his controls");
		helper.succeed();
	}

	/** Beside a Stock Ticker Create's wrench still turns him, and a dye in hand still opens the shop (as a Blaze Burner's does). */
	@GameTest(template = COURSE)
	public static void wrenchAndDyeBesideATicker(GameTestHelper helper) {
		BlockPos ticker = new BlockPos(5, 1, 5), pos = ticker.east();
		helper.setBlock(ticker, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "stock_ticker")).defaultBlockState());
		placeBot(helper, pos, Direction.NORTH, null);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos abs = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);

		ItemStack dye = new ItemStack(Items.RED_DYE);
		player.setItemInHand(InteractionHand.MAIN_HAND, dye);
		helper.assertTrue(helper.getLevel().getBlockState(abs).useItemOn(dye, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(),
			"a dye in hand shouldn't stop him opening the shop");

		ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
		player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
		helper.assertTrue(helper.getLevel().getBlockState(abs).useItemOn(wrench, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
			== ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "the wrench should get past the shop");
		wrench.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertTrue(helper.getLevel().getBlockState(abs).getValue(HelperBotBlock.FACING) != Direction.NORTH, "the wrench should turn him");
		helper.succeed();
	}

	/**
	 * With the shop setting off he's off duty: no shop, no nod at orders; on, both (D35). Its own batch, as it changes a
	 * setting the other tests read.
	 */
	@GameTest(template = COURSE, batch = "helper_bot_shop_setting")
	public static void theShopSettingTakesHimOffDuty(GameTestHelper helper) {
		BlockPos ticker = new BlockPos(5, 1, 5), pos = ticker.east();
		helper.setBlock(ticker, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "stock_ticker")).defaultBlockState());
		placeBot(helper, pos, Direction.NORTH, null);
		if (!(helper.getBlockEntity(ticker) instanceof StockTickerBlockEntity shop) || !(helper.getBlockEntity(pos) instanceof HelperBotBlockEntity bot))
			throw new GameTestAssertException("no ticker or bot");
		BlockPos at = helper.absolutePos(ticker);
		CMConfig.SERVER.helperBotShopKeeper.set(false);
		boolean keeps;
		try {
			keeps = shop.isKeeperPresent();
			HelperBots.orderPlacedAt(helper.getLevel(), at);
		} catch (RuntimeException e) {
			CMConfig.SERVER.helperBotShopKeeper.set(true);
			throw e;
		}
		helper.runAfterDelay(2, () -> {
			boolean nodded = bot.orderPlaced;
			CMConfig.SERVER.helperBotShopKeeper.set(true);
			helper.assertFalse(keeps, "off duty he doesn't keep shop");
			helper.assertFalse(nodded, "off duty he shouldn't nod at orders");
			helper.assertTrue(shop.isKeeperPresent(), "on duty he keeps shop");
			HelperBots.orderPlacedAt(helper.getLevel(), at);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(bot.orderPlaced, "on duty he nods at an order");
				helper.succeed();
			});
		});
	}

	/** Goggles show his anvil name, and which side of him the Stock Ticker he keeps shop for is (D25). */
	@GameTest(template = COURSE)
	public static void gogglesShowHisNameAndHisTicker(GameTestHelper helper) {
		BlockPos pos = new BlockPos(5, 1, 5);
		ItemStack stack = bot(null);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Bolt"));
		placeFromItem(helper, pos, stack);
		Direction facing = helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(HelperBotBlock.FACING);
		helper.setBlock(pos.relative(facing.getClockWise()), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "stock_ticker")).defaultBlockState());
		if (!(helper.getBlockEntity(pos) instanceof HelperBotBlockEntity be))
			throw new GameTestAssertException("no bot");
		// (the goggles' text itself is built on the client only; these are what it's built from)
		helper.assertTrue(Component.literal("Bolt").equals(be.name()), "goggles should call him by his name, has " + be.name());
		BlockPos at = helper.absolutePos(pos);
		helper.assertTrue(HelperBots.stockTicker(helper.getLevel(), at) != null, "he should find the ticker beside him");
		helper.assertValueEqual(HelperBotBlockEntity.side(facing, at, HelperBots.stockTicker(helper.getLevel(), at)), "right", "the ticker's side");
		helper.assertValueEqual(HelperBotBlockEntity.side(Direction.NORTH, BlockPos.ZERO, BlockPos.ZERO.north()), "front", "north of a bot facing north");
		helper.assertValueEqual(HelperBotBlockEntity.side(Direction.NORTH, BlockPos.ZERO, BlockPos.ZERO.south()), "behind", "south of a bot facing north");
		helper.assertValueEqual(HelperBotBlockEntity.side(Direction.NORTH, BlockPos.ZERO, BlockPos.ZERO.west()), "left", "west of a bot facing north");
		helper.succeed();
	}

	/** A contraption that comes apart over bedrock drops him with his colour, not plain (Create drops it without its data). */
	@GameTest(template = COURSE)
	public static void comingApartOverBedrockKeepsHisColour(GameTestHelper helper) {
		BlockPos target = new BlockPos(5, 1, 5);
		helper.setBlock(target, Blocks.BEDROCK);
		CompoundTag saved = new CompoundTag();
		saved.putString(HelperBotBlockEntity.DYE, DyeColor.LIME.getName());
		BearingContraption contraption = new BearingContraption();
		contraption.getBlocks().put(BlockPos.ZERO, new StructureBlockInfo(BlockPos.ZERO, ModBlocks.HELPER_BOT.get().defaultBlockState(), saved));
		BlockPos at = helper.absolutePos(target);
		contraption.addBlocksToWorld(helper.getLevel(), new StructureTransform(at, Direction.Axis.Y, Rotation.NONE, Mirror.NONE));
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
		helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModItems.HELPER_BOT.get()), "he should drop as an item, dropped " + drops);
		helper.assertTrue(drops.get(0).getItem().get(ModDataComponents.HELPER_BOT_DYE.get()) == DyeColor.LIME,
			"he should keep his lime, has " + drops.get(0).getItem().get(ModDataComponents.HELPER_BOT_DYE.get()));
		drops.forEach(Entity::discard);
		helper.succeed();
	}

	/** The train whose leading carriage is on this course's line. */
	private static Train trainNear(GameTestHelper helper, BlockPos origin) {
		for (Train train : Create.RAILWAYS.trains.values()) {
			CarriageContraptionEntity entity = train.carriages.isEmpty() ? null : train.carriages.get(0).anyAvailableEntity();
			if (entity != null && entity.level() == helper.getLevel() && entity.blockPosition().closerThan(origin, 32))
				return train;
		}
		return null;
	}
}
