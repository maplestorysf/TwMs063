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

import client.inventory.IItem;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.MapleStat;
import client.inventory.ItemFlag;
import client.inventory.ModifyInventory;
import constants.GameConstants;
import handling.world.MapleAntiMacro;
import handling.world.World;
import scripting.ReactorScriptManager;
import server.events.MapleCoconut;
import server.events.MapleCoconut.MapleCoconuts;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.events.MapleEventType;
import server.maps.MapleDoor;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import tools.ArrayMap;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

public class PlayersHandler {

    public static void Note(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final byte type = slea.readByte();

        switch (type) {
            case 0:
                String name = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                boolean fame = slea.readByte() > 0;
//                slea.readInt(); //0?
//                IItem itemz = chr.getCashInventory().findByCashId((int) slea.readLong());
//                if (itemz == null || !itemz.getGiftFrom().equalsIgnoreCase(name) || !chr.getCashInventory().canSendNote(itemz.getUniqueId())) {
//                    return;
//                }
                try {
                    chr.sendNote(name, msg, fame ? 1 : 0);
//                    chr.getCashInventory().sendedNote(itemz.getUniqueId());
                } catch (Exception e) {

                }
                break;
            case 1:
                final byte num = slea.readByte();
                slea.skip(2);

                for (int i = 0; i < num; i++) {
                    final int id = slea.readInt();
                    chr.deleteNote(id, slea.readByte() > 0 ? 1 : 0);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void GiveFame(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int who = slea.readInt();
        final int mode = slea.readByte();

        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = (MapleCharacter) chr.getMap().getMapObject(who, MapleMapObjectType.PLAYER);

        if (target != null) {
            if (target.getId() == chr.getId()) { // faming self
                FileoutputUtil.logToFile("logs/Hack/Ban/修改封包.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.getName() + "(" + chr.getId() + ") 修改名聲封包，使用時封鎖。加自己名聲");
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封鎖系統] " + chr.getName() + " 因為修改封包而被管理員永久停權。"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語]  " + chr.getName() + "(" + chr.getId() + ") 修改名聲封包，使用時封鎖。加自己名聲"));
                chr.ban("修改封包", true, true, false);
                chr.getClient().getSession().close();
                return;
            } else if (chr.getLevel() < 15) {
                FileoutputUtil.logToFile("logs/Hack/Ban/修改封包.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.getName() + "(" + chr.getId() + ")(等級:" + chr.getLevel() + ") 修改名聲封包，使用時封鎖。十五等以下加名聲");
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封鎖系統] " + chr.getName() + " 因為修改封包而被管理員永久停權。"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語]  " + chr.getName() + "(" + chr.getId() + ")(等級:" + chr.getLevel() + ") 修改名聲封包，使用時封鎖。十五等以下加名聲"));
                chr.ban("修改封包", true, true, false);
                chr.getClient().getSession().close();
                return;
            }
            switch (chr.canGiveFame(target)) {
                case OK:
                    if (Math.abs(target.getFame() + famechange) <= 30000) {
                        target.addFame(famechange);
                        target.updateSingleStat(MapleStat.FAME, target.getFame());
                    }
                    if (!chr.isGM()) {
                        chr.hasGivenFame(target);
                    }
                    c.sendPacket(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
                    target.getClient().sendPacket(MaplePacketCreator.receiveFame(mode, chr.getName()));
                    break;
                case NOT_TODAY:
                    c.sendPacket(MaplePacketCreator.giveFameErrorResponse(3));
                    break;
                case NOT_THIS_MONTH:
                    c.sendPacket(MaplePacketCreator.giveFameErrorResponse(4));
                    break;
            }
        }
    }

    public static void UseDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final boolean mode = slea.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (MapleMapObject obj : chr.getMap().getAllDoorsThreadsafe()) {
            final MapleDoor door = (MapleDoor) obj;
            if (door.getOwnerId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void TransformPlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        // D9 A4 FD 00
        // 11 00
        // A0 C0 21 00
        // 07 00 64 66 62 64 66 62 64
        if (slea.available() > 11) {
            chr.updateTick(slea.readInt());
            final byte slot = (byte) slea.readShort();
            final int itemId = slea.readInt();
            final String target = slea.readMapleAsciiString().toLowerCase();

            final MapleInventoryType type = GameConstants.getInventoryType(itemId);
            IItem toUse = null;
            if (type != null && chr.getInventory(type) != null) {
                toUse = chr.getInventory(type).findById(itemId);
            }
            if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
                c.getSession().writeAndFlush(MaplePacketCreator.enableActions());
                return;
            }
            switch (itemId) {
                case 2212000:
                    for (final MapleCharacter search_chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                        if (search_chr.getName().toLowerCase().equals(target)) {
                            MapleItemInformationProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                            search_chr.dropMessage(6, chr.getName() + " has played a prank on you!");
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                        }
                    }
                    break;
            }
        }
    }

