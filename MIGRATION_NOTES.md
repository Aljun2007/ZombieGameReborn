# Forge 1.20.1 → NeoForge 1.21.1 移植规范(所有移植必须遵守)

工程根: `C:/Users/79662/.zcode/workspace/default/zombiegamereborn-neoforge-1.21.1`
原始 Forge 源码: `C:/Users/79662/zgr_work/raw_src/src/main/java/com/aljun/zombiegamereborn/`
NeoForge 21.1.255 反编译源码(API 字典): `<工程>/refsrc/neoforge/` — **拿不准的 API 一律先在这里 grep/读源码确认签名**,再写代码。
 NeoForge patches(含事件类): refsrc/neoforge/net/neoforged/neoforge/

## 目标
保持类名、方法名、配置键、网络包名、命令名、游戏逻辑**完全不变**。只改加载器 API 与被 1.21.1 原版改动影响的地方。不要"顺手重构"、不要改玩法数值、不要删除任何功能。

## 包级映射(机械替换)
| Forge 1.20.1 | NeoForge 1.21.1 |
|---|---|
| `net.minecraftforge.api.distmarker.{Dist,OnlyIn}` | `net.neoforged.api.distmarker.{Dist,OnlyIn}` |
| `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod`(主类改为 `@Mod(ZombieGameReborn.class)`,构造器注入 `IEventBus modEventBus, ModContainer modContainer`,见工程内已移植主类) |
| `net.minecraftforge.eventbus.api.{SubscribeEvent,IEventBus,Event}` | `net.neoforged.bus.api.{SubscribeEvent,IEventBus,Event}` + `net.neoforged.bus.api.ICancellableEvent`(可取消事件实现它) |
| `net.minecraftforge.common.MinecraftForge.EVENT_BUS` | `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` |
| `@Mod.EventBusSubscriber(modid=..., bus=...MOD)` | 模组总线事件类:不用注解,在主类 `modEventBus.register(Class)`;游戏事件类: `@EventBusSubscriber(modid=..., value=Dist.X)` (net.neoforged.fml.common.EventBusSubscriber) |
| `TickEvent.PlayerTickEvent`(Phase 判断) | `net.neoforged.neoforge.event.tick.PlayerTickEvent.Post`(Pre 对应 .Pre) |
| `TickEvent.LevelTickEvent` | `net.neoforged.neoforge.event.tick.LevelTickEvent.Pre/.Post` |
| `TickEvent.ServerTickEvent` / `ClientTickEvent` | `net.neoforged.neoforge.event.tick.server.ServerTickEvent.Pre/.Post` / `net.neoforged.neoforge.event.tick.ClientTickEvent.Pre/.Post` |
| `net.minecraftforge.network.simple.SimpleChannel` 等 | `CustomPacketPayload` + `StreamCodec` + `RegisterPayloadHandlersEvent`(见工程内已移植 ZGRNetwork.java 与 packet/ 现成写法) |
| `NetworkEvent.Context` | `net.neoforged.neoforge.network.handling.IPayloadContext`(enqueueWork→`context.enqueueWork` 仍在; `context.getSender()` 同名) |
| `NetworkDirection` | 用 `handler.playToClient/playToServer` 表达;`.handled(...)` 收尾 |
| `PacketDistributor.sendToServer/sendToAllPlayers/SendingTo...` | `net.neoforged.neoforge.network.PacketDistributor` 静态方法: `sendToServer(payload)` / `sendToAllPlayers(payload)` / `sendToPlayer(player, payload...)` / `sendToPlayersTrackingAndSelf(...)` 等,查 refsrc 确认 |
| Capability(getCapability/AttachCapabilitiesEvent/CapabilityManager/ICapabilityProvider) | **Data Attachment**: `DeferredRegister<AttachmentType<?>>`(registry=NeoForgeRegistries.ATTACHMENT_TYPES), `AttachmentType.serializable(clazz)` 或 `.builder(...).serialize(...)`;实体直接 `entity.getData(TYPE)`/`entity.setData(TYPE,v)`/`hasData`/`removeData`;序列化用 Codec 或 INBTSerializable(`net.neoforged.neoforge.common.util.INBTSerializable` 仍存在)。附件必须保留所有字段与语义(ZombieData/PlayerData 的全部数据) |
| `AttachCapabilitiesEvent<Entity>` | 不需要 — attachments 注册后自动可用,删除相关 provider 类并改调 `getData` |
| `ForgeRegistries.X` | `net.neoforged.neoforge.registries.NeoForgeRegistries.X` 或原版 `BuiltInRegistries.X` |
| `RegisterEvent` | `DeferredRegister`(在主类构造 `DeferredRegister.create(...).register(modEventBus)`),字段用 `DeferredHolder`/`DeferredItem` |
| `NewRegistryEvent`+`RegistryBuilder`(自定义注册表) | `net.neoforged.neoforge.registries.NewRegistryEvent` + `net.neoforged.neoforge.registries.RegistryBuilder` 仍存在,查 refsrc 改造 |
| `ForgeMod.X` | `net.neoforged.neoforge.common.NeoForgeMod.X` |
| `ForgeEventFactory.X` | `net.neoforged.neoforge.common.CommonHooks.X` 或 `net.neoforged.neoforge.event.EventHooks.X`(查 refsrc) |
| `ForgeHooks.X` | `CommonHooks.X` |
| `FMLJavaModLoadingContext.get().getModEventBus()` | 构造器注入的 `IEventBus modEventBus` |
| `FMLCommonSetupEvent` | `net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent`(同名,enqueueWork 仍在) |
| `ModList.get().isLoaded(...)` | `net.neoforged.fml.ModList`(同名) |
| `FMLEnvironment.dist` | `net.neoforged.fml.loading.FMLEnvironment.dist`(同名) |
| `ServerLifecycleHooks.getCurrentServer()` | `net.neoforged.neoforge.server.ServerLifecycleHooks`(同名) |
| `ConfigScreenHandler.ConfigScreenFactory` 注册扩展点 | `modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, ...)` — 类在 `net.neoforged.neoforge.client.ConfigScreenHandler`(主类已处理,不要动) |
| `net.minecraftforge.client.gui.widget.ForgeSlider` | `net.neoforged.neoforge.client.gui.widget.ExtendedSlider`(查 refsrc 构造签名) |
| `RegisterClientReloadListenersEvent` | `net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent`(同名) |
| INBTSerializable | `net.neoforged.neoforge.common.util.INBTSerializable`(同名) |

