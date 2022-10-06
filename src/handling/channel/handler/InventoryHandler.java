package handling.channel.handler;

import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.awt.Point;

import client.inventory.Equip;
import client.inventory.IEquip;
import client.inventory.IEquip.ScrollResult;
import client.inventory.IItem;
import client.ISkill;
import client.inventory.ItemFlag;
import client.inventory.MaplePet;
import client.inventory.MaplePet.PetFlag;
import client.inventory.MapleMount;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MapleInventory;
import client.MapleStat;
import client.PlayerStats;
import constants.GameConstants;
import client.SkillFactory;
import client.anticheat.CheatingOffense;
import client.inventory.ModifyInventory;
import constants.ItemConstants;
import constants.MapConstants;
import constants.PiPiConfig;
import constants.ServerConfig;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import server.AutobanManager;
import server.Randomizer;
import server.RandomRewards;
import server.MapleShopFactory;
import server.MapleItemInformationProvider;
import server.MapleInventoryManipulator;
import server.StructRewardItem;
import server.maps.SavedLocationType;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import scripting.NPCScriptManager;
import server.PredictCardFactory;
import server.StructPotentialItem;
import server.maps.MapleKite;
import server.maps.MapleMist;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import tools.FilePrinter;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.MTSCSPacket;
import tools.packet.PetPacket;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;
import tools.packet.PlayerShopPacket;

public class InventoryHandler {

    public static final void ItemMove(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory(true)) { //hack
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); //04
        final short src = slea.readShort();                                            //01 00
        final short dst = slea.readShort();                                            //00 00
        final short quantity = slea.readShort();                                       //53 01

        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

    public static final void ItemSort(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());

