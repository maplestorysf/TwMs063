package handling.cashshop.handler;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.IItem;
import constants.ServerConfig;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import java.sql.Timestamp;
import java.util.List;
import server.CashItem;
import server.CashItemFactory;
import server.CashModItem;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.RandomRewards;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;
import tools.Pair;
import tools.data.LittleEndianAccessor;

public class CashShopOperation {

    public static void LeaveCashShop(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorageMTS().deregisterPlayer(chr);
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        int loginStatus = c.getLoginState();
        if (loginStatus != MapleClient.LOGIN_CS_LOGGEDIN) {
            c.getSession().close();
            c.disconnect(false, true);
            return;
        }
        if (chr.getEventInstance() != null) {
            c.getSession().close();
            c.disconnect(false, true);
            return;
        }
        c.updateLoginState(MapleClient.CASH_SHOP_TRANSITION_LEAVE, c.getSessionIPAddress());
        try {
            World.channelChangeData(new CharacterTransfer(chr), c, chr.getId(), c.getWorld(), c.getChannel());
            c.sendPacket(MaplePacketCreator.getChannelChange(ChannelServer.getInstance(c.getWorld(), c.getChannel()).getIP(), ChannelServer.getInstance(c.getWorld(), c.getChannel()).getPort()));
        } finally {
            c.disconnect(true, true);
            c.setPlayer(null);
            c.setReceiving(false);
        }
    }

    public static void EnterCashShop(final int playerid, final MapleClient client) {
        client.loadAccountidByPlayerid(playerid);

        if (World.Find.findDisconnect(client.getAccID()) > 0) {
            System.out.println("(EnterCashShop) 商城角色複製: " + playerid + " 帳號id: " + client.getAccID());
            FileoutputUtil.logToFile("logs/Hack/角色複製.txt", FileoutputUtil.CurrentReadable_Time() + " 玩家id: " + playerid + " 帳號id: " + client.getAccID() + " 商城角色複製 (EnterCashShop)");
            World.Find.forceDeregisterDisconnect(client.getAccID());
            client.getSession().close();
            return;
        } else if (!MapleCharacterUtil.isExistCharacterInDataBase(playerid)) {
            System.out.println("<刪除角色> (EnterCashShop)  頻道<" + client.getChannel() + ">角色複製: " + playerid + " 帳號id: " + client.getAccID());
            FileoutputUtil.logToFile("logs/Hack/角色複製.txt", FileoutputUtil.CurrentReadable_Time() + " <刪除角色複製> 玩家id: " + playerid + " 帳號id:" + client.getAccID());
            client.getSession().close();
            return;
        }
        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean mts = false;
        if (transfer == null) {
            transfer = CashShopServer.getPlayerStorageMTS().getPendingCharacter(playerid);
            mts = true;
            if (transfer == null) {
                client.getSession().close();
                //   client.disconnect(false, false);
                return;
            }
        }

        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, client, false);
        if (chr.getEventInstance() != null) {
            client.getSession().close();
            return;
        } else if (World.isPlayerSaving(chr.getAccountID())) {
            FileoutputUtil.logToFile("logs/data/儲存中載入.txt", FileoutputUtil.CurrentReadable_Time() + " 角色: " + chr.getName() + "(" + chr.getId() + ") 帳號: " + client.getAccountName() + "(" + chr.getAccountID() + ") \r\n ");
            client.getSession().close();
            return;
        }

        client.setAccID(chr.getAccountID());
        client.setWorld(chr.getWorld());
        client.loadAccountData(chr.getAccountID());
        if (!client.CheckIPAddress()) { // Remote hack
            client.getSession().close();
            //   client.disconnect(false, true);
            return;
        }

        final int state = client.getLoginState();
        boolean allowLogin = false;
        if (state == MapleClient.CASH_SHOP_TRANSITION) {
            if (!World.isCharacterListConnected(client.loadCharacterNames(client.getWorld()))) {
                //   if (!World.isConnected(chr.getName())) {
                allowLogin = true;
            }
        }
        // System.out.println( state );

