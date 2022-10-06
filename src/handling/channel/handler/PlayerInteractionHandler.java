/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.util.Arrays;

import client.inventory.IItem;
import client.inventory.ItemFlag;
import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import constants.ServerConfig;
import handling.channel.ChannelServer;
import handling.world.World;
import java.util.List;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.maps.FieldLimitType;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import server.shops.MaplePlayerShop;
import server.shops.MaplePlayerShopItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.shops.MapleMiniGame;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;
import scripting.NPCScriptManager;
import server.maps.MapleMap;
import tools.FileoutputUtil;
import tools.data.LittleEndianAccessor;

public class PlayerInteractionHandler {

    public static final byte CREATE = 0x00,
            INVITE_TRADE = 0x02,
            DENY_TRADE = 0x03,
            VISIT = 0x04,
            CHAT = 0x06,
            EXIT = 0x0A,
            OPEN = 0x0B,
            SET_ITEMS = 0x0D,
            SET_MESO = 0x0E,
            CONFIRM_TRADE = 0x0F,
            TRADE_SOMETHING = 0x11,
            PLAYER_SHOP_ADD_ITEM = 0x12,
            BUY_ITEM_PLAYER_SHOP = 0x13,
            KICK_Player = 0x18,
            MERCHANT_EXIT = 0x1A, //is this also updated
            ADD_ITEM = 0x1D,
            BUY_ITEM_STORE = 0x1E,
            BUY_ITEM_HIREDMERCHANT = 0x20,
            REMOVE_ITEM_PS = 0x17,
            REMOVE_ITEM = 0x22,
            MAINTANCE_OFF = 0x23, //This is mispelled...
            MAINTANCE_ORGANISE = 0x24,
            CLOSE_MERCHANT = 0x25,
            ADMIN_STORE_NAMECHANGE = 0x29,
//            VIEW_MERCHANT_VISITOR = 0x7FFE,
//            VIEW_MERCHANT_BLACKLIST = 0x7FFE,
//            MERCHANT_BLACKLIST_ADD = 0x7FFE,
//            MERCHANT_BLACKLIST_REMOVE = 0x7FFE,
            REQUEST_TIE = 0x2A,
            ANSWER_TIE = 0x2B,
            GIVE_UP = 0x2C,
            REQUEST_REDO = 0x2E,
            ANSWER_REDO = 0x2F,
            EXIT_AFTER_GAME = 0x30,
            CANCEL_EXIT = 0x31,
            READY = 0x32,
            UN_READY = 0x33,
            EXPEL = 0x34,
            START = 0x35,
            SKIP = 0x37,
            MOVE_OMOK = 0x38,
            SELECT_CARD = 0x3C;