事件类大都是 `net.neoforged.neoforge.event.*` 同名包(如 LivingHurtEvent/LivingDeathEvent/LivingDropsEvent/EntityJoinLevelEvent/RegisterCommandsEvent/ServerAboutToStartEvent/PlayerEvent/PlayerInteractEvent/AttackEntityEvent/EntityItemPickupEvent/LivingKnockBackEvent/LivingConversionEvent/ExplosionEvent/BlockEvent)。可取消事件改为实现 `ICancellableEvent` 而非 setCanceled 继承(事件对象本身不用改,除非自定义事件)。

## 1.20.1 → 1.21.1 原版 API 变化(高频)
- 一切以 refsrc/neoforge 里 1.21.1 反编译源码为准;先 grep 原文件确认签名再改。
- `MobCategory` 仍存在;`SpawnPlacements` 注册方式变化(1.21.1: SpawnPlacementRegisterEvent 没了,改成 `SpawnPlacements.register(type, PlacementType, Heightmap.Types, predicate)`,查 refsrc)。
- `ItemStack.hurtAndBreak(int, LivingEntity, EquipmentSlot)` 等 1.21.1 签名变化:1.21.0 起 `hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot)` 改为 `hurtAndBreak(int, LivingEntity, EquipmentSlot)`? 实际 1.21.1 是 `hurtAndBreak(int, LivingEntity, EquipmentSlot)`,1.20.1 的三参版本里中间是 LivingEntity — 相同;但 `damage(int, RandomSource, ServerPlayer, Runnable)` 变 `damage(int, LivingEntity, EquipmentSlot)`/`hurtAndBreak(int, ServerPlayer, Runnable)` 等变体,查 refsrc。
- 装备:`Mob.setItemSlot`/`EquipmentSlot` 不变;`Mob.setPersistenceRequired` 不变;`LivingEntity.getMainHandItem` 不变。
- 属性:`Zombie.SPAWN_REINFORCEMENTS_CHANCE` 移到 `Attributes.SPAWN_REINFORCEMENTS_CHANCE`;`Attributes.MOVEMENT_SPEED` 等多数不变,个别改名(`KNOCKBACK_RESISTANCE` 不变)。查 refsrc net/minecraft/world/entity/ai/attributes/Attributes.java。
- `getEyeHeight`/`dimensions` 等 Pose 相关不变;`Entity.dimensions(EntityDimensions)` → `setDimensions`? 查 refsrc。
- `LivingEntity.onSoulSpeed`/`getArmorValue` 等不变。
- 掉落:`Drops`/`LootContext` 参数变化;`Block.getDrops` 变化点查 refsrc。
- `PathNavigation`/`WalkNodeEvaluator`/`PathType`(1.20.1 `BlockPathTypes` 已更名 `PathType`)。
- `Level#getEntitiesOfClass` 不变;`Level.addFreshEntity` 不变。
- 声音:`SoundEvents.X.getLocation()` → `SoundEvents.X.location()`(1.21.x 变化)。
- `Component.literal/translatable` 不变。GUI: `Screen`/`EditBox`/`Button`/`AbstractSliderButton` 基本不变;`AbstractSliderButton.applyValue` 仍在。
- `GuiGraphics.blit/drawString` 部分重载变化(1.21.1 需要 `RenderType` 参数的 blit 或有默认重载),查 refsrc。
- `RenderType`、`MultiBufferSource` 不变。
- `ResourceLocation.fromNamespaceAndPath(ns, path)` / `ResourceLocation.parse(String)` 替代 `new ResourceLocation(...)`。
- `Codec`/`NbtUtils` 大多不变;`CompoundTag.contains(String, int)` 不变。