        final MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType); //Mode should correspond with MapleInventoryType
        boolean sorted = false;

        while (!sorted) {
            final byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.sendPacket(MaplePacketCreator.finishedSort(pInvType.getType()));
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after
        if (c == null || c.getPlayer() == null) {
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        if (invType != null) {
            MapleInventory Inv = c.getPlayer().getInventory(invType);

            final List<IItem> itemMap = new LinkedList<>();
            for (IItem item : Inv.list()) {
                itemMap.add(item.copy()); // clone all  items T___T.
            }
            for (IItem itemStats : itemMap) {
                MapleInventoryManipulator.removeById(c, invType, itemStats.getItemId(), itemStats.getQuantity(), true, false);
            }

            final List<IItem> sortedItems = sortItems(itemMap);
            for (IItem item : sortedItems) {
                MapleInventoryManipulator.addFromDrop(c, item, false);
            }
            c.sendPacket(MaplePacketCreator.finishedGather(mode));
            c.sendPacket(MaplePacketCreator.enableActions());
            itemMap.clear();
            sortedItems.clear();
        }
    }

    private static List<IItem> sortItems(final List<IItem> passedMap) {
        final List<Integer> itemIds = new ArrayList<>(); // empty list.
        for (IItem item : passedMap) {
            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
        }
        Collections.sort(itemIds); // sorts item ids

        final List<IItem> sortedList = new LinkedList<>(); // ordered list pl0x <3.

        for (Integer val : itemIds) {
            for (IItem item : passedMap) {
                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }
        return sortedList;
    }

    public static final boolean UseRewardItem(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final IItem toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        c.sendPacket(MaplePacketCreator.enableActions());
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);

                if (rewards != null && rewards.getLeft() > 0) {
                    boolean rewarded = false;
                    while (!rewarded) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    final IItem item = ii.getEquipById(reward.itemid);
                                    if (reward.period > 0) {
                                        item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                    }
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity);
                                }
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                c.sendPacket(MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                rewarded = true;
                                return rewarded;
                            }
                        }
                    }
                } else {
                    chr.dropMessage(6, "未知的錯誤.");
                }
            } else {
                chr.dropMessage(6, "你有一個欄位滿了 請空出來再打開");
            }
        }
        return false;
    }

    public static final void UseItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null/* || chr.hasDisease(MapleDisease.POTION)*/) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        final long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "由於冷卻時間尚未結束，所以無法使用此道具。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit()) || chr.getMapId() == 610030600 || chr.getMapId() == 105100300) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }

        } else {
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    public static final void UseReturnScroll(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                c.sendPacket(MaplePacketCreator.enableActions());
            }
        } else {
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    public static final void UseMagnify(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        final IItem magnify = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((byte) slea.readShort());
        final IItem toReveal = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if (magnify == null || toReveal == null) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return;
        }
        final Equip eqq = (Equip) toReveal;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        if (eqq.getState() == 1 && (magnify.getItemId() == 2460003 || (magnify.getItemId() == 2460002 && reqLevel <= 12) || (magnify.getItemId() == 2460001 && reqLevel <= 7) || (magnify.getItemId() == 2460000 && reqLevel <= 3))) {
            final List<List<StructPotentialItem>> pots = new LinkedList<>(ii.getAllPotentialInfo().values());
            int new_state = Math.abs(eqq.getPotential1());
            if (new_state > 7 || new_state < 5) { //luls
                new_state = 5;
            }
            final int lines = (eqq.getPotential2() != 0 ? 3 : 2);
            while (eqq.getState() != new_state) {
                //31001 = haste, 31002 = door, 31003 = se, 31004 = hb
                for (int i = 0; i < lines; i++) { //2 or 3 line
                    boolean rewarded = false;
                    while (!rewarded) {
                        StructPotentialItem pot = pots.get(Randomizer.nextInt(pots.size())).get(reqLevel);
                        if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) { //optionType
                            //have to research optionType before making this truely sea-like
                            switch (i) {
                                case 0:
                                    eqq.setPotential1(pot.potentialID);
                                    break;
                                case 1:
                                    eqq.setPotential2(pot.potentialID);
                                    break;
                                case 2:
                                    eqq.setPotential3(pot.potentialID);
                                    break;
                                default:
                                    break;
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            c.sendPacket(MaplePacketCreator.modifyInventory(true, new ModifyInventory(ModifyInventory.Types.MOVE, magnify)));
            //c.sendPacket(MaplePacketCreator.scrolledItem(magnify, toReveal, false, true));
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getPotentialReset(c.getPlayer().getId(), eqq.getPosition()));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1, false);
        } else {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
        }
    }

    public static final boolean UseUpgradeScroll(final byte slot, final byte dst, final byte ws, final MapleClient c, final MapleCharacter chr) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0);
    }

    public static final boolean UseUpgradeScroll(final byte slot, final byte dst, final byte ws, final MapleClient c, final MapleCharacter chr, final int vegas) {
        boolean whiteScroll = false; // 祝福捲軸
        boolean legendarySpirit = false; // 神匠之魂
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if ((ws & 2) == 2) {
            whiteScroll = true;
        }

        IEquip toScroll;
        if (dst < 0) {
            toScroll = (IEquip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else { // 神匠之魂
            legendarySpirit = true;
            toScroll = (IEquip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null) {
            return false;
        }
        final byte oldLevel = toScroll.getLevel();
        final byte oldEnhance = toScroll.getEnhance();
        final byte oldState = toScroll.getState();
        final byte oldFlag = toScroll.getFlag();
        final byte oldSlots = toScroll.getUpgradeSlots();

        IItem scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (scroll == null) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        }

        //卷軸使用判斷區
        if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() < 1) {
                c.sendPacket(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.sendPacket(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getState() >= 1 || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.sendPacket(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if (!GameConstants.canScroll(toScroll.getItemId()) && !GameConstants.isChaosScroll(toScroll.getItemId())) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((GameConstants.isCleanSlate(scroll.getItemId()) || GameConstants.isTablet(scroll.getItemId()) || GameConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { //not a durability item
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        } else if (!GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() >= 0) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        }

        IItem wscroll = null;

        // 驗證是否Hack
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.sendPacket(MaplePacketCreator.getInventoryFull());
            return false;
        }

        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }
        if (scroll.getItemId() == 2049115 && toScroll.getItemId() != 1003068) {
            //ravana
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId())) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 2: //armor
                    if (GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
            }
        } else if (!GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                return false;
            }
        }
        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            return false;
        }
        //卷軸數量異常
        if (scroll.getQuantity() <= 0) {
            return false;
        }

        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(1003)) <= 0 && chr.getSkillLevel(SkillFactory.getSkill(10001003)) <= 0 && chr.getSkillLevel(SkillFactory.getSkill(20001003)) <= 0 && chr.getSkillLevel(SkillFactory.getSkill(20011003)) <= 0 && chr.getSkillLevel(SkillFactory.getSkill(30001003)) <= 0) {
                AutobanManager.getInstance().addPoints(c, 50, 120000, "Using the Skill 'Legendary Spirit' without having it.");
                return false;
            }
        }

        // 卷軸 成功/ 失敗/ 破壞
        String end = "";
        final IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        ScrollResult scrollSuccess;
        if (scrolled == null) {
            scrollSuccess = IEquip.ScrollResult.CURSE;
            end = "破壞";
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = IEquip.ScrollResult.SUCCESS;
            end = "成功";
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots)) {// 白衣卷軸
            scrollSuccess = IEquip.ScrollResult.SUCCESS;
            end = "成功";
        } else {
            scrollSuccess = IEquip.ScrollResult.FAIL;
            end = "失敗";
        }
        try {
            String msg = "卷軸: " + MapleItemInformationProvider.getInstance().getName(scroll.getItemId()) + "(" + scroll.getItemId() + ") 裝備:" + MapleItemInformationProvider.getInstance().getName(toScroll.getItemId()) + "(" + toScroll.getItemId() + ")[力:" + toScroll.getStr() + "/敏:" + toScroll.getDex() + "/幸:" + toScroll.getLuk() + "/智:" + toScroll.getInt() + "/物攻:" + toScroll.getWatk() + "/魔功:" + toScroll.getMatk() + "/物防:" + toScroll.getWdef() + "/魔防:" + toScroll.getMdef() + "] 祝福卷: " + whiteScroll + " 神匠之魂: " + legendarySpirit + " 結果: " + end;
            if (ServerConfig.LOG_SCROLL) {
                FileoutputUtil.logToFile("logs/data/使用卷軸.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 玩家[" + c.getPlayer().getName() + "] " + msg);
            }
            if (c.getPlayer().getDebugMessage()) {
                c.getPlayer().dropMessage(-3, msg);
            }
        } catch (Exception ex) {
            FilePrinter.printError("InventoryHandler.txt", ex, "useUpgradeScroll has Exception");
        }

        // 移除卷軸
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, scroll.getPosition(), (short) 1, false, false);
        // 移除祝福卷軸
        if (whiteScroll && wscroll != null) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        }
        //道具更新類別
        final List<ModifyInventory> mods = new ArrayList<>();
        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            mods.add(new ModifyInventory(ModifyInventory.Types.REMOVE, toScroll));
            if (dst < 0) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            mods.add(new ModifyInventory(ModifyInventory.Types.REMOVE, scrolled));
            mods.add(new ModifyInventory(ModifyInventory.Types.ADD, scrolled));
        }
        //更新背包
        c.sendPacket(MaplePacketCreator.modifyInventory(true, mods));
        //卷軸特效
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit), vegas == 0);

        // 裝備中的道具更新
        if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE) && vegas == 0) {
            chr.equipChanged();
        }

        //c.sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, scroll)));
        return true;
    }

    public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null) {
            switch (itemid) {
                case 2270004: { //Purification Marble
                    final MapleMap map = chr.getMap();

                    if (mob.getHp() <= mob.getMobMaxHp() / 2) {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                        map.killMonster(mob, chr, true, false, (byte) 0);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, false, false);
                        MapleInventoryManipulator.addById(c, 4001169, (short) 1);
                    } else {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 0));
                        chr.dropMessage(5, "因怪物的生命值過高，所以無法捕捉！");
                    }
                    break;
                }
                case 2270002: { // Characteristic Stone
                    final MapleMap map = chr.getMap();

                    if (mob.getHp() <= mob.getMobMaxHp() / 2) {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                        map.killMonster(mob, chr, true, false, (byte) 0);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, false, false);
                    } else {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 0));
                        chr.dropMessage(5, "因怪物的生命值過高，所以無法捕捉！");
                    }
                    break;
                }
                case 2270000: { // Pheromone Perfume
                    if (mob.getId() != 9300101) {
                        break;
                    }
                    final MapleMap map = c.getPlayer().getMap();

                    map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                    map.killMonster(mob, chr, true, false, (byte) 0);
                    MapleInventoryManipulator.addById(c, 1902000, (short) 1, null);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, false, false);
                    break;
                }
                case 2270003: { // Cliff's Magic Cane
                    if (mob.getId() != 9500320) {
                        break;
                    }
                    final MapleMap map = c.getPlayer().getMap();

                    if (mob.getHp() <= mob.getMobMaxHp() / 2) {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                        map.killMonster(mob, chr, true, false, (byte) 0);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, false, false);
                    } else {
                        map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 0));
                        chr.dropMessage(5, "因怪物的生命值過高，所以無法捕捉！");
                    }
                    break;
                }
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void UseMountFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt(); //2260000 usually
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMount mount = chr.getMount();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null) {
            final int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(MaplePacketCreator.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void UseScriptedNPCItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        long expiration_days = 0;
        int mountid = 0;

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
            switch (toUse.getItemId()) {
                case 2430007: // Blank Compass
                {
                    final MapleInventory inventory = chr.getInventory(MapleInventoryType.SETUP);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);

                    if (inventory.countById(3994102) >= 20 // Compass Letter "North"
                            && inventory.countById(3994103) >= 20 // Compass Letter "South"
                            && inventory.countById(3994104) >= 20 // Compass Letter "East"
                            && inventory.countById(3994105) >= 20) { // Compass Letter "West"
                        MapleInventoryManipulator.addById(c, 2430008, (short) 1); // Gold Compass
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994102, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994103, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994104, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994105, 20, false, false);
                    } else {
                        MapleInventoryManipulator.addById(c, 2430007, (short) 1); // Blank Compass
                    }
                    NPCScriptManager.getInstance().start(c, 2084001);
                    break;
                }
                case 2430008: // Gold Compass
                {
                    chr.saveLocation(SavedLocationType.RICHIE);
                    MapleMap map;
                    boolean warped = false;

                    for (int i = 390001000; i <= 390001004; i++) {
                        map = c.getChannelServer().getMapFactory().getMap(i);

                        if (map.getCharactersSize() == 0) {
                            chr.changeMap(map, map.getPortal(0));
                            warped = true;
                            break;
                        }
                    }
                    if (warped) { // Removal of gold compass
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2430008, 1, false, false);
                    } else { // Or mabe some other message.
                        c.getPlayer().dropMessage(5, "全部地都在使用中，請稍後再嘗試一次。");
                    }
                    break;
                }
                case 2430112: //miracle cube
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 25) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049400, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2430112, 25, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049400, (short) 1);
                            } else {
                                c.getPlayer().dropMessage(5, "請空出一些欄位。");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 10) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049400, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2430112, 10, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049401, (short) 1);
                            } else {
                                c.getPlayer().dropMessage(5, "請空出一些欄位。.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 10 Fragments for a Potential Scroll, 25 for Advanced Potential Scroll.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "請空出一些欄位。");
                    }
                    break;
                case 2430036: //croco 1 day
                    mountid = 1027;
                    expiration_days = 1;
                    break;
                case 2430037: //black scooter 1 day
                    mountid = 1028;
                    expiration_days = 1;
                    break;
                case 2430038: //pink scooter 1 day
                    mountid = 1029;
                    expiration_days = 1;
                    break;
                case 2430039: //clouds 1 day
                    mountid = 1030;
                    expiration_days = 1;
                    break;
                case 2430040: //balrog 1 day
                    mountid = 1031;
                    expiration_days = 1;
                    break;
                case 2430053: //croco 30 day
                    mountid = 1027;
                    expiration_days = 1;
                    break;
                case 2430054: //black scooter 30 day
                    mountid = 1028;
                    expiration_days = 30;
                    break;
                case 2430055: //pink scooter 30 day
                    mountid = 1029;
                    expiration_days = 30;
                    break;
                case 2430056: //mist rog 30 day
                    mountid = 1035;
                    expiration_days = 30;
                    break;
                //race kart 30 day? unknown 2430057
                case 2430072: //ZD tiger 7 day
                    mountid = 1034;
                    expiration_days = 7;
                    break;
                case 2430073: //lion 15 day
                    mountid = 1036;
                    expiration_days = 15;
                    break;
                case 2430074: //unicorn 15 day
                    mountid = 1037;
                    expiration_days = 15;
                    break;
                case 2430075: //low rider 15 day
                    mountid = 1038;
                    expiration_days = 15;
                    break;
                case 2430076: //red truck 15 day
                    mountid = 1039;
                    expiration_days = 15;
                    break;
                case 2430077: //gargoyle 15 day
                    mountid = 1040;
                    expiration_days = 15;
                    break;
                case 2430080: //shinjo 20 day
                    mountid = 1042;
                    expiration_days = 20;
                    break;
                case 2430082: //orange mush 7 day
                    mountid = 1044;
                    expiration_days = 7;
                    break;
                case 2430091: //nightmare 10 day
                    mountid = 1049;
                    expiration_days = 10;
                    break;
                case 2430092: //yeti 10 day
                    mountid = 1050;
                    expiration_days = 10;
                    break;
                case 2430093: //ostrich 10 day
                    mountid = 1051;
                    expiration_days = 10;
                    break;
                case 2430101: //pink bear 10 day
                    mountid = 1052;
                    expiration_days = 10;
                    break;
                case 2430102: //transformation robo 10 day
                    mountid = 1053;
                    expiration_days = 10;
                    break;
                case 2430103: //chicken 30 day
                    mountid = 1054;
                    expiration_days = 30;
                    break;
                case 2430117: //lion 1 year
                    mountid = 1036;
                    expiration_days = 365;
                    break;
                case 2430118: //red truck 1 year
                    mountid = 1039;
                    expiration_days = 365;
                    break;
                case 2430119: //gargoyle 1 year
                    mountid = 1040;
                    expiration_days = 365;
                    break;
                case 2430120: //unicorn 1 year
                    mountid = 1037;
                    expiration_days = 365;
                    break;
                case 2430136: //owl 30 day
                    mountid = 1069;
                    expiration_days = 30;
                    break;
                case 2430137: //owl 1 year
                    mountid = 1069;
                    expiration_days = 365;
                    break;
                case 2430201: //giant bunny 60 day
                    mountid = 1096;
                    expiration_days = 60;
                    break;
                case 2430228: //tiny bunny 60 day
                    mountid = 1101;
                    expiration_days = 60;
                    break;
                case 2430229: //bunny rickshaw 60 day
                    mountid = 1102;
                    expiration_days = 60;
                    break;
            }
        }
        if (mountid > 0) {
            mountid += (GameConstants.isAran(c.getPlayer().getJob()) ? 20000000 : 0);
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "已經有了這個技能了。");
            } else if (expiration_days > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1, System.currentTimeMillis() + (long) (expiration_days * 24 * 60 * 60 * 1000));
                c.getPlayer().dropMessage(5, "已經學會了這個技能了。");
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void UseSummonBag(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);

            if (c.getPlayer().isGM() || !FieldLimitType.SummoningBag.check(chr.getMap().getFieldLimit())) {
                final List<Pair<Integer, Integer>> toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(itemId);

                if (toSpawn == null) {
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                MapleMonster ht;
                int type = 0;

                for (Pair<Integer, Integer> toSpawn1 : toSpawn) {
                    if (Randomizer.nextInt(99) <= toSpawn1.getRight()) {
                        ht = MapleLifeFactory.getMonster(toSpawn1.getLeft());
                        chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                    }
                }
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void UseTreasureChest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final short slot = slea.readShort();
        final int itemid = slea.readInt();

        final IItem toUse = chr.getInventory(MapleInventoryType.ETC).getItem((byte) slot);
        if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        int reward;
        int keyIDforRemoval;
        String box;
        String keyname = "";

        switch (toUse.getItemId()) {
            case 4280000: // 金寶箱
                reward = RandomRewards.getInstance().getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "金寶箱";
                break;
            case 4280001: // 銀寶箱
                reward = RandomRewards.getInstance().getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "銀寶箱";
                break;
            default: // 其他代碼 ?
                return;
        }

        // 得到的數量
        int amount = 1;
        keyname = MapleItemInformationProvider.getInstance().getName(keyIDforRemoval);
        switch (reward) {
            case 2000004:
                amount = 200; // 特殊藥水 200個
                break;
            case 2000005:
                amount = 100; // 超級藥水 100個
                break;
        }

        if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {

                final IItem item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);
                final byte rareness = GameConstants.gachaponRareItem(item.getItemId());

                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, true);
                MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
                c.sendPacket(MaplePacketCreator.getShowItemGain(reward, (short) amount, true));
                if (rareness > 0) {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("	恭喜 " + c.getPlayer().getName() + " : 從" + box + "抽到！", item, c.getChannel()));
                }
            } else {
                chr.dropMessage(5, "你有一個欄位滿了，請空出來再打開" + box + "！");
                c.sendPacket(MaplePacketCreator.enableActions());
            }
        } else {
            chr.dropMessage(5, "請確認是否有" + keyname);
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    @SuppressWarnings("empty-statement")
    public static final void UseCashItem(final LittleEndianAccessor slea, final MapleClient c) {
//        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();

        final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        if (World.isShutDown) {
            c.getPlayer().dropMessage(1, "目前無法使用商城道具。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        boolean used = false, cc = false;

        switch (itemId) {
            case 5042000: { //豫園高級瞬移之石
                MapleMap map;
                map = c.getChannelServer().getMapFactory().getMap(701000200);
                c.getPlayer().changeMap(map, map.getPortal(0));
                used = true;
                break;
            }
            case 5042001: { //不夜城高級瞬移之石
                MapleMap map;
                map = c.getChannelServer().getMapFactory().getMap(741000000);
                c.getPlayer().changeMap(map, map.getPortal(0));
                used = true;
                break;
            }
            case 5560000: //任意門一般券
            case 5561000: { //任意門高級券
                if (slea.readByte() == 0) {
                    final MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
                    if (c.getPlayer().inMapleLand()) {
                        c.getPlayer().dropMessage(5, "無法傳送到該地區。");
                        break;
                    } else if (target.getId() <= 2000000) {
                        c.getPlayer().dropMessage(5, "無法傳送到該地區。");
                        break;
                    }
                    if (itemId == 5560000 || itemId == 5561000 || (itemId != 5560000 || itemId != 5561000 && c.getPlayer().isRegRockMap(target.getId()))) {
                        if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit()) && c.getPlayer().getEventInstance() == null) { //Makes sure this map doesn't have a forced return map
                            c.getPlayer().handleMonsterEffectCard();
                            c.getPlayer().changeMap(target, target.getPortal(0));
                            used = true;
                        }
                    }
                }
                break;
            }

            case 2320000: // The Teleport Rock
            case 5041000: // 高級順移之石
            case 5040000: // 順移之石
            case 5040001: { //可樂翅膀
                used = UseTeleRock(slea, c, itemId);
                break;
            }
            case 5050000: { // 能力值
                List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>(2);
                final MapleStat apto = MapleStat.getByValue(slea.readInt());
                final MapleStat apfrom = MapleStat.getByValue(slea.readInt());
                int job = c.getPlayer().getJob();
                final PlayerStats playerst = c.getPlayer().getStat();

                if (apto == apfrom) {
                    break; // Hack
                } else if (apfrom == MapleStat.MAXMP && apto != MapleStat.MAXHP) {
                    c.getPlayer().dropMessage(1, "MP無法增加到HP以外的屬性唷");
                    break;
                }

                used = true;
                switch (apto) { // AP to
                    case STR: // str
                        if (playerst.getStr() >= 999) {
                            used = false;
                        }
                        break;
                    case DEX: // dex
                        if (playerst.getDex() >= 999) {
                            used = false;
                        }
                        break;
                    case INT: // int
                        if (playerst.getInt() >= 999) {
                            used = false;
                        }
                        break;
                    case LUK: // luk
                        if (playerst.getLuk() >= 999) {
                            used = false;
                        }
                        break;
                    case MAXHP: // hp
                        if (playerst.getMaxHp() >= 30000 || c.getPlayer().getHpMpApUsed() >= 500) {
                            used = false;
                        }
                        break;
                    case MAXMP: // mp
                        if (playerst.getMaxMp() >= 30000 || c.getPlayer().getHpMpApUsed() >= 500) {
                            used = false;
                        }
                        break;
                }
                switch (apfrom) { // AP to
                    case STR: // str
                        if (playerst.getStr() <= 4
                                || (c.getPlayer().getJob() % 1000 / 100 == 1 && playerst.getStr() <= 35)) {
                            used = false;
                        }
                        break;
                    case DEX: // dex
                        if (playerst.getDex() <= 4
                                || (c.getPlayer().getJob() % 1000 / 100 == 3 && playerst.getDex() <= 25)
                                || (c.getPlayer().getJob() % 1000 / 100 == 4 && playerst.getDex() <= 25)
                                || (c.getPlayer().getJob() % 1000 / 100 == 5 && playerst.getDex() <= 20)) {
                            used = false;
                        }
                        break;
                    case INT: // int
                        if (playerst.getInt() <= 4
                                || (c.getPlayer().getJob() % 1000 / 100 == 2 && playerst.getInt() <= 20)) {
                            used = false;
                        }
                        break;
                    case LUK: // luk
                        if (playerst.getLuk() <= 4) {
                            used = false;
                        }
                        break;
                    case MAXHP: // hp
                        if (c.getPlayer().getHpMpApUsed() <= 0 || c.getPlayer().getHpMpApUsed() >= 500) {
                            used = false;
                        }
                        break;
                    case MAXMP: // mp
                        if (c.getPlayer().getHpMpApUsed() <= 0 || c.getPlayer().getHpMpApUsed() >= 500) {
                            used = false;
                        }
                        break;
                }
                if (used) {
                    switch (apto) { // AP to
                        case STR: { // str
                            final int toSet = playerst.getStr() + 1;
                            playerst.setStr((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.STR, toSet));
                            break;
                        }
                        case DEX: { // dex
                            final int toSet = playerst.getDex() + 1;
                            playerst.setDex((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.DEX, toSet));
                            break;
                        }
                        case INT: { // int
                            final int toSet = playerst.getInt() + 1;
                            playerst.setInt((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.INT, toSet));
                            break;
                        }
                        case LUK: { // luk
                            final int toSet = playerst.getLuk() + 1;
                            playerst.setLuk((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.LUK, toSet));
                            break;
                        }
                        case MAXHP: // hp
                            short maxhp = playerst.getMaxHp();

                            if (job == 0) { // Beginner
                                maxhp += Randomizer.rand(8, 12);
                            } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212)) { // Warrior
                                ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(20, 25);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if ((job >= 200 && job <= 232)) { // Magician
                                maxhp += Randomizer.rand(10, 20);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312)) { // Bowman
                                maxhp += Randomizer.rand(16, 20);
                            } else if ((job >= 500 && job <= 522) || (job >= 3500 && job <= 3512)) { // Pirate
                                ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(18, 22);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1500 && job <= 1512) { // Pirate
                                ISkill improvingMaxHP = SkillFactory.getSkill(15100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(18, 22);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1100 && job <= 1112) { // Soul Master
                                ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(36, 42);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1200 && job <= 1212) { // Flame Wizard
                                maxhp += Randomizer.rand(15, 21);
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxhp += Randomizer.rand(40, 50);
                            } else { // GameMaster
                                maxhp += Randomizer.rand(50, 100);
                            }
                            maxhp = (short) Math.min(30000, Math.abs(maxhp));
                            c.getPlayer().setHpMpApUsed((short) (c.getPlayer().getHpMpApUsed() + 1));
                            playerst.setMaxHp(maxhp);
                            statupdate.add(new Pair<>(MapleStat.MAXHP, (int) maxhp));
                            break;

                        case MAXMP: // mp
                            short maxmp = playerst.getMaxMp();

                            if (job == 0) { // Beginner
                                maxmp += Randomizer.rand(6, 8);
                            } else if (job >= 100 && job <= 132) { // Warrior
                                maxmp += Randomizer.rand(5, 7);
                            } else if ((job >= 200 && job <= 232) || (job >= 3200 && job <= 3212)) { // Magician
                                ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp += Randomizer.rand(18, 20);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY() * 2;
                                }
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 522) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512)) { // Bowman
                                maxmp += Randomizer.rand(10, 12);
                            } else if (job >= 1100 && job <= 1112) { // Soul Master
                                maxmp += Randomizer.rand(6, 9);
                            } else if (job >= 1200 && job <= 1212) { // Flame Wizard
                                ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp += Randomizer.rand(18, 20);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY() * 2;
                                }
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxmp += Randomizer.rand(6, 9);
                            } else { // GameMaster
                                maxmp += Randomizer.rand(50, 100);
                            }
                            maxmp = (short) Math.min(30000, Math.abs(maxmp));
                            c.getPlayer().setHpMpApUsed((short) (c.getPlayer().getHpMpApUsed() + 1));
                            playerst.setMaxMp(maxmp);
                            statupdate.add(new Pair<>(MapleStat.MAXMP, (int) maxmp));
                            break;
                    }
                    switch (apfrom) { // AP from
                        case STR: { // str
                            final int toSet = playerst.getStr() - 1;
                            playerst.setStr((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.STR, toSet));
                            break;
                        }
                        case DEX: { // dex
                            final int toSet = playerst.getDex() - 1;
                            playerst.setDex((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.DEX, toSet));
                            break;
                        }
                        case INT: { // int
                            final int toSet = playerst.getInt() - 1;
                            playerst.setInt((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.INT, toSet));
                            break;
                        }
                        case LUK: { // luk
                            final int toSet = playerst.getLuk() - 1;
                            playerst.setLuk((short) toSet);
                            statupdate.add(new Pair<>(MapleStat.LUK, toSet));
                            break;
                        }
                        case MAXHP: // HP
                            short maxhp = playerst.getMaxHp();
                            if (job == 0) { // Beginner
                                maxhp -= Randomizer.rand(9, 13);
                            } else if (job >= 100 && job <= 132) { // Warrior
                                ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= Randomizer.rand(21, 26);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 200 && job <= 232) { // Magician
                                maxhp -= Randomizer.rand(18, 19);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512)) { // Bowman, Thief
                                maxhp -= Randomizer.rand(17, 21);
                            } else if (job >= 500 && job <= 522) { // Pirate
                                ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= Randomizer.rand(19, 23);
                                if (improvingMaxHPLevel > 0) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1500 && job <= 1512) { // Pirate
                                ISkill improvingMaxHP = SkillFactory.getSkill(15100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= Randomizer.rand(19, 23);
                                if (improvingMaxHPLevel > 0) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1100 && job <= 1112) { // Soul Master
                                ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= Randomizer.rand(38, 43);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1200 && job <= 1212) { // Flame Wizard
                                maxhp -= Randomizer.rand(16, 22);
                            } else if ((job >= 2000 && job <= 2112) || (job >= 3200 && job <= 3212)) { // Aran
                                maxhp -= Randomizer.rand(44, 54);
                            } else { // GameMaster
                                maxhp -= 20;
                            }
                            //c.getPlayer().setHpMpApUsed((short) (c.getPlayer().getHpMpApUsed() - 1));
                            if (playerst.getHp() > playerst.getMaxHp()) {
                                playerst.setHp(maxhp);
                            }
                            playerst.setMaxHp(maxhp);
                            statupdate.add(new Pair<>(MapleStat.MAXHP, (int) maxhp));
                            statupdate.add(new Pair<>(MapleStat.HP, (int) playerst.getHp()));

                            break;
                        case MAXMP: // MP
                            short maxmp = playerst.getMaxMp();
                            if (job == 0) { // Beginner
                                maxmp -= 8;
                            } else if (job >= 100 && job <= 132) { // Warrior
                                maxmp -= Randomizer.rand(6, 7);
                            } else if (job >= 200 && job <= 232) { // Magician
                                ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp -= Randomizer.rand(19, 21);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                }
                            } else if ((job >= 500 && job <= 522) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512)) { // Pirate, Bowman. Thief
                                maxmp -= Randomizer.rand(10, 13);
                            } else if (job >= 1100 && job <= 1112) { // Soul Master
                                maxmp -= Randomizer.rand(7, 10);
                            } else if (job >= 1200 && job <= 1212) { // Flame Wizard
                                ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp -= Randomizer.rand(18, 23);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                }
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxmp -= Randomizer.rand(8, 10);
                            } else { // GameMaster
                                maxmp -= 20;
                            }
                            //c.getPlayer().setHpMpApUsed((short) (c.getPlayer().getHpMpApUsed() - 1));
                            if (playerst.getMp() > playerst.getMaxMp()) {
                                playerst.setMp(maxmp);
                            }
                            playerst.setMaxMp(maxmp);
                            statupdate.add(new Pair<>(MapleStat.MAXMP, (int) maxmp));
                            statupdate.add(new Pair<>(MapleStat.MP, (int) playerst.getMp()));
                            break;
                    }
                    c.sendPacket(MaplePacketCreator.updatePlayerStats(statupdate, true, c.getPlayer().getJob()));
                }
                break;
            }
            case 5050001: // SP Reset (1st job)
            case 5050002: // SP Reset (2nd job)
            case 5050003: // SP Reset (3rd job)
            case 5050004: { // SP Reset (4th job)
                int skill1 = slea.readInt();
                int skill2 = slea.readInt();

                ISkill skillSPTo = SkillFactory.getSkill(skill1);
                ISkill skillSPFrom = SkillFactory.getSkill(skill2);

                if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                    break;
                }
                if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0) {
                    c.getPlayer().changeSkillLevel(skillSPFrom, (byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom));
                    c.getPlayer().changeSkillLevel(skillSPTo, (byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo));
                    used = true;
                }
                break;
            }
            case 5060005: { // DS高級孵化器
                c.getPlayer().dropMessage("此道具尚未開放，無法使用。");
                c.sendPacket(MaplePacketCreator.enableActions());
                break;
            }
            case 5060000: { // Item Tag
                final IItem item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readByte());

                if (item != null && item.getOwner().equals("")) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().contains(z) || item.getOwner().contains(z)) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setOwner(c.getPlayer().getName());
                        c.getPlayer().forceReAddItem_Flag(item, MapleInventoryType.EQUIPPED);
                        c.getPlayer().dropMessage(5, "刻名成功！");
                        used = true;
                    }
                }
                break;
            }
            case 5520001: //p.karma
            case 5520000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                if (item != null && !ItemFlag.KARMA_EQ.check(item.getFlag()) && !ItemFlag.KARMA_USE.check(item.getFlag())) {
                    if ((itemId == 5520000 && MapleItemInformationProvider.getInstance().isKarmaEnabled(item.getItemId())) || (itemId == 5520001 && MapleItemInformationProvider.getInstance().isPKarmaEnabled(item.getItemId()))) {
                        byte flag = item.getFlag();
                        if (type == MapleInventoryType.EQUIP) {
                            flag |= ItemFlag.KARMA_EQ.getValue();
                        } else {
                            flag |= ItemFlag.KARMA_USE.getValue();
                        }
                        item.setFlag(flag);

                        c.getPlayer().forceReAddItem_Flag(item, type);
                        used = true;
                    }
                }
                break;
            }
            case 5570000: { // Vicious Hammer
                slea.readInt(); // Inventory type, Hammered eq is always EQ.
                final Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                // another int here, D3 49 DC 00
                if (item != null) {
                    if (GameConstants.canHammer(item.getItemId()) && MapleItemInformationProvider.getInstance().getSlots(item.getItemId()) > 0 && item.getViciousHammer() <= 2) {
                        item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));

                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                        //c.sendPacket(MTSCSPacket.ViciousHammer(true, (byte) item.getViciousHammer()));
                        //c.sendPacket(MTSCSPacket.ViciousHammer(false, (byte) 0));
                        used = true;
                        cc = true;
                    } else {
                        c.getPlayer().dropMessage(5, "可能不能使用在這個道具上。");
                        cc = true;
                    }
                }
                break;
            }
            case 5610001:
            case 5610000: { // Vega 30
                slea.readInt(); // Inventory type, always eq
                final byte dst = (byte) slea.readInt();
                slea.readInt(); // Inventory type, always use
                final byte src = (byte) slea.readInt();
                used = UseUpgradeScroll(src, dst, (byte) 2, c, c.getPlayer(), itemId); //cannot use ws with vega but we dont care
                cc = used;
                break;
            }
            case 5060001: { // Sealing Lock
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    byte flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061000: { // Sealing Lock 7 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    byte flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);
                    item.setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061001: { // Sealing Lock 30 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    byte flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061002: { // Sealing Lock 90 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    byte flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5060003: {//peanut
                IItem item = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(4170023);
                if (item == null || item.getQuantity() <= 0) { // hacking{
                    return;
                }
                if (getIncubatedItems(c)) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                    used = true;
                }
            }
            break;
            case 5090100: // Wedding Invitation Card
            case 5090000: { // Note
                final String sendTo = slea.readMapleAsciiString();
                final String msg = slea.readMapleAsciiString();
                c.getPlayer().sendNote(sendTo, msg);
                used = true;
                break;
            }
            case 5100000: { // Congratulatory Song
                c.getPlayer().getMap().broadcastMessage(MTSCSPacket.playCashSong(5100000, c.getPlayer().getName()));
                used = true;
                break;
            }
            case 5201001:  //小鋼珠盒子(500)
            case 5201002:  //小鋼珠盒子(3000)
            case 5201003: {//小鋼珠盒子(100)
                int ss = 0;
                switch (itemId) {
                    case 5201001:
                        ss = 500;
                        break;
                    case 5201002:
                        ss = 3000;
                        break;
                    case 5201003:
                        ss = 100;
                        break;
                    default:
                        break;
                }
                c.getPlayer().gainBeans(ss);
                c.getPlayer().dropMessage(1, "小鋼珠已新增" + ss + "顆");
                used = true;
                break;
            }
            case 5330000:
                NPCScriptManager.getInstance().start(c, 9010009);
                break;
            /* 日拋隱形眼鏡 */
            case 5152100:
            case 5152101:
            case 5152102:
            case 5152103:
            case 5152104:
            case 5152105:
            case 5152106:
            case 5152107: {
                int color = (itemId - 5152100) * 100;
                if (c.getPlayer().getLevel() < 30) {
                    c.getPlayer().dropMessage(1, "必須等級30級以上才可以使用.");
                    break;
                }
                if (color >= 0 && c.getPlayer().changeFace((short) itemId, color)) {
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "使用日拋隱形眼鏡出現錯誤。");
                }
                break;
            }
            /* 寵物名字 */
            case 5170000: {
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;
                if (pet == null) {
                    break;
                }
                String nName = slea.readMapleAsciiString();
                pet.setName(nName);
                c.sendPacket(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
                c.sendPacket(MaplePacketCreator.enableActions());
                c.getPlayer().getMap().broadcastMessage(MTSCSPacket.changePetName(c.getPlayer(), nName, slo));
                used = true;
                break;
            }
            /* 寵物技能 */
            case 5190001:
            case 5190002:
            case 5190003:
            case 5190004:
            case 5190005:
            case 5190006:
            case 5190007:
            case 5190008:
            case 5190000:
            case 5191001:
            case 5191002:
            case 5191003:
            case 5191004:
            case 5191000: {
                final boolean isAdd = (itemId / 1000) % 10 == 0;
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                PetFlag zz = PetFlag.getByAddId(itemId);
                if (zz != null && !zz.check(pet.getFlags())) {
                    if (isAdd) {
                        pet.setFlags(pet.getFlags() | zz.getValue());
                    } else {
                        pet.setFlags(pet.getFlags() - zz.getValue());
                    }
                    c.sendPacket(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
                    c.sendPacket(MaplePacketCreator.enableActions());
                    c.sendPacket(MTSCSPacket.changePetFlag(uniqueid, true, zz.getValue()));
                    used = true;
                }
                break;
            }
            /* 智慧貓頭鷹 */
            case 5230000: {// owl of minerva
                final int itemSearch = slea.readInt();
                List<HiredMerchant> hms = new ArrayList<>();
                for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                    if (!cserv.searchMerchant(itemSearch).isEmpty()) {
                        hms.addAll(cserv.searchMerchant(itemSearch));
                    }
                }
                if (hms.size() > 0) {
                    c.sendPacket(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "找不到物品.");
                }
                break;
            }
            /* 寵物食品 */
            case 5240000:
            case 5240001:
            case 5240002:
            case 5240003:
            case 5240004:
            case 5240005:
            case 5240006:
            case 5240007:
            case 5240008:
            case 5240009:
            case 5240010:
            case 5240011:
            case 5240012:
            case 5240013:
            case 5240014:
            case 5240015:
            case 5240016:
            case 5240017:
            case 5240018:
            case 5240019:
            case 5240020:
            case 5240021:
            case 5240022:
            case 5240023:
            case 5240024:
            case 5240025:
            case 5240026:
            case 5240027:
            case 5240028: { // Pet food
                MaplePet pet = c.getPlayer().getPet(0);
                if (pet == null) {
                    break;
                }
                if (!pet.canConsume(itemId)) {
                    pet = c.getPlayer().getPet(1);
                    if (pet != null) {
                        if (!pet.canConsume(itemId)) {
                            pet = c.getPlayer().getPet(2);
                            if (pet != null) {
                                if (!pet.canConsume(itemId)) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                final byte petindex = c.getPlayer().getPetIndex(pet);
                pet.setFullness(100);
                if (pet.getCloseness() < 30000) {
                    if (pet.getCloseness() + 100 > 30000) {
                        pet.setCloseness(30000);
                    } else {
                        pet.setCloseness(pet.getCloseness() + 100);
                    }
                    if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);
                        c.sendPacket(PetPacket.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                        c.getPlayer().getMap().broadcastMessage(PetPacket.showPetLevelUp(c.getPlayer(), petindex));
                    }
                }
                c.sendPacket(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition())));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 1, petindex, true, true), true);
                used = true;
                break;
            }

            case 5281001:
            /*花香*/

            case 5280001: // Gas Skill
            /*毒屁*/
            case 5281000: {
                Rectangle bounds = new Rectangle((int) c.getPlayer().getPosition().getX(), (int) c.getPlayer().getPosition().getY(), 1, 1);
                MapleMist mist = new MapleMist(bounds, c.getPlayer());
                c.getPlayer().getMap().spawnMist(mist, 10000, true);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), "哦，不，我放屁!", false, 1));
                c.sendPacket(MaplePacketCreator.enableActions());
                used = true;
                break;
            }
            case 5320000: {
                String name = slea.readMapleAsciiString();
                String otherName = slea.readMapleAsciiString();
                long unk = slea.readInt();
                long unk_2 = slea.readInt();
                int cardId = slea.readByte();
                short unk_3 = slea.readShort();
                byte unk_4 = slea.readByte();
                // int comm = slea.readByte();
                int comm = Randomizer.rand(1, 6);
                PredictCardFactory pcf = PredictCardFactory.getInstance();
                PredictCardFactory.PredictCard Card = pcf.getPredictCard(cardId);
                // int commentId = Randomizer.nextInt(pcf.getCardCommentSize() + comm);
                PredictCardFactory.PredictCardComment Comment = pcf.getPredictCardComment(comm);
                //  PredictCardFactory.PredictCardComment Comment = pcf.getPredictCardComment(commentId);
                MapleCharacter victim = MapleCharacter.getOnlineCharacterByName(otherName);
                if ((Card == null) || (Comment == null)) {
                    break;
                } else if (!name.equals(c.getPlayer().getName())) {
                    c.getPlayer().dropMessage(1, "我的角色名字請填自己。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                } else if (victim == null) {
                    c.getPlayer().dropMessage(1, "對方必須上線。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                } else if (c.getPlayer().getGMLevel() < victim.getGMLevel()) {
                    c.getPlayer().dropMessage(1, "無法對此玩家使用。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }

                int love = Randomizer.rand(1, Comment.score) + 5;
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.startMapEffect("", 0, false)); // 清除當前地圖速配結果
                c.getSession().write(MTSCSPacket.showPredictCard(name, otherName, love, cardId, Comment.effectType));
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.startMapEffect(name + "和" + otherName + "的速配程度 " + Comment.worldmsg0, 5120026, true));
                used = true;
                break;
            }
            /* 黑板 */
            case 5370000:
            case 5370001: {
                if (c.getPlayer().getMapId() / 1000000 == 109) {
                    c.getPlayer().dropMessage(1, "請勿在活動地圖使用黑板");
                } else if (!c.getPlayer().getCanTalk()) {
                } else {
                    c.getPlayer().setChalkboard(slea.readMapleAsciiString());
                }
                break;
            }
            case 5390000: // Diablo Messenger
            case 5390001: // Cloud 9 Messenger
            case 5390002: // Loveholic Messenger
            case 5390003: // New Year Megassenger 1
            case 5390004: // New Year Megassenger 2
            case 5390005: // Cute Tiger Messenger
            case 5390006: { // Tiger Roar's Messenger
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "必須等級10級以上才可以使用.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canAvatarSmega2()) {
                    c.getPlayer().dropMessage(6, "很抱歉為了防止刷廣,所以你每10秒只能用一次.");
                    break;
                }
                if ((!c.getPlayer().getCanTalk() || c.getChannelServer().getMegaphoneMuteState()) && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "目前喇叭停止使用.");
                    break;
                }
                final String text = slea.readMapleAsciiString();
                final StringBuilder sb = new StringBuilder();
                addMedalString(c.getPlayer(), sb);
                sb.append(c.getPlayer().getName());
                sb.append(" : ");
                sb.append(text);
                if (text.length() > 55) {
                    break;
                }
                final boolean ear = slea.readByte() != 0;

                if (c.getPlayer().isPlayer() && !PiPiConfig.isCanTalkText(text)) {
                    c.getPlayer().dropMessage("說髒話是不禮貌的，請勿說髒話。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (c.getPlayer().isPlayer()) {
                    World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, text, ear));
                } else if (c.getPlayer().isGM()) {
                    World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, text, ear));
                }
                if (ServerConfig.LOG_MEGA) {
                    FileoutputUtil.logToFile("logs/聊天/廣播頻道.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 玩家『" + c.getPlayer().getName() + "』頻道『" + c.getChannel() + "』廣播道具『" + itemId + "』：" + text);
                }
                used = true;
                break;
            }

            case 5450000: { // Mu Mu the Travelling Merchant
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(5, "你不能在這張地圖裡使用，如果卡住請使用 @ea 來解卡。");
                        c.sendPacket(MaplePacketCreator.enableActions());
                        return;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "你還沒有10等以上");
                } else if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                    c.getPlayer().dropMessage(5, "你不能在這張地圖裡使用，如果卡住請使用 @ea 來解卡。");
                } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(5, "你不能在這張地圖裡使用，如果卡住請使用 @ea 來解卡。");
                } else {
                    MapleShopFactory.getInstance().getShop(61).sendShop(c);
                }
                //used = true;
                break;
            }
            default:
                switch (itemId / 10000) {
                    case 512:
                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        String msg = ii.getMsg(itemId);
                        final String ourMsg = slea.readMapleAsciiString();
                        if (!msg.contains("%s")) {
                            msg = ourMsg;
                        } else {
                            msg = msg.replaceFirst("%s", ourMsg);
                            if (!msg.contains("%s")) {
                                msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                            } else {
                                try {
                                    msg = msg.replaceFirst("%s", ourMsg);
                                } catch (Exception e) {
                                    msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                                }
                            }
                        }
                        c.getPlayer().getMap().startMapEffect(ourMsg, itemId);
                        final int buff = ii.getStateChangeItem(itemId);
                        if (buff != 0) {
                            for (MapleCharacter mChar : c.getPlayer().getMap().getCharactersThreadsafe()) {
                                ii.getItemEffect(buff).applyTo(mChar);
                            }
                        }
                        used = true;
                        break;
                    default:
                        switch (itemId / 10000) {
                            case 507: {
                                if (c.getPlayer().getLevel() < 10) {
                                    c.getPlayer().dropMessage(5, "道具10等以後才可以使用.");
                                    break;
                                } else if (!c.getPlayer().getCheatTracker().canAvatarSmega2()) {
                                    c.getPlayer().dropMessage(6, "很抱歉為了防止刷廣,所以你每10秒只能用一次.");
                                    break;
                                } else if (c.getChannelServer().getMegaphoneMuteState()) {
                                    c.getPlayer().dropMessage(5, "目前廣播系統尚未開啟.");
                                    break;
                                }
                                int msgType = itemId / 1000 % 10;

                                StringBuilder sb = new StringBuilder();
                                boolean ear = false;

                                if (msgType != 6 && msgType != 7 && msgType != 1) {
                                    String message = slea.readMapleAsciiString();
                                    if (message.length() > 65) {
                                        break;
                                    } else if (c.getPlayer().isPlayer() && !PiPiConfig.isCanTalkText(message)) {
                                        c.getPlayer().dropMessage("說髒話是不禮貌的，請勿說髒話。");
                                        c.sendPacket(MaplePacketCreator.enableActions());
                                        break;
                                    }
                                    addMedalString(c.getPlayer(), sb);
                                    sb.append(c.getPlayer().getName());
                                    sb.append(" : ");
                                    sb.append(message);
                                    ear = slea.readByte() != 0;
                                    if (ServerConfig.LOG_MEGA) {
                                        FileoutputUtil.logToFile("logs/聊天/廣播頻道.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 玩家『" + c.getPlayer().getName() + "』伺服器『" + c.getWorld() + "』頻道『" + c.getChannel() + "』廣播道具『" + itemId + "』：" + message);
                                    }
                                }

                                used = true;
                                switch (msgType) {
                                    case 1: // 紅色喇叭(改多世界)
                                        StringBuilder worldsb = new StringBuilder();
                                        String worldmsg = slea.readMapleAsciiString();
                                        if (worldmsg.length() > 65) {
                                            break;
                                        } else if (c.getPlayer().isPlayer() && !PiPiConfig.isCanTalkText(worldmsg)) {
                                            c.getPlayer().dropMessage("說髒話是不禮貌的，請勿說髒話。");
                                            c.sendPacket(MaplePacketCreator.enableActions());
                                            break;
                                        }
                                        addMedalString(c.getPlayer(), worldsb);
                                        worldsb.append(c.getPlayer().getName());
                                        worldsb.append(" [W ");
                                        worldsb.append(c.getPlayer().getWorld());
                                        worldsb.append("]");
                                        worldsb.append(" : ");
                                        worldsb.append(worldmsg);
                                        if (ServerConfig.LOG_MEGA) {
                                            FileoutputUtil.logToFile("logs/聊天/廣播頻道.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 玩家『" + c.getPlayer().getName() + "』伺服器『" + c.getWorld() + "』頻道『" + c.getChannel() + "』廣播道具『" + itemId + "』：" + worldmsg);
                                        }
                                        World.Broadcast.broadcastMessage(MaplePacketCreator.getMegaphone(worldsb.toString()));
                                        break;
                                    case 2: // 高質量喇叭
                                        World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getSuperMegaphone(sb.toString(), ear, c.getChannel()));
                                        break;
                                    case 3: // 心臟
                                        World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getHeartSmega(sb.toString(), ear, c.getChannel()));
                                        break;
                                    case 4: //骨頭
                                        World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getSkullSmega(sb.toString(), ear, c.getChannel()));
                                        break;
                                    case 6: //道具喇叭
                                        final String ItemMessage = slea.readMapleAsciiString();

                                        if (ItemMessage.length() > 65) {
                                            break;
                                        }
                                        final StringBuilder Itemsb = new StringBuilder();
                                        addMedalString(c.getPlayer(), Itemsb);
                                        Itemsb.append(c.getPlayer().getName());
                                        Itemsb.append(" : ");
                                        Itemsb.append(ItemMessage);

                                        boolean Itemear = slea.readByte() > 0;

                                        IItem item = null;
                                        if (slea.readByte() == 1) { //item
                                            byte invType = (byte) slea.readInt();
                                            byte pos = (byte) slea.readInt();
                                            item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                                        }
                                        if (ServerConfig.LOG_MEGA) {
                                            if (item == null) {
                                                FileoutputUtil.logToFile("logs/聊天/廣播頻道.txt", "\r\n " + FileoutputUtil.CurrentReadable_Time() + " 道具:" + itemId + " " + (c.getPlayer().getName()) + " : " + ItemMessage);
                                            } else {
                                                FileoutputUtil.logToFile("logs/聊天/廣播頻道.txt", "\r\n " + FileoutputUtil.CurrentReadable_Time() + " 道具:" + itemId + "(" + (item.getItemId()) + ") " + (c.getPlayer().getName()) + " : " + ItemMessage);
                                            }
                                        }

                                        if (used) {
                                            World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.itemMegaphone(Itemsb.toString(), Itemear, c.getChannel(), item));
                                        }
                                        break;
                                    case 7: //　三行喇叭
                                        final byte numLines = slea.readByte();
                                        if (numLines > 3) {
                                            return;
                                        }
                                        final StringBuilder sbb = new StringBuilder();
                                        addMedalString(c.getPlayer(), sbb);
                                        final List<String> allmessages = new LinkedList<>();
                                        String messages;
                                        for (int i = 0; i < numLines; i++) {
                                            messages = slea.readMapleAsciiString();
                                            if (messages.length() > 65) {
                                                break;
                                            } else if (c.getPlayer().isPlayer() && !PiPiConfig.isCanTalkText(messages)) {
                                                c.getPlayer().dropMessage("說髒話是不禮貌的，請勿說髒話。");
                                                c.sendPacket(MaplePacketCreator.enableActions());
                                                break;
                                            }
                                            allmessages.add(sbb + c.getPlayer().getName() + " : " + messages);
                                        }
                                        if (ServerConfig.LOG_MEGA) {
                                            FileoutputUtil.logToFile("日誌/紀錄/監聽/廣播.txt", "\r\n " + FileoutputUtil.CurrentReadable_Time() + " 道具:" + itemId + " " + (c.getPlayer().getName()) + " : " + allmessages);
                                        }
                                        World.Broadcast.broadcastSmega(c.getWorld(), MaplePacketCreator.getTripleSmega(allmessages, slea.readByte() > 0, c.getChannel()));
                                        break;
                                }
                                break;
                            }
                            case 508:
                                // 風箏 by Kodan
                                MapleKite Kite = new MapleKite(c.getPlayer(), c.getPlayer().getPosition(), c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId(), slea.readMapleAsciiString(), itemId);
                                c.getPlayer().getMap().spawnKite(Kite);
                                used = true;
                                break;
                            case 510:
                                c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                                used = true;
                                break;
                            case 520:
                                final int mesars = MapleItemInformationProvider.getInstance().getMeso(itemId);
                                if (mesars > 0 && c.getPlayer().getMeso() < (Integer.MAX_VALUE - mesars)) {
                                    used = true;
                                    if (Math.random() > 0.1) {
                                        final int gainmes = Randomizer.nextInt(mesars);
                                        c.getPlayer().gainMeso(gainmes, false);
                                        c.sendPacket(MTSCSPacket.sendMesobagSuccess(gainmes));
                                    } else {
                                        c.sendPacket(MTSCSPacket.sendMesobagFailed());
                                    }
                                }
                                /*                } else if (itemId / 10000 == 562) {
                UseSkillBook(slot, itemId, c, c.getPlayer()); //this should handle removing*/
                                break;
                            case 553:
                                UseRewardItem(slot, itemId, c, c.getPlayer());// this too
                                break;
                            default:
                                System.out.println("未處理的商城道具ID : " + itemId);
                                System.out.println(slea.toString(true));
                                break;
                        }
                        break;
                }
        }

        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
        }

        c.sendPacket(MaplePacketCreator.enableActions());
        if (cc) {
            if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "自動換頻道失敗");
                return;
            }
            c.getPlayer().dropMessage(5, "自動換頻道中, 請稍候...");
            c.getPlayer().changeChannel(c.getChannel() == ChannelServer.getChannelCount() ? 1 : (c.getChannel() + 1));
        }
    }

    public static final void PlayerPickup(final LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        if (World.isShutDown) {
            c.getPlayer().dropMessage(1, "目前無法撿物品。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.hasBlockedInventory(true)) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        chr.updateTick(slea.readInt());
        slea.skip(1); // [4] Seems to be tickcount, [1] always 0
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            //  c.getPlayer().dropMessage("撿物範圍: " + Distance);
            if (Distance > 2500) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
            } else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 90000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER, " 範圍: " + chr.getPosition().distanceSq(mapitem.getPosition()));
                chr.getClient().sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (ServerConfig.LOG_PICKITEM && ((mapitem.getItem() != null && mapitem.getItem().getEquipOnlyId() > 0) || (GameConstants.isAllScroll(mapitem.getItemId()) || GameConstants.isOpScroll(mapitem.getItemId())))) {
                FileoutputUtil.logToFile("logs/data/撿道具.txt", FileoutputUtil.NowTime() + "角色名字:" + c.getPlayer().getName() + " 從地板撿取了: " + MapleItemInformationProvider.getInstance().getName(mapitem.getItemId()) + "(" + mapitem.getItemId() + ") 數量:" + mapitem.getItem().getQuantity() + " 道具唯一ID: " + mapitem.getItem().getEquipOnlyId() + " \r\n");
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<>();

                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        m.gainMeso(mapitem.getMeso() / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0), true, true);
                    }
                } else {
                    chr.gainMeso(mapitem.getMeso(), true, true);
                }
                removeItem(chr, mapitem, ob);
            } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItem().getItemId())) {
                c.sendPacket(MaplePacketCreator.enableActions());
                c.getPlayer().dropMessage(5, "此物品無法被撿起.");
            } else if (useItem(c, mapitem.getItemId())) {
                removeItem(c.getPlayer(), mapitem, ob);
            } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if (mapitem.getItem().getQuantity() >= 50 && GameConstants.isUpgradeScroll(mapitem.getItem().getItemId())) {
                    c.setMonitored(true); //hack check
                }
                if (mapitem.isPlayerDrop() && ItemConstants.isEquip(mapitem.getItemId()) && mapitem.getItem().getEquipOnlyId() > 0) {
                    c.getPlayer().checkCopyItemsByID(mapitem.getOwner(), mapitem.getItemId());
                }

                if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, false)) {
                    removeItem(chr, mapitem, ob);
                }
            } else {
                c.sendPacket(MaplePacketCreator.getInventoryFull());
                c.sendPacket(MaplePacketCreator.getShowInventoryFull());
                c.sendPacket(MaplePacketCreator.enableActions());
            }
        } finally {
            lock.unlock();
        }
    }

    public static final void PetPickup(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (World.isShutDown) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        final byte petz = (byte) 0;
        final MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        chr.updateTick(slea.readInt());
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        // lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.sendPacket(MaplePacketCreator.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.isPlayerDrop() && mapitem.getDropType() == 2 && mapitem.getOwner() == chr.getId()) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (chr.getTrade() != null) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            final double pickUpDistanceCS = Client_Reportedpos.distanceSq(mapitem.getPosition());
            final double pickUpDistanceSS = pet.getPos().distanceSq(mapitem.getPosition());
            if (pickUpDistanceCS > 10000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, "距離" + pickUpDistanceCS + " 寵物座標 (" + Client_Reportedpos.getX() + "," + Client_Reportedpos.getY() + ")" + "物品座標 (" + mapitem.getPosition().getX() + "," + mapitem.getPosition().getY() + ")");
            }
            if (pickUpDistanceSS > 120000.0) {
                if (!chr.checkWarpingMap()) {
                    chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER, "距離" + pickUpDistanceSS + " 寵物座標 (" + pet.getPos().getX() + "," + pet.getPos().getY() + ")" + "物品座標 (" + mapitem.getPosition().getX() + "," + mapitem.getPosition().getY() + ")");
                }
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }

            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0), true);
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItemPet(chr, mapitem, petz);
            } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId()) || mapitem.getItemId() / 10000 == 291) {
                c.sendPacket(MaplePacketCreator.enableActions());
            } else if (useItem(c, mapitem.getItemId())) {
                removeItemPet(chr, mapitem, petz);
            } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                    c.setMonitored(true); //hack check
                }
                if (mapitem.isPlayerDrop() && ItemConstants.isEquip(mapitem.getItemId()) && mapitem.getItem().getEquipOnlyId() > 0) {
                    c.getPlayer().checkCopyItemsByID(mapitem.getOwner(), mapitem.getItemId());
                }
                removeItemPet(chr, mapitem, petz);
                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, true);

            }
        } finally {
            //lock.unlock();
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final byte consumeval = ii.isConsumeOnPickup(id);
            if (consumeval > 0) {
                if (consumeval == 2) {
                    if (c.getPlayer().getParty() != null) {
                        for (final MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                            if (chr != null && chr.isAlive()) {
                                ii.getItemEffect(id).applyTo(chr);
                            }
                        }
                    } else {
                        ii.getItemEffect(id).applyTo(c.getPlayer());
                    }
                } else {
                    ii.getItemEffect(id).applyTo(c.getPlayer());
                }
                c.sendPacket(MaplePacketCreator.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static final void removeItemPet(final MapleCharacter chr, final MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet), mapitem.getPosition());
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    public static void removeItem(final MapleCharacter chr, final MapleMapItem mapitem, final MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static void addMedalString(final MapleCharacter c, final StringBuilder sb) {
        final IItem medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
        if (medal != null) { // Medal
            sb.append("<");
            sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
            sb.append("> ");
        }
    }

    private static boolean getIncubatedItems(MapleClient c) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 2) {
            c.getPlayer().dropMessage(5, "Please make room in your inventory.");
            return false;
        }
        final int[] ids = {2430091, 2430092, 2430093, 2430101, 2430102, //mounts 
            2340000, //rares
            1152000, 1152001, 1152004, 1152005, 1152006, 1152007, 1152008, //toenail only comes when db is out.
            1000040, 1102246, 1082276, 1050169, 1051210, 1072447, 1442106, //blizzard
            3010019, //chairs
            1001060, 1002391, 1102004, 1050039, 1102040, 1102041, 1102042, 1102043, //equips
            1082145, 1082146, 1082147, 1082148, 1082149, 1082150, //wg
            2043704, 2040904, 2040409, 2040307, 2041030, 2040015, 2040109, 2041035, 2041036, 2040009, 2040511, 2040408, 2043804, 2044105, 2044903, 2044804, 2043009, 2043305, 2040610, 2040716, 2041037, 2043005, 2041032, 2040305, //scrolls
            2040211, 2040212, 1022097, //dragon glasses
            2049000, 2049001, 2049002, 2049003, //clean slate
            1012058, 1012059, 1012060, 1012061, //pinocchio nose msea only.
            1332100, 1382058, 1402073, 1432066, 1442090, 1452058, 1462076, 1472069, 1482051, 1492024, 1342009,//durability weapons level 105
            2049400, 2049401, 2049301};
        //out of 1000
        final int[] chances = {100, 100, 100, 100, 100,
            1,
            10, 10, 10, 10, 10, 10, 10,
            5, 5, 5, 5, 5, 5, 5,
            2,
            10, 10, 10, 10, 10, 10, 10, 10,
            5, 5, 5, 5, 5, 5,
            10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            5, 5, 10,
            10, 10, 10, 10,
            5, 5, 5, 5,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 2, 1, 2};
        int z = Randomizer.nextInt(ids.length);
        while (chances[z] < Randomizer.nextInt(1000)) {
            z = Randomizer.nextInt(ids.length);
        }
        int z_2 = Randomizer.nextInt(ids.length);
        while (z_2 == z || chances[z_2] < Randomizer.nextInt(1000)) {
            z_2 = Randomizer.nextInt(ids.length);
        }
        c.sendPacket(MaplePacketCreator.getPeanutResult(ids[z], (short) 1, ids[z_2], (short) 1));
        return MapleInventoryManipulator.addById(c, ids[z], (short) 1) && MapleInventoryManipulator.addById(c, ids[z_2], (short) 1);

    }

    public static final void OwlMinerva(final LittleEndianAccessor slea, final MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000) {
            final int itemSearch = slea.readInt();
            List<HiredMerchant> hms = new ArrayList<>();
            for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
                if (!cserv.searchMerchant(itemSearch).isEmpty()) {
                    hms.addAll(cserv.searchMerchant(itemSearch));
                }
            }
            if (hms.size() > 0) {
                c.sendPacket(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "找不到該物品");
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void Owl(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().haveItem(5230000, 1, false, false) || c.getPlayer().haveItem(2310000, 1, false, false)) {
            if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
                c.sendPacket(MaplePacketCreator.getOwlOpen());
            } else {
                c.getPlayer().dropMessage(5, "限自由市場内使用。");
                c.sendPacket(MaplePacketCreator.enableActions());
            }
        }
    }
    public static final int OWL_ID = 2; //don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static final void UseSkillBook(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(4);
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            return;
        }
        final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getSkillStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        final int SuccessRate = skilldata.get("success");
        final int ReqSkillLevel = skilldata.get("reqSkillLevel");
        final int MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        for (;;) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null) {
                break; // End of data
            }
            if (Math.floor(CurrentLoopedSkillId / 10000) == chr.getJob()) {
                final ISkill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
                if (chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                    canuse = true;
                    if (Randomizer.nextInt(99) <= SuccessRate && SuccessRate != 0) {
                        success = true;
                        final ISkill skill2 = CurrSkillData;
                        chr.changeSkillLevel(skill2, chr.getSkillLevel(skill2), (byte) MasterLevel);
                    } else {
                        success = false;
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    break;
                } else { // Failed to meet skill requirements
                    canuse = false;
                }
            }
        }
        //c.sendPacket(MaplePacketCreator.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(chr, skill, maxlevel, canuse, success));
    }

    public static final void OwlWarp(final LittleEndianAccessor slea, final MapleClient c) {
        c.sendPacket(MaplePacketCreator.enableActions());
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022 && c.getPlayer().getPlayerShop() == null) {
            final int id = slea.readInt();
            final int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                HiredMerchant merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        final MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                            if (ips instanceof HiredMerchant) {
                                merchant = (HiredMerchant) ips;
                            }
                        }
                        break;
                }
                if (merchant != null) {
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors((byte) 14, (byte) 1);
                        c.getPlayer().setPlayerShop(merchant);
                        c.sendPacket(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    } else if (!merchant.isOpen() || !merchant.isAvailable()) {
                        c.getPlayer().dropMessage(1, "這個商店在整理或者是沒在販賣東西。");
                    } else if (merchant.getFreeSlot() == -1) {
                        c.getPlayer().dropMessage(1, "商店人數已經滿了，請稍後再進入。");
                    } else if (merchant.isInBlackList(c.getPlayer().getName())) {
                        c.getPlayer().dropMessage(1, "被加入黑名單了，所以不能進入。");
                    } else {
                        c.getPlayer().setPlayerShop(merchant);
                        merchant.addVisitor(c.getPlayer());
                        c.sendPacket(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    }
                } else {
                    c.getPlayer().dropMessage(1, "商店正在整理中，");
                }
            }
        }
    }

    public static final boolean UseTeleRock(final LittleEndianAccessor slea, final MapleClient c, int itemId) {
        boolean used = false;
        if (itemId == 5041001 || itemId == 5040004) {
            slea.readByte(); //useless
        }
        if (slea.readByte() == 0) { // Rocktype
            int map = slea.readInt();
            final MapleMap target = c.getChannelServer().getMapFactory().getMap(map);
            if (!c.getPlayer().inMapleLand() && !MapConstants.isMapleLand(map)) {
                if ((itemId == 5041000 && c.getPlayer().isRockMap(target.getId())) || (itemId != 5041000 && c.getPlayer().isRegRockMap(target.getId())) || ((itemId == 5040004 || itemId == 5041001))) {
                    if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit()) && c.getPlayer().getEventInstance() == null) { //Makes sure this map doesn't have a forced return map
                        c.getPlayer().handleMonsterEffectCard();
                        c.getPlayer().changeMap(target, target.getPortal(0));
                        used = true;
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "無法在楓之島使用。");
            }
        } else {
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());

            if (victim != null && !victim.isGM() && c.getPlayer().getEventInstance() == null && victim.getEventInstance() == null) {
                if (!victim.inMapleLand()) {
                    if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit())) {
                        if (itemId == 5041000 || itemId == 5040004 || itemId == 5041001 || (victim.getMapId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) { // Viprock or same continent
                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
                            used = true;
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(5, "無法傳送到楓之島。");
                }
            }
        }
        return used && itemId != 5041001 && itemId != 5040004;
    }

    public static void owlse(final MapleClient c, int point, int itemid) {
        final int itemSearch = itemid;

        List<HiredMerchant> hms = new ArrayList<>();
        for (ChannelServer cserv : LoginServer.getWorldStatic(c.getPlayer().getMap().getWorld()).getChannels()) {
            if (!cserv.searchMerchant(itemSearch).isEmpty()) {
                hms.addAll(cserv.searchMerchant(itemSearch));
            }
        }
        if (hms.size() > 0) {
            if (c.getPlayer().haveItem(5230000, 1)) {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5230000, 1, true, false);
            } else {
                if (c.getPlayer().getCSPoints(point) >= 5) {
                    c.getPlayer().modifyCSPoints(point, -5, true);
                } else {
                    c.getPlayer().dropMessage(1, "點數不足，無法查詢！");
                    if (NPCScriptManager.getInstance().getCM(c) != null) {
                        NPCScriptManager.getInstance().dispose(c);
                        c.sendPacket(MaplePacketCreator.enableActions());
                    }
                }
            }
            if (NPCScriptManager.getInstance().getCM(c) != null) {
                NPCScriptManager.getInstance().dispose(c);
            }
            c.sendPacket(MaplePacketCreator.getOwlSearched(itemSearch, hms));
        } else {
            if (NPCScriptManager.getInstance().getCM(c) != null) {
                NPCScriptManager.getInstance().dispose(c);
                c.sendPacket(MaplePacketCreator.enableActions());
            }
            c.getPlayer().dropMessage(1, "找不到物品");

        }
    }
}