    public static final void HiredMerchantRemoteControl(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte action = slea.readByte();
        if (chr == null || action != 3 || !chr.haveItem(5470000)) {
            return;
        }

        // 使用限制
        if (chr.getMapId() != 910000000
                || chr.getParty() != null) {
            return;
        }

        if (World.isShutDown) {
            chr.dropMessage(1, "伺服器即將關閉或者正在準備，所以無法正常使用精靈商人、個人商店、小遊戲。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        ChannelServer ch = ChannelServer.getInstance(chr.getWorld(), chr.getMap().getChannel());
        if (ch == null) {
            return;
        }
        for (int i = 1; i < 23; i++) {
            MapleMap map = ch.getMapFactory().getMap(910000000 + i);
            if (map != null) {
                List<MapleMapObject> obs = map.getMapObjects(Arrays.asList(MapleMapObjectType.HIRED_MERCHANT));
                for (MapleMapObject ob : obs) {
                    if (ob instanceof HiredMerchant) {
                        HiredMerchant merchant = (HiredMerchant) ob;
                        if (merchant.isOwner(chr)) {
                            merchant.setOpen(false);
                            merchant.removeAllVisitors((byte) 14, (byte) 1);
                            chr.setPlayerShop((IMaplePlayerShop) ob);
                            c.sendPacket(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                            return;
                        }
                    }
                }
            }
        }

        chr.dropMessage(1, "未找到精靈商人，請確認後再使用。");
    }

    public static final void PlayerInteraction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        //System.out.println(slea.toString());
        if (chr == null) {
            return;
        }

        final byte action = slea.readByte();
        switch (action) { // Mode
            case KICK_Player: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null) {
                    ips.removeAllVisitors(5, 1); //no msg
                }
                break;
            }
            case CREATE: {
                final byte createType = slea.readByte();
                if (createType == 3) { // trade
                    MapleTrade.startTrade(chr);
                } else if (createType == 1 || createType == 2 || createType == 4 || createType == 5) { // shop
                    if (!chr.getMap().getMapObjectsInRange(chr.getPosition(), 20000, Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT)).isEmpty()) {
                        chr.dropMessage(1, "此處無法建立商店");
                        c.sendPacket(MaplePacketCreator.enableActions());
                        return;
                    } else if (createType == 1 || createType == 2) {
                        if (FieldLimitType.Minigames.check(chr.getMap().getFieldLimit())) {
                            chr.dropMessage(1, "此處無法開設小遊戲");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                    }
                    final String desc = slea.readMapleAsciiString();
                    String pass = "";
                    if (slea.readByte() > 0 && (createType == 1 || createType == 2)) {
                        pass = slea.readMapleAsciiString();
                    }
                    if (createType == 1 || createType == 2) {
                        if (World.isShutDown) {
                            chr.dropMessage(1, "伺服器即將關閉或者正在準備，所以無法正常使用精靈商人、個人商店、小遊戲。");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        final int piece = slea.readByte();
                        final int itemId = createType == 1 ? (4080000 + piece) : 4080100;
                        if (!chr.haveItem(itemId) || (c.getPlayer().getMapId() >= 910000001 && c.getPlayer().getMapId() <= 910000022)) {
                            return;
                        }
                        MapleMiniGame game = new MapleMiniGame(chr, itemId, desc, pass, createType); //itemid
                        game.setPieceType(piece);
                        chr.setPlayerShop(game);
                        game.setAvailable(true);
                        game.setOpen(true);
                        game.send(c);
                        chr.getMap().addMapObject(game);
                        game.update();
                    } else {
                        IItem shop = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) slea.readShort());
                        if (shop == null || shop.getQuantity() <= 0 || shop.getItemId() != slea.readInt() || c.getPlayer().getMapId() < 910000001 || c.getPlayer().getMapId() > 910000022) {
                            return;
                        }
                        if (World.isShutDown) {
                            chr.dropMessage(1, "伺服器即將關閉或者正在準備，所以無法正常使用精靈商人、個人商店、小遊戲。");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        } else if (!chr.getCanTalk()) {
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        for (int i = 5; i >= 1; i--) {
                            chr.dropMessage(1, "使用合約書請詳細閱讀\r\n由於精靈商人、個人商店BUG太多 請玩家使用時請抱著此種心態\r\n1.東西不想要了 2.沒錢拿也沒差\r\n因為很重要所以請閱讀" + i + "/5次");
                        }
                        if (createType == 4) {
                            MaplePlayerShop mps = new MaplePlayerShop(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(mps);
                            chr.getMap().addMapObject(mps);
                            c.sendPacket(PlayerShopPacket.getPlayerStore(chr, true));
                        } else {
                            final HiredMerchant merch = new HiredMerchant(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(merch);
                            chr.getMap().addMapObject(merch);
                            c.sendPacket(PlayerShopPacket.getHiredMerch(chr, merch, true));
                        }
                    }
                }
                break;
            }
            case INVITE_TRADE: {
                MapleTrade.inviteTrade(chr, chr.getMap().getCharacterById(slea.readInt()));
                break;
            }
            case DENY_TRADE: {
                MapleTrade.declineTrade(chr);
                break;
            }
            case VISIT: {
                if (World.isShutDown) {
                    chr.dropMessage(1, "伺服器即將關閉或者正在準備，所以無法正常使用精靈商人、個人商店、小遊戲。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (chr.getTrade() != null && chr.getTrade().getPartner() != null) {
                    MapleTrade.visitTrade(chr, chr.getTrade().getPartner().getChr());
                } else if (chr.getMap() != null) {
                    final int obid = slea.readInt();
                    MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                    if (ob == null) {
                        ob = chr.getMap().getMapObject(obid, MapleMapObjectType.SHOP);
                    }
                    if (ob instanceof IMaplePlayerShop && chr.getPlayerShop() == null) {
                        final IMaplePlayerShop ips = (IMaplePlayerShop) ob;

                        if (ob instanceof HiredMerchant) {
                            final HiredMerchant merchant = (HiredMerchant) ips;
                            if (merchant.isOwner(chr)) {
                                merchant.setOpen(false);
                                merchant.removeAllVisitors((byte) 14, (byte) 1);
                                chr.setPlayerShop(ips);
                                c.sendPacket(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                                merchant.sendMsg(c);
                            } else if (!merchant.isOpen() || !merchant.isAvailable()) {
                                chr.dropMessage(1, "這個商店在整理或者是沒在販賣東西。");
                            } else if (ips.getFreeSlot() == -1) {
                                chr.dropMessage(1, "商店人數已經滿了，請稍後再進入。");
                            } else if (merchant.isInBlackList(chr.getName())) {
                                chr.dropMessage(1, "被加入黑名單了，所以不能進入。");
                            } else {
                                chr.setPlayerShop(ips);
                                merchant.addVisitor(chr);
                                c.sendPacket(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                                merchant.sendMsg(c);
                            }
                        } else if (ips instanceof MaplePlayerShop && ((MaplePlayerShop) ips).isBanned(chr.getName())) {
                            chr.dropMessage(1, "被加入黑名單了，所以不能進入。");
                        } else if (ips.getFreeSlot() < 0 || ips.getVisitorSlot(chr) > -1 || !ips.isOpen() || !ips.isAvailable()) {
                            c.sendPacket(PlayerShopPacket.getMiniGameFull());
                        } else {
                            if (slea.available() > 0 && slea.readByte() > 0) { //a password has been entered
                                String pass = slea.readMapleAsciiString();
                                if (!pass.equals(ips.getPassword())) {
                                    c.getPlayer().dropMessage(1, "你輸入的密碼錯誤,請重新再試一次.");
                                    return;
                                }
                            } else if (ips.getPassword().length() > 0) {
                                c.getPlayer().dropMessage(1, "你輸入的密碼錯誤,請重新再試一次.");
                                return;
                            }
                            chr.setPlayerShop(ips);
                            ips.addVisitor(chr);
                            if (ips instanceof MapleMiniGame) {
                                ((MapleMiniGame) ips).send(c);
                            } else {
                                c.sendPacket(PlayerShopPacket.getPlayerStore(chr, false));
                            }
                        }
                    }
                }
                break;
            }
            case CHAT: {
//                slea.readInt();
                if (chr.getTrade() != null) {
                    chr.getTrade().chat(slea.readMapleAsciiString());
                } else if (chr.getPlayerShop() != null) {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    final String Msg = slea.readMapleAsciiString();
                    ips.broadcastToVisitors(PlayerShopPacket.shopChat(chr.getName() + " : " + Msg, ips.getVisitorSlot(chr)));
                    if (ips.getShopType() == 1) { // Hired Merchant
                        ((HiredMerchant) ips).addMsg(chr.getName() + " : " + Msg, ips.getVisitorSlot(chr));
                    }
                }
                break;
            }
            case EXIT: {
                if (chr.getTrade() != null) {
                    final MapleTrade t = chr.getTrade();
                    if (t != null) {
                        if (!t.isLocked()) {
                            MapleTrade.cancelTrade(chr.getTrade(), chr.getClient());
                        }
                    }
                } else {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    if (ips == null) {
                        return;
                    }
                    if (!ips.isAvailable() || (ips.isOwner(chr) && ips.getShopType() != 1)) {
                        ips.closeShop(false, ips.isAvailable());
                    } else {
                        ips.removeVisitor(chr);
                    }
                    chr.setPlayerShop(null);
                    NPCScriptManager.getInstance().dispose(c);
                    c.sendPacket(MaplePacketCreator.enableActions());
                }
                break;
            }
            case OPEN: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null) {
                    if (shop.isOwner(chr)) {
                        if (shop.getShopType() < 3) {
                            if (chr.getMap().allowPersonalShop()) {
                                if (World.isShutDown) {
                                    chr.dropMessage(1, "伺服器即將關閉或者正在準備，所以無法正常使用精靈商人、個人商店、小遊戲。");
                                    c.sendPacket(MaplePacketCreator.enableActions());
                                    shop.closeShop(shop.getShopType() == 1, false);
                                    return;
                                }
                                if (shop.getShopType() == 1) {
                                    final HiredMerchant merchant = (HiredMerchant) shop;
                                    merchant.setStoreId(c.getChannelServer().addMerchant(merchant));
                                    merchant.setOpen(true);
                                    merchant.setAvailable(true);
                                    chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
                                    chr.setPlayerShop(null);

                                } else if (shop.getShopType() == 2) {
                                    shop.setOpen(true);
                                    shop.setAvailable(true);
                                    shop.update();
                                    final MaplePlayerShop playershop = (MaplePlayerShop) shop;
                                    c.getChannelServer().addPlayerShop(playershop);
                                }
                            } else {
                                c.getSession().close();
                                if (ServerConfig.LOG_DC) {
                                    FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                                }
                            }
                        }
                    }
                }
                break;
            }

            case SET_ITEMS: {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
                final IItem item = chr.getInventory(ivType).getItem((byte) slea.readShort());
                final short quantity = slea.readShort();
                final byte targetSlot = slea.readByte();

                if (chr.getTrade() != null && item != null) {
                    if ((quantity <= item.getQuantity() && quantity >= 0) || GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                        chr.getTrade().setItems(c, item, targetSlot, quantity);
                    }
                }
                break;
            }
            case SET_MESO: {
                final MapleTrade trade = chr.getTrade();
                if (trade != null) {
                    trade.setMeso(slea.readInt());
                }
                break;
            }
            case CONFIRM_TRADE: {
                if (chr.getTrade() != null) {
                    MapleTrade.completeTrade(chr);
                }
                break;
            }
            case MERCHANT_EXIT: {
                /*		final IMaplePlayerShop shop = chr.getPlayerShop();
                 if (shop != null && shop instanceof HiredMerchant && shop.isOwner(chr)) {
                 shop.setOpen(true);
                 chr.setPlayerShop(null);
                 }*/
                break;
            }
            case PLAYER_SHOP_ADD_ITEM:
            case ADD_ITEM: {
                //      System.out.println(slea.toString());
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                final byte slot = (byte) slea.readShort();
                short bundles = slea.readShort(); // 數量
                final short perBundle = slea.readShort(); // Price per bundle
                final int price = slea.readInt();
                if (price <= 0 || bundles <= 0 || perBundle <= 0) {
                    return;
                }
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame || !shop.getCanShop()) {
                    return;
                }
                final IItem ivItem = chr.getInventory(type).getItem(slot);
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ivItem != null) {
                    long check = bundles * perBundle;
                    if (check > 32767 || check <= 0) { //This is the better way to check.
                        return;
                    }
                    final short bundles_perbundle = (short) (bundles * perBundle);
//                    if (bundles_perbundle < 0) { // int_16 overflow
//                        return;
//                    }
                    if (ivItem.getQuantity() >= bundles_perbundle) {
                        final byte flag = ivItem.getFlag();
                        if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        if (ii.isDropRestricted(ivItem.getItemId()) || ii.isAccountShared(ivItem.getItemId())) {
                            if (!(ItemFlag.KARMA_EQ.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
                                c.sendPacket(MaplePacketCreator.enableActions());
                                return;
                            }
                        }

                        if (bundles_perbundle >= 50 && GameConstants.isUpgradeScroll(ivItem.getItemId())) {
                            c.setMonitored(true); //hack check
                        }
                        if (GameConstants.isThrowingStar(ivItem.getItemId()) || GameConstants.isBullet(ivItem.getItemId())) {
                            // Ignore the bundles
                            final IItem sellItem = ivItem.copyWithQuantity(ivItem.getQuantity());
                            shop.addItem(new MaplePlayerShopItem(sellItem, (short) 1, price));
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                        } else {
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, bundles_perbundle, true);

                            final IItem sellItem = ivItem.copy();
                            sellItem.setQuantity(perBundle);
                            shop.addItem(new MaplePlayerShopItem(sellItem, bundles, price));
                        }
                        c.sendPacket(PlayerShopPacket.shopItemUpdate(shop));
                        if (shop instanceof HiredMerchant) {// 精靈商人放物品存檔
                            ((HiredMerchant) shop).saveItems();
                        }
                    }
                }
                break;
            }
            case BUY_ITEM_PLAYER_SHOP:
            case BUY_ITEM_STORE:
            case BUY_ITEM_HIREDMERCHANT: { // Buy and Merchant buy

                if (chr.getTrade() != null) {
                    MapleTrade.completeTrade(chr);
                    break;
                }
                final int item = slea.readByte();
                final short quantity = slea.readShort();
                //slea.skip(4);
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || shop.isOwner(chr) || shop instanceof MapleMiniGame || item >= shop.getItems().size() || !shop.getCanShop()) {
                    return;
                }
                final MaplePlayerShopItem tobuy = shop.getItems().get(item);
                if (tobuy == null) {
                    return;
                }
                long check = tobuy.bundles * quantity;
                long check2 = tobuy.price * quantity;
                long check3 = tobuy.item.getQuantity() * quantity;
                if (check <= 0 || check2 > 2147483647 || check2 <= 0 || check3 > 32767 || check3 < 0) { //This is the better way to check.
                    return;
                }
                if (tobuy.bundles < quantity || (tobuy.bundles % quantity != 0 && GameConstants.isEquip(tobuy.item.getItemId())) || chr.getMeso() - (check2) > 2147483647 || shop.getMeso() + (check2) < 0) {
                    return;
                }
                if (chr.getMeso() - (check2) < 0) {
                    c.getPlayer().dropMessage(1, "您的楓幣不足.");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (shop.getMeso() + (check2) > 2147483647) {
                    c.getPlayer().dropMessage(1, "您購買的商店營業額已經超標，請通知店主來收錢。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (quantity >= 50 && tobuy.item.getItemId() == 2340000) {
                    c.setMonitored(true); //hack check
                }
                if (ServerConfig.LOG_OWNERSTORE) {
                    FileoutputUtil.logToFile("logs/data/個人商店.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "] 玩家 " + c.getPlayer().getName() + " 從  " + chr.getPlayerShop() + " 的個人商店購買了" + MapleItemInformationProvider.getInstance().getName(tobuy.item.getItemId()) + " (" + tobuy.item.getItemId() + ") x" + quantity + " 單個價錢為 : " + tobuy.price + " 道具唯一ID:" + tobuy.item.getEquipOnlyId() + "\r\n");
                }
                shop.buy(c, item, quantity);
                shop.broadcastToVisitors(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case REMOVE_ITEM_PS:
            case REMOVE_ITEM: {
                int slot = slea.readShort(); //0
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame || shop.getItems().size() <= 0 || shop.getItems().size() <= slot || slot < 0 || !shop.getCanShop()) {
                    return;
                }
                final MaplePlayerShopItem item = shop.getItems().get(slot);

                if (item != null) {
                    if (item.bundles > 0) {
                        IItem item_get = item.item.copy();
                        int count = item.item.getQuantity();
                        long check = item.bundles * count;
                        if (check <= 0 || check > 32767) {
                            return;
                        }
                        item_get.setQuantity((short) check);
                        if (item_get.getQuantity() >= 50 && GameConstants.isUpgradeScroll(item.item.getItemId())) {
                            c.setMonitored(true); //hack check
                        }
                        if (MapleInventoryManipulator.checkSpace(c, item_get.getItemId(), item_get.getQuantity(), item_get.getOwner())) {
                            MapleInventoryManipulator.addFromDrop(c, item_get, false);
                            item.bundles = 0;
                            shop.removeFromSlot(slot);
                        }
                    }
                }
                c.sendPacket(PlayerShopPacket.shopItemUpdate(shop));
                if (shop instanceof HiredMerchant) {// 精靈商人移除物品存檔
                    ((HiredMerchant) shop).saveItems();
                }
                break;
            }
            case MAINTANCE_OFF: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop instanceof HiredMerchant && shop.isOwner(chr) && shop.getCanShop()) {
                    //boolean save = false;
                    shop.setOpen(true);
                    chr.setPlayerShop(null);
                    //shop.closeShop(save, true);
                }
                break;
            }
            case MAINTANCE_ORGANISE: {
                final IMaplePlayerShop imps = chr.getPlayerShop();
                if (imps != null && imps.isOwner(chr) && !(imps instanceof MapleMiniGame)) {
                    for (int i = 0; i < imps.getItems().size(); i++) {
                        if (imps.getItems().get(i).bundles == 0) {
                            imps.getItems().remove(i);
                        }
                    }
                    if (chr.getMeso() + imps.getMeso() < 0) {
                        //    System.out.println(new StringBuilder().append("調用位置: ").append(new java.lang.Throwable().getStackTrace()[0]).toString());

                        c.sendPacket(PlayerShopPacket.shopItemUpdate(imps));
                    } else {
                        chr.gainMeso(imps.getMeso(), false);
                        imps.setMeso(0);

                        c.sendPacket(PlayerShopPacket.shopItemUpdate(imps));
                    }
                } else {
                    c.sendPacket(MaplePacketCreator.enableActions());
                }
                break;
            }
            case CLOSE_MERCHANT: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr) && merchant.isAvailable()) {
                    c.sendPacket(PlayerShopPacket.shopErrorMessage(0x11, 0));
                    c.sendPacket(MaplePacketCreator.getPopupMsg("請去找富蘭德里領取你的裝備和楓幣"));
                    c.sendPacket(MaplePacketCreator.enableActions());
                    merchant.removeAllVisitors(-1, -1);
                    chr.setPlayerShop(null);
                    merchant.closeShop(true, true);
                }
                break;
                /*  final IMaplePlayerShop merchant = chr.getPlayerShop();
                 if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                 //c.sendPacket(MaplePacketCreator.getPopupMsg("請去找富蘭德里領取你的裝備和楓幣"));
                 boolean save = false;

                 if (chr.getMeso() + merchant.getMeso() < 0) {
                 save = true;
                 } else {
                 if (merchant.getMeso() > 0) {
                 chr.gainMeso(merchant.getMeso(), false);
                 }
                 merchant.setMeso(0);

                 if (merchant.getItems().size() > 0) {
                 for (MaplePlayerShopItem items : merchant.getItems()) {
                 if (items.bundles > 0) {
                 IItem item_get = items.item.copy();
                 item_get.setQuantity((short) (items.bundles * items.item.getQuantity()));
                 if (MapleInventoryManipulator.addFromDrop(c, item_get, false)) {
                 items.bundles = 0;
                 } else {
                 save = true;
                 break;
                 }
                 }
                 }
                 }
                 }
                 //c.sendPacket(PlayerShopPacket.shopErrorMessage(0x10, 0));
                 merchant.closeShop(save, true);
                 chr.setPlayerShop(null);
                 }
                 break;*/
            }
            case TRADE_SOMETHING:
            case ADMIN_STORE_NAMECHANGE: { // Changing store name, only Admin
                // 01 00 00 00
                break;
            }
//            case VIEW_MERCHANT_VISITOR: {
//                final IMaplePlayerShop merchant = chr.getPlayerShop();
//                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
//                    ((HiredMerchant) merchant).sendVisitor(c);
//                }
//                break;
//            }
//            case VIEW_MERCHANT_BLACKLIST: {
//                final IMaplePlayerShop merchant = chr.getPlayerShop();
//                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
//                    ((HiredMerchant) merchant).sendBlackList(c);
//                }
//                break;
//            }
//            case MERCHANT_BLACKLIST_ADD: {
//                final IMaplePlayerShop merchant = chr.getPlayerShop();
//                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
//                    ((HiredMerchant) merchant).addBlackList(slea.readMapleAsciiString());
//                }
//                break;
//            }
//            case MERCHANT_BLACKLIST_REMOVE: {
//                final IMaplePlayerShop merchant = chr.getPlayerShop();
//                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
//                    ((HiredMerchant) merchant).removeBlackList(slea.readMapleAsciiString());
//                }
//                break;
//            }
            case GIVE_UP: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(chr)));
                    game.nextLoser();
                    game.setOpen(true);
                    game.update();
                    game.checkExitAfterGame();
                }
                break;
            }
            case EXPEL: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    if (!((MapleMiniGame) ips).isOpen()) {
                        break;
                    }
                    ips.removeAllVisitors(5, 1); //no msg
                }
                break;
            }
            case READY:
            case UN_READY: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (!game.isOwner(chr) && game.isOpen()) {
                        game.setReady(game.getVisitorSlot(chr));
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameReady(game.isReady(game.getVisitorSlot(chr))));
                    }
                }
                break;
            }
            case START: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOwner(chr) && game.isOpen()) {
                        for (int i = 1; i < ips.getSize(); i++) {
                            if (!game.isReady(i)) {
                                return;
                            }
                        }
                        game.setGameType();
                        game.shuffleList();
                        if (game.getGameType() == 1) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameStart(game.getLoser()));
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardStart(game, game.getLoser()));
                        }
                        game.setOpen(false);
                        game.update();
                    }
                }
                break;
            }
            case REQUEST_TIE: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestTie(), false);
                    } else {
                        game.getMCOwner().getClient().sendPacket(PlayerShopPacket.getMiniGameRequestTie());
                    }
                    game.setRequestedTie(game.getVisitorSlot(chr));
                }
                break;
            }
            case ANSWER_TIE: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getRequestedTie() > -1 && game.getRequestedTie() != game.getVisitorSlot(chr)) {
                        if (slea.readByte() > 0) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 1, game.getRequestedTie()));
                            game.nextLoser();
                            game.setOpen(true);
                            game.update();
                            game.checkExitAfterGame();
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyTie());
                        }
                        game.setRequestedTie(-1);
                    }
                }
                break;
            }

            case REQUEST_REDO: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestREDO(), false);
                    } else {
                        game.getMCOwner().getClient().sendPacket(PlayerShopPacket.getMiniGameRequestREDO());
                    }
                    game.setRequestedTie(game.getVisitorSlot(chr));
                }
                break;
            }
            case ANSWER_REDO: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    //     if (game.getRequestedTie() > -1 && game.getRequestedTie() != game.getVisitorSlot(chr)) {
                    if (slea.readByte() > 0) {
                        ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip1(ips.getVisitorSlot(chr)));
                        game.nextLoser();
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyTie());
                    }
                    game.setRequestedTie(-1);
                }
                //}
                break;
            }
            case SKIP: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    /* if (game.getLoser() != ips.getVisitorSlot(chr)) {
                     ips.broadcastToVisitors(PlayerShopPacket.shopChat("反過來不能由被跳過 " + chr.getName() + ". 失敗者: " + game.getLoser() + " 遊客: " + ips.getVisitorSlot(chr), ips.getVisitorSlot(chr)));
                     return;
                     }*/
                    ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip(ips.getVisitorSlot(chr)));
                    game.nextLoser();
                }
                break;
            }
            case MOVE_OMOK: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    /* if (game.getLoser() != game.getVisitorSlot(chr)) {
                     game.broadcastToVisitors(PlayerShopPacket.shopChat("不能放在通過 " + chr.getName() + ". 失敗者: " + game.getLoser() + " 遊客: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                     return;
                     }*/
                    game.setPiece(slea.readInt(), slea.readInt(), slea.readByte(), chr);
                }
                break;
            }
            case SELECT_CARD: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    /* if (game.getLoser() != game.getVisitorSlot(chr)) {
                     game.broadcastToVisitors(PlayerShopPacket.shopChat("不能放在通過 " + chr.getName() + ". 失敗者: " + game.getLoser() + " 遊客: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                     return;
                     }*/
                    if (slea.readByte() != game.getTurn()) {
                        game.broadcastToVisitors(PlayerShopPacket.shopChat("不能放在通過 " + chr.getName() + ". 失敗者: " + game.getLoser() + " 遊客: " + game.getVisitorSlot(chr) + " 是否為真: " + game.getTurn(), game.getVisitorSlot(chr)));
                        return;
                    }
                    final int slot = slea.readByte();
                    final int turn = game.getTurn();
                    final int fs = game.getFirstSlot();
                    if (turn == 1) {
                        game.setFirstSlot(slot);
                        if (game.isOwner(chr)) {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn), false);
                        } else {
                            game.getMCOwner().getClient().sendPacket(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn));
                        }
                        game.setTurn(0); //2nd turn nao
                        return;
                    } else if (game.getCardId(fs + 1) == game.getCardId(slot + 1)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 2 : 3));
                        game.setPoints(game.getVisitorSlot(chr)); //correct.. so still same loser. diff turn tho
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 0 : 1));
                        game.nextLoser();//wrong haha

                    }
                    game.setTurn(1);
                    game.setFirstSlot(0);

                }
                break;
            }
            case EXIT_AFTER_GAME: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(chr)));
                    game.nextLoser();
                    game.setOpen(true);
                    game.update();
                    game.checkExitAfterGame();
                }
                break;
            }
            case CANCEL_EXIT: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.setExitAfter(chr);
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameExitAfter(game.isExitAfter(chr)));
                }
                break;
            }
            default: {
                //some idiots try to send huge amounts of data to this (:
                //System.out.println("Unhandled interaction action by " + chr.getName() + " : " + action + ", " + slea.toString());
                //19 (0x13) - 00 OR 01 -> itemid(maple leaf) ? who knows what this is
                break;
            }
        }
    }
}
