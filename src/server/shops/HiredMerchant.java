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
package server.shops;

import java.util.concurrent.ScheduledFuture;
import client.inventory.IItem;
import client.inventory.ItemFlag;
import constants.GameConstants;
import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConfig;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Timer.EtcTimer;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;

public class HiredMerchant extends AbstractPlayerStore {

    private final long start;
    private int storeid;
    public boolean canShop = true;
    private ReentrantReadWriteLock merchantLock = new ReentrantReadWriteLock();
    private final Map<String, Integer> messages;
    private final List<String> blacklist;
    private ScheduledFuture<?> schedule;

    public HiredMerchant(MapleCharacter owner, int itemId, String desc) {
        super(owner, itemId, desc, "", 3);
        start = System.currentTimeMillis();
        blacklist = new LinkedList<>();
        messages = new HashMap<>();
        this.schedule = EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                HiredMerchant.this.removeAllVisitors(14, 0);
                closeShop(true, true);
            }
        }, 1000 * 60 * 60 * 24);
    }

    /**
     *
     * @return
     */
    @Override
    public byte getShopType() {
        return IMaplePlayerShop.HIRED_MERCHANT;
    }

    public final void setStoreId(final int storeid) {
        this.storeid = storeid;
    }

    public List<MaplePlayerShopItem> searchItem(final int itemSearch) {
        final List<MaplePlayerShopItem> itemz = new LinkedList<>();
        for (MaplePlayerShopItem item : items) {
            if (item.item.getItemId() == itemSearch && item.bundles > 0) {
                itemz.add(item);
            }
        }
        return itemz;
    }

    @Override
    public void buy(MapleClient c, int item, short quantity) {
        final MaplePlayerShopItem pItem = items.get(item);
        final IItem shopItem = pItem.item;
        final IItem newItem = shopItem.copy();
        final short perbundle = newItem.getQuantity();
        final int theQuantity = (pItem.price * quantity);
        newItem.setQuantity((short) (quantity * perbundle));

        byte flag = newItem.getFlag();

        if (ItemFlag.KARMA_EQ.check(flag)) {
            newItem.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
        } else if (ItemFlag.KARMA_USE.check(flag)) {
            newItem.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
        }

        if (!c.getPlayer().canHold(newItem.getItemId())) {
            c.getPlayer().dropMessage(1, "您的背包滿了");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        newItem.setGMLog(c.getPlayer().getName() + " Buy from  " + getOwnerName() + "'s Merchant " + newItem.getItemId() + "x" + quantity + " Prize : " + pItem.price);

        if (MapleInventoryManipulator.addFromDrop(c, newItem, false)) {
            pItem.bundles -= quantity; // Number remaining in the store
            final int gainmeso = getMeso() + (pItem.price * quantity);
            setMeso(gainmeso - GameConstants.EntrustedStoreTax(gainmeso));
            c.getPlayer().gainMeso(-pItem.price * quantity, false);
            MapleCharacter Owner = getMCOwnerWorld();
            if (Owner != null) {
                Owner.dropMessage(5, "道具 " + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + " (" + perbundle + ") × " + quantity + " 已被其他玩家購買，還剩下：" + pItem.bundles + " 個");
            }
            if (ServerConfig.LOG_MERCHANT) {
                FileoutputUtil.logToFile("logs/data/精靈商人.txt", "\r\n 時間　[" + FileoutputUtil.NowTime() + "] IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 玩家 " + c.getPlayer().getName() + " 從  " + getOwnerName() + " 的精靈商人購買了" + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + " (" + newItem.getItemId() + ") x" + quantity + " 單個價錢為 : " + pItem.price + " 道具唯一ID:" + newItem.getEquipOnlyId());
            }
            final StringBuilder sb = new StringBuilder("[GM 密語] 玩家 " + c.getPlayer().getName() + " 從  " + getOwnerName() + " 的精靈商人購買了 " + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + "(" + newItem.getItemId() + ") x" + quantity + " 單個價錢為 : " + pItem.price + " 道具唯一ID:" + newItem.getEquipOnlyId());
            for (World wl : LoginServer.getWorlds()) {
                for (ChannelServer cserv : wl.getChannels()) {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                        if (chr.getmsg_HiredMerchant()) {
                            chr.dropMessage(sb.toString());
                        }
                    }
                }
            }
        } else {
            c.getPlayer().dropMessage(1, "您的背包滿了，請檢查您的背包！");
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    @Override
    public void closeShop(boolean saveItems, boolean remove) {
        merchantLock.writeLock().lock();
        try {
            if (schedule != null) {
                schedule.cancel(false);
            }
            if (saveItems && getCanShop()) {
                saveItems();
                items.clear();
            }

            if (remove) {
                ChannelServer.getInstance(world, channel).removeMerchant(this);
                getMap().broadcastMessage(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
                MapleCharacter Owner = getMCOwnerWorld();
                if (Owner != null) {
                   Owner.dropMessage(5, "您的精靈商人已經結束營業，請找富蘭德里領取剩餘的道具和錢。");
                }
            }
            setCanShop(false);
            getMap().removeMapObject(this);
            schedule = null;
        } finally {
            merchantLock.writeLock().unlock();
        }
    }

    public int getTimeLeft() {
        return (int) ((System.currentTimeMillis() - start) / 1000);
    }

    public final int getStoreId() {
        return storeid;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        if (isAvailable()) {
            client.sendPacket(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
        }
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (isAvailable()) {
            client.sendPacket(PlayerShopPacket.spawnHiredMerchant(this));
        }
    }

    public final boolean isInBlackList(final String bl) {
        return blacklist.contains(bl);
    }

    public final void addBlackList(final String bl) {
        blacklist.add(bl);
    }

    public final void removeBlackList(final String bl) {
        blacklist.remove(bl);
    }

    public final void sendBlackList(final MapleClient c) {
        c.sendPacket(PlayerShopPacket.MerchantBlackListView(blacklist));
    }

    public final void sendVisitor(final MapleClient c) {
        try {
            c.sendPacket(PlayerShopPacket.MerchantVisitorView(visitors));
        } catch (Exception ex) {

        }
    }

    public final void addMsg(String msg, byte slot) {
        messages.put(msg, (int) slot);
    }

    public final void sendMsg(final MapleClient c) {
        messages.entrySet().stream().forEach((msg) -> {
            c.sendPacket(PlayerShopPacket.shopChat(msg.getKey(), msg.getValue()));
        });
    }

    @Override
    public final void setCanShop(boolean set) {
        canShop = set;
    }

    @Override
    public final boolean getCanShop() {
        return canShop;
    }
}