        if (!allowLogin) {
            client.setPlayer(null);
            client.getSession().close();
            //client.disconnect(false, false);
            return;
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        if (client.getLastLogin() + 5 * 1000 < currentTime.getTime()) {
            client.setReceiving(false);
            client.getSession().close();
            return;
        }
        client.setPlayer(chr);

        client.updateLoginState(MapleClient.LOGIN_CS_LOGGEDIN, client.getSessionIPAddress());
        if (mts) {
            CashShopServer.getPlayerStorageMTS().registerPlayer(chr);
            client.sendPacket(MTSCSPacket.startMTS(chr, client));
            //MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(client.getPlayer().getId()), client);
        } else {
            CashShopServer.getPlayerStorage().registerPlayer(chr);
            client.sendPacket(MTSCSPacket.warpCS(client));
            sendCashShopUpdate(client);
        }
    }

    public static void sendCashShopUpdate(final MapleClient c) {
        c.sendPacket(MTSCSPacket.showCashShopAcc(c));
        c.sendPacket(MTSCSPacket.showGifts(c));
        RefreshCashShop(c);
        c.sendPacket(MTSCSPacket.sendShowWishList(c.getPlayer()));
    }

    public static void CouponCode(final String code, final MapleClient c) {
        boolean validcode = false;
        int type = -1, item = -1, size = -1, time = -1;

        validcode = MapleCharacterUtil.getNXCodeValid(code.toUpperCase(), validcode);

        if (validcode) {
            type = MapleCharacterUtil.getNXCodeType(code);
            item = MapleCharacterUtil.getNXCodeItem(code);
            size = MapleCharacterUtil.getNXCodeSize(code);
            time = MapleCharacterUtil.getNXCodeTime(code);
            if (type <= 4) {
                try {
                    MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            /*
             * 類型說明！
             * 基本上，這使得優惠券代碼做不同的東西！
             *
             * Type 1: GASH點數
             * Type 2: 楓葉點數
             * Type 3: 物品x數量(默認1個)
             * Type 4: 楓幣
             */
            int maplePoints = 0, mesos = 0, as = 0;
            String cc = "", tt = "";
            switch (type) {
                case 1:
                    c.getPlayer().modifyCSPoints(1, item, false);
                    maplePoints = item;
                    cc = "GASH";
                    break;
                case 2:
                    c.getPlayer().modifyCSPoints(2, item, false);
                    maplePoints = item;
                    cc = "楓葉點數";
                    break;
                case 3:
                    MapleInventoryManipulator.addById(c, item, (short) size, "優待卷禮品.", null, time);
                    as = 1;
                    break;
                case 4:
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    cc = "楓幣";
                    break;
            }
            if (time == -1) {
                tt = "永久";
                as = 2;
            }
            switch (as) {
                case 1:
                    //c.sendPacket(MTSCSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
                    c.getPlayer().dropMessage(1, "已成功使用優待卷獲得" + MapleItemInformationProvider.getInstance().getName(item) + time + "天 x" + size + "。");
                    break;
                case 2:
                    c.getPlayer().dropMessage(1, "已成功使用優待卷獲得" + MapleItemInformationProvider.getInstance().getName(item) + "永久 x" + size + "。");
                    break;
                default:
                    c.getPlayer().dropMessage(1, "已成功使用優待卷獲得" + item + cc);
                    break;
            }
        } else {
            c.sendPacket(MTSCSPacket.sendCSFail(0xB3)); //idb
        }
        RefreshCashShop(c);
    }

    public static final void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {

        final int action = slea.readByte();

        CashItemFactory cif = CashItemFactory.getInstance();
        switch (action) {
            case 2:
            case 26: {   // Buy Item
                final int useNX = slea.readByte() + 1;
                final int snCS = slea.readInt();
                CashItem cItem = cif.getSimpleItem(snCS);
                CashModItem cmItem = cif.getModItem(snCS);
                List<Integer> ccc = null;
                if (action == 26 && cItem != null) {
                    ccc = CashItemFactory.getInstance().getPackageItems(cItem.getId());
                }
                boolean canBuy = true;
                int errorCode = 0;

                if ((cItem == null && cmItem == null) || (action == 30 && (ccc == null || ccc != null && ccc.isEmpty())) || useNX < 1 || useNX > 2) {
                    canBuy = false;
                } else if (cmItem == null || !cmItem.isOnSale()) {
                    canBuy = false;
                    errorCode = 225;
                } else if (chr.getCSPoints(useNX) < cmItem.getPrice()) {
                    if (useNX == 1) {
                        errorCode = 168;
                    } else {
                        errorCode = 225;
                    }
                    canBuy = false;
                } else if (!cmItem.genderEquals(c.getPlayer().getGender())) {
                    canBuy = false;
                    errorCode = 186;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    canBuy = false;
                    errorCode = 175;
                } else if (cmItem.getPrice() < 0) {
                    canBuy = false;
                }
                if (canBuy && cmItem != null) {
                    for (int i : GameConstants.cashBlock) {
                        if (cmItem.getId() == i) {
                            c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(cmItem.getId()));
                            RefreshCashShop(c);
                            return;
                        }
                    }

                    if (action == 2) { // 購買單個道具
                        chr.modifyCSPoints(useNX, -cmItem.getPrice(), false);
                        IItem itemz = chr.getCashInventory().toItem(cmItem, chr);
                        if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == cmItem.getId() && itemz.getQuantity() == cmItem.getCount()) {
                            chr.getCashInventory().addToInventory(itemz);
                            c.sendPacket(MTSCSPacket.showBoughtCashItem(itemz, cmItem.getSN(), c.getAccID()));
                            if (ServerConfig.LOG_CSBUY) {
                                FileoutputUtil.logToFile("logs/data/商城購買.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + cmItem.getPrice() + "點 來購買" + cmItem.getId() + "x" + cmItem.getCount());
                            }
                        } else {
                            c.sendPacket(MTSCSPacket.sendCSFail(errorCode));
                        }
                    } else { // 套裝
                        Map<Integer, IItem> ccz = new HashMap<>();
                        for (int i : ccc) {
                            final CashItem cii = CashItemFactory.getInstance().getSimpleItem(i);
                            for (int iz : GameConstants.cashBlock) {
                                if (cii.getId() == iz) {
                                    continue;
                                }
                            }
                            IItem itemz = chr.getCashInventory().toItem(cii, chr, MapleInventoryManipulator.getUniqueId(cii.getId(), null), "");
                            if (itemz == null || itemz.getUniqueId() <= 0 || itemz.getItemId() != cii.getId()) {
                                continue;
                            }
                            ccz.put(cii.getSN(), itemz);
                            c.getPlayer().getCashInventory().addToInventory(itemz);
                        }
                        if (ServerConfig.LOG_CSBUY) {
                            FileoutputUtil.logToFile("logs/data/商城購買.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + cmItem.getPrice() + "點 來購買套裝" + cmItem.getId() + "x" + cmItem.getCount());
                        }
                        chr.modifyCSPoints(useNX, -cmItem.getPrice(), false);
                        c.sendPacket(MTSCSPacket.showBoughtCashPackage(ccz, c.getAccID()));
                    }

                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(errorCode));
                }

                RefreshCashShop(c);
                break;
            }
            case 3: { // gift
                final String secondPassword = slea.readMapleAsciiString();
                final int sn = slea.readInt();
                final String characterName = slea.readMapleAsciiString();
                final String message = slea.readMapleAsciiString();

                boolean canBuy = true;
                int errorCode = 0;
                CashModItem cItem = cif.getModItem(sn);

                Pair<Integer, Pair<Integer, Integer>> info = MapleCharacterUtil.getInfoByName(characterName, c.getPlayer().getWorld());

                if (cItem == null) {
                    canBuy = false;
                } else if (!cItem.isOnSale()) {
                    canBuy = false;
                    errorCode = 225;
                } else if (chr.getCSPoints(1) < cItem.getPrice()) {
                    errorCode = 168;
                    canBuy = false;
                } else if (!c.check2ndPassword(secondPassword)) {
                    canBuy = false;
                    errorCode = 197;
                } else if (message.getBytes().length < 1 || message.getBytes().length > 74) {
                    canBuy = false;
                    errorCode = 225;
                } else if (info == null) {
                    canBuy = false;
                    errorCode = 172;
                } else if (info.getRight().getLeft() == c.getAccID() || info.getLeft() == c.getPlayer().getId()) {
                    canBuy = false;
                    errorCode = 171;
                } else if (!cItem.genderEquals(info.getRight().getRight())) {
                    canBuy = false;
                    errorCode = 176;
                }
                if (canBuy && info != null && cItem != null) {
                    for (int i : GameConstants.cashBlock) {
                        if (cItem.getId() == i) {
                            c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(cItem.getId()));
                            return;
                        }
                    }
                    c.getPlayer().getCashInventory().gift(info.getLeft(), c.getPlayer().getName(), message, cItem.getSN(), MapleInventoryIdentifier.getInstance());
                    c.getPlayer().modifyCSPoints(1, -cItem.getPrice(), false);
                    if (ServerConfig.LOG_CSBUY) {
                        FileoutputUtil.logToFile("logs/data/商城送禮.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了點券" + cItem.getPrice() + "點 贈送了" + cItem.getId() + "x" + cItem.getCount() + " 給" + characterName);
                    }
                    c.sendPacket(MTSCSPacket.sendGift(characterName, cItem, cItem.getPrice() / 2, false));
                    chr.sendNote(characterName, chr.getName() + " 送了你禮物! 趕快去商城確認看看.", (byte) 0); //fame or not
                    MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(characterName);
                    if (receiver != null) {
                        receiver.showNote();
                    }
                    //c.sendPacket(MTSCSPacket.sendGift(cItem.getPrice(), cItem.getId(), cItem.getCount(), characterName), f);
                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(errorCode));
                }
                RefreshCashShop(c);
                break;
            }

            case 4: { //Wish List
                chr.clearWishlist();
                if (slea.available() < 40) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                }
                int[] wishlist = new int[10];
                for (int i = 0; i < 10; i++) {
                    wishlist[i] = slea.readInt();
                }
                chr.setWishlist(wishlist);
                c.sendPacket(MTSCSPacket.setWishList(chr));
                RefreshCashShop(c);
                break;
            }
            ////////////////////
            case 5: {

                final int useNX = slea.readByte() + 1;
                final boolean coupon = slea.readByte() > 0;
                if (coupon) {
                    final MapleInventoryType type = getInventoryType(slea.readInt());
                    int value = 41, maxSlot = 96;
                    switch (type) {
                        case EQUIP:
                            value = 89;
                            maxSlot = 96;
                            break;
                        case USE:
                            value = 89;
                            maxSlot = 96;
                            break;
                        case SETUP:
                            value = 89;
                            maxSlot = 96;
                            break;
                        case ETC:
                            value = 89;
                            maxSlot = 96;
                            break;
                    }
                    if (chr.getCSPoints(useNX) >= 100 && chr.getInventory(type).getSlotLimit() < value) {
                        chr.modifyCSPoints(useNX, -100, false);
                        chr.getInventory(type).addSlot((byte) 8);
                        chr.dropMessage(1, "欄位已經擴充到 " + chr.getInventory(type).getSlotLimit() + " 格。");
                        if (ServerConfig.LOG_CSBUY) {
                            FileoutputUtil.logToFile("logs/data/商城擴充.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + "100點 來購買擴充欄位" + type.name() + "8格 目前共有" + chr.getInventory(type).getSlotLimit() + "格");
                        }
                    } else {
                        chr.dropMessage("最大可擴充至 " + maxSlot + " 格。");
                        //c.sendPacket(MTSCSPacket.sendCSFail(0xA4));
                    }
                } else {
                    final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                    int value = 41, maxSlot = 96;
                    switch (type) {
                        case EQUIP:
                            value = 96;
                            maxSlot = 96;
                            break;
                        case USE:
                            value = 96;
                            maxSlot = 96;
                            break;
                        case SETUP:
                            value = 96;
                            maxSlot = 96;
                            break;
                        case ETC:
                            value = 96;
                            maxSlot = 96;
                            break;
                    }
                    if (chr.getCSPoints(useNX) >= 100 && chr.getInventory(type).getSlotLimit() < value) {
                        chr.modifyCSPoints(useNX, -100, false);
                        chr.getInventory(type).addSlot((byte) 4);
                        chr.dropMessage(1, "欄位已經擴充到 " + chr.getInventory(type).getSlotLimit() + " 格。");
                        if (ServerConfig.LOG_CSBUY) {
                            FileoutputUtil.logToFile("logs/data/商城擴充.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + "100點 來購買擴充欄位" + type.name() + "4格 目前共有" + chr.getInventory(type).getSlotLimit() + "格");
                        }
                    } else {
                        chr.dropMessage("最大可擴充至 " + maxSlot + " 格。");
                        //c.sendPacket(MTSCSPacket.sendCSFail(0xA4));
                    }
                }
                RefreshCashShop(c);
                break;
            }
            case 6: {
                final int useNX = slea.readByte() + 1;
                if (chr.getCSPoints(useNX) >= 100 && chr.getStorage().getSlots() < 96) {
                    chr.modifyCSPoints(useNX, -100, false);
                    chr.getStorage().increaseSlots((byte) 4);
                    chr.getStorage().saveToDB();
                    if (ServerConfig.LOG_CSBUY) {
                        FileoutputUtil.logToFile("logs/data/商城擴充.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + "100點 來購買擴充欄位倉庫4格 目前共有" + chr.getStorage().getSlots() + "格");
                    }
                    c.sendPacket(MTSCSPacket.increasedStorageSlots(chr.getStorage().getSlots()));
                } else {
                    chr.dropMessage("最大可擴充至96格。");
                    //c.sendPacket(MTSCSPacket.sendCSFail(0xA4));
                }
                RefreshCashShop(c);
                break;
            }

            case 7: {
                final int useNX = slea.readByte() + 1;
                //     slea.readByte();
                //CashItem item = cif.getSimpleItem(slea.readInt());
                CashModItem item = cif.getModItem(slea.readInt());
                int slots = c.getCharacterSlots();
                if (item == null || c.getPlayer().getCSPoints(useNX) < item.getPrice() || slots > 15) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                }
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice(), false);
                if (c.gainCharacterSlot()) {
                    c.sendPacket(MTSCSPacket.increasedStorageSlots(slots + 1));
                    if (ServerConfig.LOG_CSBUY) {
                        FileoutputUtil.logToFile("logs/data/商城擴充.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了" + (useNX == 1 ? "點券" : "楓葉點數") + item.getPrice() + "點 來購買擴充角色欄位 目前共有" + c.getCharacterSlots() + "格");
                    }
                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(0));
                }
                RefreshCashShop(c);
                break;
            }

