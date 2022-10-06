package server;

import java.util.LinkedList;
import java.util.List;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import constants.GameConstants;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import constants.ServerConfig;
import constants.WorldConstants;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.lang.ref.WeakReference;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;

public class MapleTrade {

    private MapleTrade partner = null;
    private final List<IItem> items = new LinkedList<>();
    private List<IItem> exchangeItems;
    private int meso = 0, exchangeMeso = 0;
    private boolean locked = false, canCancel = false, canceling = false, completing = false;
    private final WeakReference<MapleCharacter> chr;
    private final byte tradingslot;

    public MapleTrade(final byte tradingslot, final MapleCharacter chr) {
        this.tradingslot = tradingslot;
        this.chr = new WeakReference<>(chr);
    }

    public static boolean isAbonormalTradeStatus(MapleTrade local, MapleTrade partner) {
        if (local != null && partner != null) {
            return local.isCanceling() || partner.isCanceling() || local.isCompleting() || partner.isCompleting();
        } else if (local != null && partner == null) {
            return local.isCanceling() || local.isCompleting();
        } else if (local == null && partner != null) {
            return partner.isCanceling() || partner.isCompleting();
        } else if (local == null && partner == null) {
            return true;
        }
        return true;
    }

    public boolean isCompleting() {
        return completing;
    }

    public void setCompleting(boolean set) {
        canceling = set;
    }

    public boolean isCanceling() {
        return canceling;
    }

    public void setCanceling(boolean set) {
        canceling = set;
    }

    public final void CompleteTrade() {
        final int mesos = exchangeMeso;
        String Items = "";

        if (exchangeItems != null) { // just to be on the safe side...
            for (final IItem item : exchangeItems) {
                byte flag = item.getFlag();

                if (ItemFlag.KARMA_EQ.check(flag)) {
                    item.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    item.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                }
                Items += item.getItemId() + "(" + item.getItemName() + ")x" + item.getQuantity() + " 唯一ID:" + item.getEquipOnlyId() + ", ";
                MapleInventoryManipulator.addFromDrop(chr.get().getClient(), item, false);
            }
            exchangeItems.clear();
        }
        if (exchangeMeso > 0) {
            chr.get().gainMeso(exchangeMeso - GameConstants.getTaxAmount(exchangeMeso), false, true, false);
        }
        exchangeMeso = 0;

        chr.get().getClient().sendPacket(MaplePacketCreator.TradeMessage(tradingslot, (byte) 0x07));
        chr.get().saveToDB(false, false);
        chr.get().addTradeMsg(chr.get().getName(), " 交易道具,收到: " + Items + "\r\n");
        chr.get().addTradeMsg(chr.get().getName(), " 交易楓幣,收到 " + mesos + "\r\n");
        chr.get().addTradeMsg(chr.get().getName(), " 交易結束,結果: 交易成功\r\n");
    }

    public final void cancel(final MapleClient c) {
        cancel(c, 0);
    }

    public final void cancel(final MapleClient c, final int unsuccessful) {
        final int mesos = meso;
        String Items = "";
        if (items != null) { // just to be on the safe side...
            for (final IItem item : items) {
                Items += item.getItemId() + "(" + item.getItemName() + ")x" + item.getQuantity() + " 唯一ID:" + item.getEquipOnlyId() + ", ";
                MapleInventoryManipulator.addFromDrop(c, item, false);
            }
            items.clear();
        }
        if (meso > 0) {
            c.getPlayer().gainMeso(meso, false, true, false);
        }
        meso = 0;
        if (c != null) {
            c.sendPacket(MaplePacketCreator.getTradeCancel(tradingslot, unsuccessful));
        }
        if (c != null && c.getPlayer() != null) {
            chr.get().addTradeMsg(chr.get().getName(), " 交易道具,收到: " + Items + "\r\n");
            chr.get().addTradeMsg(chr.get().getName(), " 交易楓幣,收到 " + mesos + "\r\n");
            chr.get().addTradeMsg(chr.get().getName(), " 交易結束,結果: 交易失敗\r\n");
        }

    }