    public static void HitReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final int charPos = slea.readInt();
        final short stance = slea.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
//        double range = c.getPlayer().getPosition().distanceSq(reactor.getPosition());
//        if (range > 0) {
//            c.getPlayer().dropMessage("反應堆: " + reactor.getReactorId() + " 與角色距離: " + (long) range);
//        }

        reactor.hitReactor(charPos, stance, c);
    }

    public static void TouchReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final boolean touched = slea.readByte() > 0;
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (!touched || reactor == null || !reactor.isAlive() || reactor.getReactorId() < 6109013 || reactor.getReactorId() > 6109027) {
            return;
        }
        ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play
    }

    public static void hitCoconut(LittleEndianAccessor slea, MapleClient c) {
        /*CB 00 A6 00 06 01
         * A6 00 = coconut id
         * 06 01 = ?
         */
        int id = slea.readShort();
        String co = "農夫的樂趣";
        MapleCoconut map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.打果子);
        if (map == null || !map.isRunning()) {
            map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.打瓶蓋);
            co = "可樂熊";
            if (map == null || !map.isRunning()) {
                return;
            }
        }
        //System.out.println("Coconut1");
        MapleCoconuts nut = map.getCoconut(id);
        if (nut == null || !nut.isHittable()) {
            return;
        }
        if (System.currentTimeMillis() < nut.getHitTime()) {
            return;
        }
        //System.out.println("Coconut2");
        if (nut.getHits() > 2 && Math.random() < 0.4 && !nut.isStopped()) {
            //System.out.println("Coconut3-1");
            nut.setHittable(false);
            if (Math.random() < 0.01 && map.getStopped() > 0) {
                nut.setStopped(true);
                map.stopCoconut();
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
                return;
            }
            nut.resetHits(); // For next event (without restarts)
            //System.out.println("Coconut4");
            if (Math.random() < 0.05 && map.getBombings() > 0) {
                //System.out.println("Coconut5-1");
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 2));
                map.bombCoconut();
            } else if (map.getFalling() > 0) {
                //System.out.println("Coconut5-2");
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 3));
                map.fallCoconut();
                if (c.getPlayer().getTeam() == 0) {
                    map.addMapleScore();
                    //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getErrorNotice(c.getPlayer().getName() + " of Team Maple knocks down a " + co + "."));
                } else {
                    map.addStoryScore();
                    //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getErrorNotice(c.getPlayer().getName() + " of Team Story knocks down a " + co + "."));
                }
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.coconutScore(map.getCoconutScore()));
            }
        } else {
            //System.out.println("Coconut3-2");
            nut.hit();
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
        }
    }

    public static void FollowRequest(final LittleEndianAccessor slea, final MapleClient c) {
//        MapleCharacter tt = c.getPlayer().getMap().getCharacterById(slea.readInt());
//        if (slea.readByte() > 0) {
//            //1 when changing map
//            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
//            if (tt != null && tt.getFollowId() == c.getPlayer().getId()) {
//                tt.setFollowOn(true);
//                c.getPlayer().setFollowOn(true);
//            } else {
//                c.getPlayer().checkFollow();
//            }
//            return;
//        }
//        if (slea.readByte() > 0) { //cancelling follow
//            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
//            if (tt != null && tt.getFollowId() == c.getPlayer().getId() && c.getPlayer().isFollowOn()) {
//                c.getPlayer().checkFollow();
//            }
//            return;
//        }
//        if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && c.getPlayer().getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
//            tt.setFollowId(c.getPlayer().getId());
//            tt.setFollowOn(false);
//            tt.setFollowInitiator(false);
//            c.getPlayer().setFollowOn(false);
//            c.getPlayer().setFollowInitiator(false);
//            tt.getClient().sendPacket(MaplePacketCreator.followRequest(c.getPlayer().getId()));
//        } else {
//            c.sendPacket(MaplePacketCreator.getPopupMsg("You are too far away."));
//        }
    }

    public static void FollowReply(final LittleEndianAccessor slea, final MapleClient c) {
//        if (c.getPlayer().getFollowId() > 0 && c.getPlayer().getFollowId() == slea.readInt()) {
//            MapleCharacter tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
//            if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
//                boolean accepted = slea.readByte() > 0;
//                if (accepted) {
//                    tt.setFollowId(c.getPlayer().getId());
//                    tt.setFollowOn(true);
//                    tt.setFollowInitiator(true);
//                    c.getPlayer().setFollowOn(true);
//                    c.getPlayer().setFollowInitiator(false);
//                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.followEffect(tt.getId(), c.getPlayer().getId(), null));
//                } else {
//                    c.getPlayer().setFollowId(0);
//                    tt.setFollowId(0);
//                    tt.getClient().sendPacket(MaplePacketCreator.getFollowMsg(5));
//                }
//            } else {
//                if (tt != null) {
//                    tt.setFollowId(0);
//                    c.getPlayer().setFollowId(0);
//                }
//                c.sendPacket(MaplePacketCreator.getPopupMsg("You are too far away."));
//            }
//        } else {
//            c.getPlayer().setFollowId(0);
//        }
    }

    public static void UnlockItem(final LittleEndianAccessor slea, final MapleClient c) { //封印之鎖解除鑰匙 ID:2051000
        //95 00 | 01 00 | 02 00 | 02 00
        short Itemsize = slea.readShort();
        short _type = slea.readShort();
        short slot = slea.readShort();

        final MapleInventoryType type = MapleInventoryType.getByType((byte) _type);
        if (type == null) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        final IItem item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        boolean add = false;
        final int UnlockItem = 2051000;
        java.util.Map<IItem, MapleInventoryType> eqs = new ArrayMap<>();
        if (ItemFlag.LOCK.check(item.getFlag())) {
            item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
            add = true;
            c.sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, item)));
            c.getPlayer().dropMessage(5, "已經解鎖！");
        } else if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
            item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
            add = true;
            c.sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, item)));
            c.getPlayer().dropMessage(5, "已經解鎖！");
        }
        if (add) {
            eqs.put(item, type);
            MapleInventoryManipulator.removeById(c.getPlayer().getClient(), MapleInventoryType.USE, UnlockItem, 1, false, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void Solomon(final LittleEndianAccessor slea, final MapleClient c) {
        c.sendPacket(MaplePacketCreator.enableActions());
        c.getPlayer().updateTick(slea.readInt());
        IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        if (item == null || item.getItemId() != slea.readInt() || item.getQuantity() <= 0 || c.getPlayer().getGachExp() > 0 || c.getPlayer().getLevel() > 50 || MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP() <= 0) {
            return;
        }
        c.getPlayer().setGachExp(c.getPlayer().getGachExp() + MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP());
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, item.getPosition(), (short) 1, false);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, c.getPlayer().getGachExp());
    }

    public static void GachExp(final LittleEndianAccessor slea, final MapleClient c) {
        c.sendPacket(MaplePacketCreator.enableActions());
        c.getPlayer().updateTick(slea.readInt());
        if (c.getPlayer().getGachExp() <= 0) {
            return;
        }
        c.getPlayer().gainExp(c.getPlayer().getGachExp() * GameConstants.getExpRate_Quest(c.getPlayer().getLevel()), true, true, false);
        c.getPlayer().setGachExp(0);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, 0);
    }

    public static void RingAction(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        switch (mode) {
            case 0: {
                final String name = slea.readMapleAsciiString();
                final int itemid = slea.readInt();
                final int newItemId = 1112300 + (itemid - 2240004);
                final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                int errcode = 0;
                if (c.getPlayer().getMarriageId() > 0) {
                    errcode = 0x17;
                } else if (c.getPlayer().haveItem(newItemId)) {
                    c.getPlayer().dropMessage("請先將身上的戒指丟棄唷。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                } else if (chr == null) {
                    errcode = 0x12;
                } else if (chr.getMapId() != c.getPlayer().getMapId()) {
                    errcode = 0x13;
                } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) {
                    errcode = 0x14;
                } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    errcode = 0x15;
                } else if (chr.getMarriageId() > 0 || chr.getMarriageItemId() > 0) {
                    errcode = 0x18;
                } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240004 || itemid > 2240015) {
                    errcode = 0x0D;
                } else if (chr.getId() == c.getPlayer().getId()) {
                    c.getPlayer().dropMessage(1, "無法跟自己結婚。");
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (errcode > 0) {
                    c.sendPacket(MaplePacketCreator.sendEngagement((byte) errcode, 0, null, null));
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                c.getPlayer().setMarriageItemId(itemid);
                if (chr != null) {
                    chr.getClient().sendPacket(MaplePacketCreator.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
                }
                //1112300 + (itemid - 2240004)
                break;
            }
            case 1:
                c.getPlayer().setMarriageItemId(0);
                break;
            case 2: {
                //accept/deny proposal
                final boolean accepted = slea.readByte() > 0;
                final String name = slea.readMapleAsciiString();
                final int id = slea.readInt();
                final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0) {
                    c.sendPacket(MaplePacketCreator.sendEngagement((byte) 0x1D, 0, null, null));
                    c.sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
                if (accepted) {
                    final int newItemId = 1112300 + (chr.getMarriageItemId() - 2240004);
                    if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                        c.sendPacket(MaplePacketCreator.sendEngagement((byte) 0x15, 0, null, null));
                        c.sendPacket(MaplePacketCreator.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, newItemId, (short) 1);
                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);
                    MapleInventoryManipulator.addById(chr.getClient(), newItemId, (short) 1);
                    chr.getClient().sendPacket(MaplePacketCreator.sendEngagement((byte) 0x10, newItemId, chr, c.getPlayer()));
                    chr.setMarriageId(c.getPlayer().getId());
                    c.getPlayer().setMarriageId(chr.getId());
                } else {
                    chr.getClient().sendPacket(MaplePacketCreator.sendEngagement((byte) 0x1E, 0, null, null));
                }
                c.sendPacket(MaplePacketCreator.enableActions());
                chr.setMarriageItemId(0);
                break;
            }
            case 3:
                //drop, only works for ETC
                final int itemId = slea.readInt();
                final MapleInventoryType type = GameConstants.getInventoryType(itemId);
                final IItem item = c.getPlayer().getInventory(type).findById(itemId);
                if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                    MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
                }
                break;
            default:
                break;
        }
    }

    public static void UpdateCharInfo(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (slea.available() == 0) {
            //TODO
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        int type = slea.readByte();
        switch (type) {
            case 0:
                // 角色訊息
                String charmessage = slea.readMapleAsciiString();
                c.getPlayer().setcharmessage(charmessage);
                //System.err.println("SetCharMessage");
                break;
            case 1:
                // 表情
                int expression = slea.readByte();
                if (expression >= 0) {
                    c.getPlayer().setexpression(expression);
                }
                //System.err.println("Expression"+ expression);
                break;
            case 2:
                // 生日及星座
                int blood = slea.readByte();
                int month = slea.readByte();
                int day = slea.readByte();
                int constellation = slea.readByte();
                if (blood >= 0) {
                    c.getPlayer().setblood(blood);
                }   if (month >= 0) {
                    c.getPlayer().setmonth(month);
                }   if (day >= 0) {
                    c.getPlayer().setday(day);
                }   if (constellation >= 0) {
                    c.getPlayer().setconstellation(constellation);
                }    //System.err.println("Constellation");
                break;
            default:
                break;
        }
    }

    public static void AntiMacro(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, boolean isItem) {
        if (c == null || chr == null || chr.getMap() == null) {
            return;
        }
        if (!isItem && !chr.isGM()) {
            return;
        }

        // 偵測角色可測謊狀態處理
        String toAntiChrName = slea.readMapleAsciiString();
        MapleCharacter victim = chr.getMap().getCharacterByName(toAntiChrName);
        if (victim == null || chr.getGMLevel() < victim.getGMLevel()) {
            // 找不到測謊角色
            c.sendPacket(MaplePacketCreator.AntiMacro.cantFindPlayer());
            return;
        }

        short slot = 0;
        // 使用測謊機道具處理
        if (isItem) {
            slot = slea.readShort();
            IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
            int itemId = slea.readInt();

            // 偵測使用的測謊機道具是否合理
            switch (itemId) {
                case 2190000: {
                    if (toUse.getItemId() != itemId) {
                        return;
                    }
                    break;
                }
                default: {
                    chr.dropMessage("這個測謊機道具暫時不能用,請回報給管理員。");
                    return;
                }
            }
        }

        if (MapleAntiMacro.startAnti(chr, victim, (byte) (isItem ? MapleAntiMacro.ITEM_ANTI : MapleAntiMacro.GM_SKILL_ANTI)) && isItem) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
    }

    public static void OldAntiMacroQuestion(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (c == null || chr == null) {
            return;
        }
        if (MapleAntiMacro.getCharacterState(chr) != MapleAntiMacro.ANTI_NOW) {
            return;
        }
        String inputCode = slea.readMapleAsciiString();
        if (MapleAntiMacro.verifyCode(chr.getName(), inputCode)) {
            MapleAntiMacro.antiSuccess(chr);
        } else {
            MapleAntiMacro.antiReduce(chr);
        }
    }
}