            case 10: {
                IItem item = c.getPlayer().getCashInventory().findByCashId((int) slea.readLong());
                if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                    IItem item_ = item.copy();
                    short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                    if (pos >= 0) {
                        if (item_.getPet() != null) {
                            item_.getPet().setInventoryPosition(pos);
                            c.getPlayer().addPet(item_.getPet());
                        }
                        c.getPlayer().getCashInventory().removeFromInventory(item);
                        c.sendPacket(MTSCSPacket.confirmFromCSInventory(item_, pos));
                    } else {
                        c.sendPacket(MTSCSPacket.sendCSFail(0xB1));
                    }
                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(0xB1));
                }
                RefreshCashShop(c);
                break;
            }

            case 11: {
                int uniqueid = (int) slea.readLong();
                MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                IItem item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
                if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                    IItem item_ = item.copy();
                    c.getPlayer().getInventory(type).removeItem(item.getPosition(), item.getQuantity(), false);
                    int sn = CashItemFactory.getInstance().getItemSN(item_.getItemId());
                    if (item_.getPet() != null) {
                        c.getPlayer().removePet(item_.getPet());
                    }
                    item_.setPosition((byte) 0);
                    item_.setGMLog("購物商城購買 時間: " + FileoutputUtil.CurrentReadable_Time());
                    c.getPlayer().getCashInventory().addToInventory(item_);
                    c.sendPacket(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), sn));
                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(0xB1));
                }
                RefreshCashShop(c);
                break;
            }
            case 22: {
                String secondpw = slea.readMapleAsciiString(); // as13
                int uniqueid = (int) slea.readLong();
                IItem item = c.getPlayer().getCashInventory().findByCashId(uniqueid);

                // 有期限、非裝備
                if (item == null || !GameConstants.isEquip(item.getItemId()) || item.getExpiration() != -1) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                } else if (!c.check2ndPassword(secondpw)) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xC5));
                    RefreshCashShop(c);
                    return;
                }
                int sn = cif.getItemSN(item.getItemId());
                CashItem citem = cif.getSimpleItem(sn);
                CashModItem cmitem = cif.getModItem(sn);
                if (citem == null) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                }
                c.getPlayer().modifyCSPoints(2, (int) Math.round((cmitem == null ? citem.getPrice() : cmitem.getPrice()) * 0.3), false);
                c.getPlayer().getCashInventory().removeFromInventory(item);
                RefreshCashShop(c);

                break;
            }
            case 27: { // Package gift
                final String secondPassword = slea.readMapleAsciiString();
                final int sn = slea.readInt();
                final String characterName = slea.readMapleAsciiString();
                final String message = slea.readMapleAsciiString();

                CashItem cItem = cif.getSimpleItem(sn);
                IItem item = chr.getCashInventory().toItem(cItem);

                Pair<Integer, Pair<Integer, Integer>> info = MapleCharacterUtil.getInfoByName(characterName, c.getPlayer().getWorld());
                if (c.getSecondPassword() != null) {
                    if (secondPassword == null) { // 確認是否外掛
                        c.getPlayer().dropMessage(1, "請輸入密碼。");
                        RefreshCashShop(c);
                        return;
                    } else if (!c.check2ndPassword(secondPassword)) { // 第二密碼錯誤
                        c.getPlayer().dropMessage(1, "密碼錯誤。");
                        RefreshCashShop(c);
                        return;
                    }
                    if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getRight().getLeft() == c.getAccID()) {
                        c.sendPacket(MTSCSPacket.sendCSFail(0xA2)); //9E v75
                        RefreshCashShop(c);
                        return;
                    } else if (!cItem.genderEquals(info.getRight().getRight())) {
                        c.sendPacket(MTSCSPacket.sendCSFail(0xA3));
                        RefreshCashShop(c);
                        return;
                    } else {
                        for (int i : GameConstants.cashBlock) {
                            if (cItem.getId() == i) {
                                c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(cItem.getId()));
                                RefreshCashShop(c);
                                return;
                            }
                        }
                        c.getPlayer().getCashInventory().gift(info.getLeft(), c.getPlayer().getName(), message, cItem.getSN(), MapleInventoryIdentifier.getInstance());
                        c.getPlayer().modifyCSPoints(1, -cItem.getPrice(), false);
                        c.sendPacket(MTSCSPacket.sendGift(characterName, cItem, cItem.getPrice() / 2, false));
                        chr.sendNote(characterName, chr.getName() + " 送了你禮物! 趕快去商城確認看看.", (byte) 0); //fame or not
                        MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(characterName);
                        if (receiver != null) {
                            receiver.showNote();
                        }
                    }
                }
                RefreshCashShop(c);
                break;
            }
            case 28: { //1 meso
                final CashModItem item = cif.getModItem(slea.readInt());
                if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                } else if (c.getPlayer().getMeso() < item.getPrice()) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0xB8));
                    RefreshCashShop(c);
                    return;
                } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0xB1));
                    RefreshCashShop(c);
                    return;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (item.getId() == iz) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        RefreshCashShop(c);
                        return;
                    }
                }
                byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null);
                if (pos < 0) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0xB1));
                    RefreshCashShop(c);
                    return;
                }
                chr.gainMeso(-item.getPrice(), false);
                c.sendPacket(MTSCSPacket.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), pos, item.getId()));
                RefreshCashShop(c);
                break;
            }

            case 25: // crush ring
            case 31: { // friendRing
                /*
                 E6 00 
                 23 
                 08 00 5D 31 31 31 31 31 31 31 
                 EB E8 3E 01 
                 09 00 71 77 65 71 77 65 71 65 71 
                 04 00 58 44 44 0A
                 */
                final String secondPassword = slea.readMapleAsciiString();
                final int sn = slea.readInt();
                final String partnerName = slea.readMapleAsciiString();
                final String message = slea.readMapleAsciiString();
                final CashModItem cItem = cif.getModItem(sn);
                Pair<Integer, Pair<Integer, Integer>> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());

                boolean canBuy = true;
                int errorCode = 0;

                if (cItem == null) {
                    canBuy = false;
                } else if (!cItem.isOnSale()) {
                    canBuy = false;
                    errorCode = 225;
                } else if (chr.getCSPoints(1) < cItem.getPrice()) {
                    errorCode = 168;
                    canBuy = false;
                } else if (!c.check2ndPassword(secondPassword)) {
                    canBuy = false;
                    errorCode = 197;
                } else if (message.getBytes().length < 1 || message.getBytes().length > 74) {
                    canBuy = false;
                    errorCode = 225;
                } else if (info == null) {
                    canBuy = false;
                    errorCode = 172;
                } else if (info.getRight().getLeft() == c.getAccID() || info.getLeft() == c.getPlayer().getId()) {
                    canBuy = false;
                    errorCode = 171;
                } else if (!cItem.genderEquals(info.getRight().getRight())) {
                    canBuy = false;
                    errorCode = 176;
                } else if (!GameConstants.isEffectRing(cItem.getId())) {
                    canBuy = false;
                    errorCode = 0;
                } else if (info.getRight().getRight() == c.getPlayer().getGender() && action == 29) {
                    canBuy = false;
                    errorCode = 191;
                }
                if (canBuy && info != null && cItem != null) {
                    for (int i : GameConstants.cashBlock) { //just incase hacker
                        if (cItem.getId() == i) {
                            c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(cItem.getId()));
                            RefreshCashShop(c);
                            return;
                        }
                    }
                    int err = MapleRing.createRing(cItem.getId(), c.getPlayer(), partnerName, message, info.getLeft(), cItem.getSN());
                    if (err != 1) {
                        c.sendPacket(MTSCSPacket.sendCSFail(0)); //9E v75
                        RefreshCashShop(c);
                        return;
                    }

                    c.getPlayer().modifyCSPoints(1, -cItem.getPrice(), false);
                    chr.sendNote(partnerName, chr.getName() + " 送了你禮物! 趕快去商城確認看看.", (byte) 0); //fame or not
                    MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(partnerName);
                    if (receiver != null) {
                        receiver.showNote();
                    }
                    if (ServerConfig.LOG_CSBUY) {
                        FileoutputUtil.logToFile("logs/data/商城送禮.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了點券" + cItem.getPrice() + "點 贈送了" + cItem.getId() + "x" + cItem.getCount() + " 給" + partnerName);
                    }
                } else {
                    c.sendPacket(MTSCSPacket.sendCSFail(errorCode));
                }
                RefreshCashShop(c);
                break;
            }
            case 45: { // 送禮後重整處理
                RefreshCashShop(c);
                break;
            }
            case 47: { //楓葉點數購買
                CashModItem item = cif.getModItem(slea.readInt());
                if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice()) {
                    c.sendPacket(MTSCSPacket.sendCSFail(0));
                    RefreshCashShop(c);
                    return;
                }
                switch (item.getPrice()) {
                    case 50:
                        c.getPlayer().modifyCSPoints(2, item.getPrice(), false);
                        break;
                    case 150:
                        c.getPlayer().modifyCSPoints(2, item.getPrice(), false);
                        break;
                    case 500:
                        c.getPlayer().modifyCSPoints(2, item.getPrice(), false);
                        break;
                }
                chr.dropMessage(1, "成功購買楓葉點數:" + item.getPrice());
                c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                RefreshCashShop(c);
                break;
            }
            default:
                c.sendPacket(MTSCSPacket.sendCSFail(0));
                RefreshCashShop(c);
        }

    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200075:
                return MapleInventoryType.EQUIP;
            case 50200074:
                return MapleInventoryType.USE;
            case 50200073:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    private static void RefreshCashShop(MapleClient c) {
        c.sendPacket(MTSCSPacket.showCashInventory(c));
        c.sendPacket(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
        c.sendPacket(MTSCSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c);
    }

    public static void sendWebSite(final MapleClient c) {
        c.sendPacket(MTSCSPacket.sendWEB(c));
        RefreshCashShop(c);
    }

    public static final void UseXmaxsSurprise(final LittleEndianAccessor slea, final MapleClient c) {
        int CashId = (int) slea.readLong();
        IItem item = c.getPlayer().getCashInventory().findByCashId(CashId);
        if (item != null && item.getItemId() == 5222000 && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
            final int RewardIemId = RandomRewards.getInstance().getXmasreward();
            final CashItem rewardItem = CashItemFactory.getInstance().getItem(RewardIemId);

            if (rewardItem == null) {
                c.sendPacket(MTSCSPacket.sendCSFail(0));
                RefreshCashShop(c);
                return;
            }

            for (int i : GameConstants.cashBlock) {
                if (rewardItem.getId() == i) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(rewardItem.getId()));
                    RefreshCashShop(c);
                    return;
                }
            }

            IItem itemz = c.getPlayer().getCashInventory().toItem(rewardItem);
            if (itemz != null) {
                if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.sendPacket(MTSCSPacket.showXmasSurprise(true, CashId, itemz, c.getAccID()));
                    RefreshCashShop(c);
                    return;
                }
                c.getPlayer().getCashInventory().addToInventory(itemz);
                c.sendPacket(MTSCSPacket.showXmasSurprise(false, CashId, itemz, c.getAccID()));
                c.getPlayer().getCashInventory().removeFromInventory(item);
            } else {
                c.sendPacket(MTSCSPacket.sendCSFail(0));
            }
        }
    }
}