    public final boolean isLocked() {
        return locked;
    }

    public final void setMeso(final int meso) {
        if (locked || partner == null || meso <= 0 || this.meso + meso <= 0) {
            return;
        }
        if (chr.get().getMeso() >= meso) {
            chr.get().addTradeMsg(chr.get().getName(), " 設定楓幣[" + meso + "]\r\n");
            chr.get().gainMeso(-meso, false, true, false);
            this.meso += meso;
            chr.get().getClient().sendPacket(MaplePacketCreator.getTradeMesoSet((byte) 0, this.meso));
            if (partner != null) {
                partner.getChr().getClient().sendPacket(MaplePacketCreator.getTradeMesoSet((byte) 1, this.meso));
            }
        }
    }

    public final void addItem(final IItem item) {
        if (locked || partner == null) {
            return;
        }
        chr.get().addTradeMsg(chr.get().getName(), " 設定道具[" + item.getItemId() + "]數量[" + item.getQuantity() + "]\r\n");
        items.add(item);
        chr.get().getClient().sendPacket(MaplePacketCreator.getTradeItemAdd((byte) 0, item));
        if (partner != null) {
            partner.getChr().getClient().sendPacket(MaplePacketCreator.getTradeItemAdd((byte) 1, item));
        }
    }

    public final void chat(final String message) {
        if (partner == null) {
            return;
        }
        //if (!CommandProcessor.processCommand(chr.get().getClient(), message, CommandType.TRADE)) {
        if (chr.get().getCanTalk()) {
            chr.get().dropMessage(-2, chr.get().getName() + " : " + message);
        }
        if (ServerConfig.LOG_CHAT) {
            try {
                FileoutputUtil.logToFile("logs/聊天/交易聊天.txt", " " + FileoutputUtil.NowTime() + " IP: " + chr.get().getClient().getSession().remoteAddress().toString().split(":")[0] + " 『" + chr.get().getName() + "』對『" + partner.getChr().getName() + "』的交易聊天：  " + message + "\r\n");
            } catch (Exception ex) {

            }
        }
        final StringBuilder sb = new StringBuilder("[GM 密語] 『" + chr.get().getName() + "』對『" + partner.getChr().getName() + "』的交易聊天：  " + message);
        for (ChannelServer cserv : LoginServer.getWorldStatic(chr.get().getWorld()).getChannels()) {
            for (MapleCharacter chr_ : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                if (chr_.getmsg_Chat()) {
                    chr_.dropMessage(sb.toString());
                }
            }
        }
        if (partner != null) {
            if (chr.get().getCanTalk()) {
                partner.getChr().getClient().sendPacket(PlayerShopPacket.shopChat(chr.get().getName() + " : " + message, 1));
            }
        }
        //}
    }

    public final MapleTrade getPartner() {
        return partner;
    }

    public final void setPartner(final MapleTrade partner) {
        if (locked) {
            return;
        }
        if (this.items != null) {
            this.items.clear();
        }
        if (this.exchangeItems != null) {
            this.exchangeItems.clear();
        }
        this.meso = 0;
        this.exchangeMeso = 0;
        this.partner = partner;
    }

    public final MapleCharacter getChr() {
        return chr.get();
    }

    public final int getNextTargetSlot() {
        if (items.size() >= 9) {
            return -1;
        }
        int ret = 1; //first slot
        for (IItem item : items) {
            if (item.getPosition() == ret) {
                ret++;
            }
        }
        return ret;
    }

