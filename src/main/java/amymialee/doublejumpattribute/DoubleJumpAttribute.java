package amymialee.doublejumpattribute;

import amymialee.doublejumpattribute.client.DoubleJumpWrapper;
import amymialee.doublejumpattribute.items.JumpBootsItem;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class DoubleJumpAttribute implements ModInitializer {
    public static final String MOD_ID = "doublejumpattribute";
    public static final Identifier PACKET_ADD_VELOCITY = id("addplayervelocity");
    public static final Identifier PACKET_SET_VELOCITY = id("setplayervelocity");
    public static final Identifier PACKET_DOUBLEJUMPED = id("doublejumped");
    public static final EntityAttribute JUMPS = Registry.register(Registries.ATTRIBUTE, id("double_jump_attribute"), new ClampedEntityAttribute("attribute." + MOD_ID + '.' + "jumps", 0, 0, 1024).setTracked(true));
    public static final Item JUMP_BOOTS = Registry.register(Registries.ITEM, id("jump_boots"), new JumpBootsItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE).fireproof()));
    public static final SoundEvent JUMP_SOUND_EVENT = Registry.register(Registries.SOUND_EVENT, id("entity.doublejumpattribute.jump"), SoundEvent.of(id("entity.doublejumpattribute.jump")));
    public static final Identifier DOUBLE_JUMP_STAT = Registry.register(Registries.CUSTOM_STAT, "double_jumped", id("double_jumped"));

    public static double getDoubleJumps(final LivingEntity entity) {
        return entity.getAttributeInstance(JUMPS) == null ? 0 : Objects.requireNonNull(entity.getAttributeInstance(JUMPS)).getValue();
    }

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_DOUBLEJUMPED, (server, playerEntity, playNetworkHandler, packetByteBuf, packetSender) -> {
            ((DoubleJumpWrapper) playerEntity).doubleJump();
            playerEntity.getWorld().playSoundFromEntity(null, playerEntity, JUMP_SOUND_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            for (int i = 0; i < 24; i++) {
                playerEntity.getWorld().addParticle(ParticleTypes.CLOUD,
                        playerEntity.getX() + playerEntity.getRandom().nextGaussian() * 0.12999999523162842D,
                        playerEntity.getBoundingBox().minY + 0.5D + playerEntity.getRandom().nextGaussian() * 0.12999999523162842D,
                        playerEntity.getZ() + playerEntity.getRandom().nextGaussian() * 0.12999999523162842D,
                        1, 0.0D, 0.0D);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, a) -> dispatcher.register(CommandManager.literal("setplayervelocity").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("target", EntityArgumentType.player())
                .then(CommandManager.argument("x", FloatArgumentType.floatArg()).then(CommandManager.argument("y", FloatArgumentType.floatArg()).then(CommandManager.argument("z", FloatArgumentType.floatArg()).executes(ctx -> {
                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                    float x = FloatArgumentType.getFloat(ctx, "x");
                    float y = FloatArgumentType.getFloat(ctx, "y");
                    float z = FloatArgumentType.getFloat(ctx, "z");
                    target.setVelocity(x, y, z);
                    PacketByteBuf buf2 = PacketByteBufs.create();
                    buf2.writeFloat(x);
                    buf2.writeFloat(y);
                    buf2.writeFloat(z);
                    ServerPlayNetworking.send(target, PACKET_SET_VELOCITY, buf2);
                    ctx.getSource().sendFeedback(new Supplier<Text>() {
                        @Override
                        public Text get() {
                            return Text.literal("Set velocity of " + target.getDisplayName().getString() + " to " + x + " " + y + " " + z + ".").formatted(Formatting.GRAY);
                        }
                    }, true);
                    return 0;
                })))))));

        CommandRegistrationCallback.EVENT.register((_dispatcher, _dedicated, _a) -> _dispatcher.register(CommandManager.literal("addplayervelocity").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("target", EntityArgumentType.player())
                .then(CommandManager.argument("x", FloatArgumentType.floatArg()).then(CommandManager.argument("y", FloatArgumentType.floatArg()).then(CommandManager.argument("z", FloatArgumentType.floatArg()).executes(_ctx -> {
                    ServerPlayerEntity _target = EntityArgumentType.getPlayer(_ctx, "target");
                    float _x = FloatArgumentType.getFloat(_ctx, "x");
                    float _y = FloatArgumentType.getFloat(_ctx, "y");
                    float _z = FloatArgumentType.getFloat(_ctx, "z");
                    _target.addVelocity(_x, _y, _z);
                    PacketByteBuf _buf2 = PacketByteBufs.create();
                    _buf2.writeFloat(_x);
                    _buf2.writeFloat(_y);
                    _buf2.writeFloat(_z);
                    ServerPlayNetworking.send(_target, PACKET_ADD_VELOCITY, _buf2);
                    _ctx.getSource().sendFeedback(new Supplier<Text>() {
                        @Override
                        public Text get() {
                            return Text.literal("Added " + _x + " " + _y + " " + _z + " velocity to " + _target.getDisplayName().getString() + ".").formatted(Formatting.GRAY);
                        }
                    }, true);
                    return 0;
                })))))));
    }
    
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}