## 网络(已定稿,勿改结构)
`network/ZGRNetwork.java` 与 `network/packet/` 下已由本次移植统一改成 NeoForge 风格:每个 packet 一个 record 实现 `CustomPacketPayload`,`TYPE` 常量、`STREAM_CODEC` 静态字段,ZGRNetwork 里 `payloadRegistrar.playToClient(...)`/`playToServer(...)` 注册。后续引用处按此写法。

## 附件(已定稿)
`common/entity/capability/` 与 `common/player/capability/` 的 Capability 一律转 Attachment:`ZGRAttachments` 注册类,使用处 `entity.getData(ZGRAttachments.ZOMBIE_DATA)` 等。原接口 IZombieData/IPlayerData 保留为接口,ZombieData/PlayerData 实现类保留字段与全部方法。

## Diplomat / 外部模组
- TACZ 在 1.21.1 无版本 → `com.tacz.guns.*` 一律编译期桩类(位于 `src/tacz_stub/java`,compileOnly sourceSet),运行期靠 ModList 守卫,不要破坏调用代码。
- pointblank/musketmod/enhancedcelestials 用 libs/ 下真实 1.21.1 NeoForge jar 编译,API 签名若与 1.20.1 不同,按 jar 里的真实签名改调用处(用 unzip -l / javap 查)。
- GuardVillagers 纯反射,不用动。

## 构建命令
```
cd <工程根>
JAVA_HOME="C:/Users/79662/jdk21/jdk-21.0.12.1+1" ./gradlew compileJava --console=plain -q
```
Gradle 9.5.0 已在本机缓存。**不要**跑 `gradlew build`/`test`,只跑 `compileJava`。

## 附件契约(必须遵守,跨包一致)
- 新增类 `com.aljun.zombiegamereborn.common.attachment.ZGRAttachments`(由负责 capability 包的代理创建):
  - `public static final AttachmentType<ZombieData> ZOMBIE_DATA`(ZombieData implements INBTSerializable<CompoundTag>,可序列化,随实体保存)
  - `public static final AttachmentType<PlayerData> PLAYER_DATA`(同上)
- `ZGRZombieAPI.getZombieData(Zombie)` 等实现改为 `zombie.getData(ZGRAttachments.ZOMBIE_DATA)`;hasData/removeData/setData 同理。原 IZombieData/IPlayerData 接口与实现类的全部字段、方法签名**保持不变**。

## 网络契约(主代理已完成,直接使用)
- 每个 packet 类实现 `CustomPacketPayload`,含 `public static final Type<X> TYPE` 与 `public static final StreamCodec<FriendlyByteBuf, X> STREAM_CODEC = StreamCodec.ofMember(X::encode, X::decode)`,`type()` 返回 TYPE。
- `ZGRNetwork.register(IEventBus modEventBus)` 在 `RegisterPayloadHandlersEvent` 里 `registrar("1")` 注册;playToServer/playToClient 已按原 SimpleChannel 方向 1:1 映射;客户端包 handler 内保持原来的 `Class.forName(...)` 反射调用,签名改为 `handleXxx(Packet packet, IPayloadContext context)`。
- `ZGRNetwork.sendToServer/sendToClient/sendToAllClients/sendToNearby/sendToTrackingEntity` 方法签名不变,内部用 `PacketDistributor` 静态方法。
- `Component.Serializer.toJson/fromJson` 在 1.21.1 已删除:用 `ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow()` / `.parse(JsonOps.INSTANCE, JsonParser.parseString(s)).getOrThrow()`。TimeBroadcastPacket 与 ClientPacketHandlers 两处。

## 各代理文件归属(只改自己名下的文件,公共契约见上)