    public final boolean setItems(final MapleClient c, final IItem item, byte targetSlot, final int quantity) {
        int target = getNextTargetSlot();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (target == -1 || GameConstants.isPet(item.getItemId()) || isLocked() || (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.CASH && quantity != 1) || (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP && quantity != 1)) {
            return false;
        }
        final byte flag = item.getFlag();
        if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return false;
        }
        if (ii.isDropRestricted(item.getItemId()) || ii.isAccountShared(item.getItemId())) {
            if (!(ItemFlag.KARMA_EQ.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return false;
            }
        }
        IItem tradeItem = item.copy();
        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            tradeItem.setQuantity(item.getQuantity());
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(item.getItemId()), item.getPosition(), item.getQuantity(), true);
        } else {
            tradeItem.setQuantity((short) quantity);
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(item.getItemId()), item.getPosition(), (short) quantity, true);
        }
        if (targetSlot < 0) {
            targetSlot = (byte) target;
        } else {
            for (IItem itemz : items) {
                if (itemz.getPosition() == targetSlot) {
                    targetSlot = (byte) target;
                    break;
                }
            }
        }
        tradeItem.setPosition(targetSlot);
        addItem(tradeItem);
        return true;
    }

    private int check() { //0 = fine, 1 = invent space not, 2 = pickupRestricted
        if (chr.get() == null) {
            return 2;
        }
        if (chr.get().getMeso() + exchangeMeso < 0) {
            return 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (final IItem item : exchangeItems) {
            switch (GameConstants.getInventoryType(item.getItemId())) {
                case EQUIP:
                    eq++;
                    break;
                case USE:
                    use++;
                    break;
                case SETUP:
                    setup++;
                    break;
                case ETC:
                    etc++;
                    break;
                case CASH: // Not allowed, probably hacking
                    cash++;
                    break;
            }
            if (ii.isPickupRestricted(item.getItemId()) && chr.get().getInventory(GameConstants.getInventoryType(item.getItemId())).findById(item.getItemId()) != null) {
                return 2;
            } else if (ii.isPickupRestricted(item.getItemId()) && chr.get().haveItem(item.getItemId(), 1, true, true)) {
                return 2;
            }
        }
        if (chr.get().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.get().getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.get().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.get().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.get().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
            return 1;
        }
        return 0;
    }

    public final static void completeTrade(final MapleCharacter c) {

        final MapleTrade local = c.getTrade();
        final MapleTrade partner = local.getPartner();

        if (partner == null || local.locked) {
            return;
        }
        if (c.canTrade() && partner.getChr().canTrade()) {
            local.locked = true; // Locking the trade
            partner.getChr().getClient().sendPacket(MaplePacketCreator.getTradeConfirmation());

            partner.exchangeItems = local.items; // Copy this to partner's trade since it's alreadt accepted
            partner.exchangeMeso = local.meso; // Copy this to partner's trade since it's alreadt accepted

            if (partner.isLocked()) { // Both locked
                if (!isAbonormalTradeStatus(local, partner)) {
                    int lz = local.check(), lz2 = partner.check();
                    if (lz == 0 && lz2 == 0) {
                        local.setCompleting(true);
                        partner.setCompleting(true);
                        local.CompleteTrade();
                        partner.CompleteTrade();
                    } else {
                        local.setCanceling(true);
                        partner.setCanceling(true);
                        partner.cancel(partner.getChr().getClient(), lz == 0 ? lz2 : lz);
                        local.cancel(c.getClient(), lz == 0 ? lz2 : lz);
                    }
                    local.getChr().addTradeMsg(local.getChr().getName(), " 確認操作 與" + partner.getChr().getName() + "(" + partner.getChr().getClient().getSessionIPAddress() + ")的交易 結果: " + ((lz + lz2 == 0) ? "成功" : "失敗") + "\r\n");
                    local.getChr().addEmptyTradeMsg("========================================\r\n");
                    partner.getChr().addTradeMsg(partner.getChr().getName(), " 確認操作 與" + local.getChr().getName() + "(" + local.getChr().getClient().getSessionIPAddress() + ")的交易 結果: " + ((lz + lz2 == 0) ? "成功" : "失敗") + "\r\n");
                    partner.getChr().addEmptyTradeMsg("========================================\r\n");
                    local.getChr().endTradeMsg(local.getChr().getName());
                    partner.getChr().endTradeMsg(partner.getChr().getName());
                    partner.getChr().setTrade(null);
                    c.setTrade(null);
                    local.setCanceling(false);
                    partner.setCanceling(false);
                    local.setCompleting(false);
                    partner.setCompleting(false);
                    if (local.getChr().getClient().getAccID() == partner.getChr().getClient().getAccID()) {
                        local.getChr().ban("修改數據包 - 同帳號角色交易", true, true, false);
                        partner.getChr().ban("修改數據包 - 同帳號角色交易", true, true, false);
                        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封號系統]" + local.getChr().getName() + " 因為使用非法軟件而被永久封號。"));
                        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封號系統]" + partner.getChr().getName() + " 因為使用非法軟件而被永久封號。"));
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語]" + partner.getChr().getName() + "和" + local.getChr().getName() + " 為同帳號的角色且進行交易。"));
                        FileoutputUtil.logToFile("logs/Hack/ban/交易異常.txt", "時間: " + FileoutputUtil.NowTime() + " IP: " + local.getChr().getClient().getSessionIPAddress() + " MAC: " + local.getChr().getNowMacs() + " " + local.getChr().getName() + " 和 " + partner.getChr().getName() + " 為同個帳號的角色且進行交易\r\n");
                        local.getChr().getClient().getSession().close();
                        partner.getChr().getClient().getSession().close();
                        return;
                    }
                }
            }
        } else {
            //  FileoutputUtil.logToFile("logs/Hack/交易複製.txt", "時間: " + FileoutputUtil.NowTime() + " 名稱[" + local.getChr().getName() + "](編號" + local.getChr().getId() + ")<帳號" + local.getChr().getAccountID() + "> 和 [" + partner.getChr().getName() + "](編號" + partner.getChr().getId() + ")<帳號" + partner.getChr().getAccountID() + "> 交易疑似複製， 地圖:" + local.getChr().getMapId() + "\r\n ");
        }
    }

    public static final void cancelTrade(final MapleTrade Localtrade, final MapleClient c) {
        final MapleTrade partner = Localtrade.getPartner();

        if (!isAbonormalTradeStatus(Localtrade, partner)) {
            Localtrade.setCanceling(true);
            if (partner != null) {
                partner.setCanceling(true);
            }
            Localtrade.cancel(c);

            if (partner != null && partner.getChr() != null) {
                Localtrade.getChr().addTradeMsg(Localtrade.getChr().getName(), " 確認操作 與" + partner.getChr().getName() + "(" + partner.getChr().getClient().getSessionIPAddress() + ")的交易 結果: 取消\r\n");
                Localtrade.getChr().addEmptyTradeMsg("========================================\r\n");
                partner.getChr().addTradeMsg(partner.getChr().getName(), " 確認操作 與" + Localtrade.getChr().getName() + "(" + Localtrade.getChr().getClient().getSessionIPAddress() + ")的交易 結果: 取消\r\n");
                partner.getChr().addEmptyTradeMsg("========================================\r\n");
                Localtrade.getChr().endTradeMsg(Localtrade.getChr().getName());
                partner.getChr().endTradeMsg(partner.getChr().getName());

                partner.cancel(partner.getChr().getClient());
                partner.setCanceling(false);
                partner.getChr().setTrade(null);
            }
            Localtrade.setCanceling(false);

            if (Localtrade.chr.get() != null) {
                Localtrade.chr.get().setTrade(null);
            }
        }
    }

    public static final void startTrade(final MapleCharacter c) {
        if (c.getTrade() == null) {
            c.setTrade(new MapleTrade((byte) 0, c));
            c.getClient().sendPacket(MaplePacketCreator.getTradeStart(c.getClient(), c.getTrade(), (byte) 0));
        } else {
            c.getClient().sendPacket(MaplePacketCreator.getErrorNotice("您目前已經在交易了"));
        }
    }

    public static final void inviteTrade(final MapleCharacter c1, final MapleCharacter c2) {
        // c1 提出邀請交易來源
        // c2 接收邀請交易對象
        if (c1 == null || c1.getTrade() == null || c2 == null) {
            return;
        }
        if (WorldConstants.isShutDown) {
            cancelTrade(c1.getTrade(), c1.getClient());
            c1.dropMessage(1, "伺服器即將關閉，無法執行本操作。");
            c1.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        } else if (c1.getWorld() != c2.getWorld()) {
            cancelTrade(c1.getTrade(), c1.getClient());
            c1.dropMessage(1, "不同世界無法交易");
            c1.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        } else if (c2.isOperateStorage() || !c2.isAlive()) {
            cancelTrade(c1.getTrade(), c1.getClient());
            c1.getClient().sendPacket(MaplePacketCreator.getErrorNotice("對方正在忙碌中。"));
            c1.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        } else if (c1.isOperateStorage() || !c1.isAlive()) {
            cancelTrade(c1.getTrade(), c1.getClient());
            c1.dropMessage(1, "目前狀態無法交易");
            c1.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        } else if (c2.getPlayerShop() != null) {
            cancelTrade(c1.getTrade(), c1.getClient());
            c1.dropMessage(1, "對方目前狀態無法交易");
            c1.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        if (c1.getTrade().getPartner() != null) {
            c1.dropMessage(1, "目前狀態無法送出邀請");
            return;
        }
        if (c2.getTrade() == null && c1.getTrade().getPartner() == null) {
            c1.addTradeMsg(c1.getName(), " 與" + c2.getName() + "交易開始\r\n");
            c2.addTradeMsg(c2.getName(), " 與" + c1.getName() + "交易開始\r\n");
            c2.setTrade(new MapleTrade((byte) 1, c2));
            c2.getTrade().setPartner(c1.getTrade());
            c1.getTrade().setPartner(c2.getTrade());
            c2.setInvited(true);
            c2.getClient().sendPacket(MaplePacketCreator.getTradeInvite(c1));
        } else {
            c1.getClient().sendPacket(MaplePacketCreator.getErrorNotice("另一位玩家正在交易中"));
            cancelTrade(c1.getTrade(), c1.getClient());
        }
    }

    public static final void visitTrade(final MapleCharacter c1, final MapleCharacter c2) {
        if (c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
            // We don't need to check for map here as the user is found via MapleMap.getCharacterById()
            c2.getClient().sendPacket(MaplePacketCreator.getTradePartnerAdd(c1));
            c1.getClient().sendPacket(MaplePacketCreator.getTradeStart(c1.getClient(), c1.getTrade(), (byte) 1));
//            c1.dropMessage(-2, "System : Use @tradehelp to see the list of trading commands");
//            c2.dropMessage(-2, "System : Use @tradehelp to see the list of trading commands");
        } else {
            c1.getClient().sendPacket(MaplePacketCreator.getErrorNotice("交易已經被關閉."));
        }
    }

    public static final void declineTrade(final MapleCharacter c) {
        final MapleTrade trade = c.getTrade();
        MapleCharacter other = null;
        if (trade != null) {
            if (trade.getPartner() != null) {
                other = trade.getPartner().getChr();
                if (other != null) {
                    other.setInvited(false);
                    if (other.getTrade() != null) {
                        other.getTrade().cancel(other.getClient());
                        other.setTrade(null);
                    }
                    other.dropMessage(5, c.getName() + " 拒絕了你的邀請");
                }
            }
            if (other != null) {
                trade.cancel(c.getClient());
            }
            c.setTrade(null);
        }
    }
}
