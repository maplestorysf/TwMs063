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

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

public class PartyHandler {

    public static final void DenyPartyRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        final int partyid = slea.readInt();
        if (c.getPlayer().getParty() == null) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null) {
                if (action == 0x1B) { //accept
                    if (party.getMembers().size() < 6) {
                        World.Party.updateParty(partyid, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                        c.getPlayer().receivePartyMemberHP();
                        c.getPlayer().updatePartyMemberHP();
                    } else {
                        c.sendPacket(MaplePacketCreator.partyStatusMessage(17));
                    }
                } else if (action != 0x16) {
                    final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
                    if (cfrom != null) {
                        cfrom.getClient().sendPacket(MaplePacketCreator.partyStatusMessage(22, c.getPlayer().getName()));
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "已拒絕組隊邀請。");
            }
        } else {
            c.getPlayer().dropMessage(5, "不能加入組隊，因為已經有組隊了。");
        }

    }

    public static final void PartyOperatopn(final LittleEndianAccessor slea, final MapleClient c) {
        final int operation = slea.readByte();
        MapleParty party = c.getPlayer().getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());

        switch (operation) {
            case 1: // create
                if (c.getPlayer().getParty() == null) {
                  //  if (!MapleJob.isBeginner(c.getPlayer().getJob())) {
                        party = World.Party.createParty(partyplayer);
                        c.getPlayer().setParty(party);
                        c.sendPacket(MaplePacketCreator.partyCreated(party.getId()));
                   // } else {
                    //    c.sendPacket(MaplePacketCreator.partyStatusMessage(10));
                    //}
                } else {
                    if (partyplayer.equals(party.getLeader()) && party.getMembers().size() == 1) { //only one, reupdate
                        c.sendPacket(MaplePacketCreator.partyCreated(party.getId()));
                    } else {
                        c.getPlayer().dropMessage(5, "不能創建組隊，因為已經有組隊了。");
                    }
                }
                break;
            case 2: // leave
                if (party != null) { //are we in a party? o.O"
                    if (partyplayer.equals(party.getLeader())) { // disband
                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                        if (c.getPlayer().getPyramidSubway() != null) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                    } else {
                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                        }
                        if (c.getPlayer().getPyramidSubway() != null) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                    }
                    c.getPlayer().setParty(null);
                }
                break;
            case 3: // accept invitation
                final int partyid = slea.readInt();
                if (c.getPlayer().getParty() == null) {
                    party = World.Party.getParty(partyid);
                    if (party != null) {
                        if (party.getMembers().size() < 6) {
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.sendPacket(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "已拒絕組隊邀請。");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "不能加入組隊，因為已經有組隊了。");
                }
                break;
            case 4: // invite
                // TODO store pending invitations and check against them
                final MapleCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (invited != null) {
                    if (invited.getParty() == null && party != null) {
                        if (invited.getLevel() > 10 || invited.getJob() == 200) {
                            if (party.getMembers().size() < 6) {
                                invited.getClient().sendPacket(MaplePacketCreator.partyInvite(c.getPlayer(), false));
                            } else {
                                c.sendPacket(MaplePacketCreator.partyStatusMessage(17));
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "由於對方的等級低於10級以下，所以不能邀請對方。");
                        }
                    } else {
                        c.sendPacket(MaplePacketCreator.partyStatusMessage(16));
                    }
                } else {
                    c.sendPacket(MaplePacketCreator.partyStatusMessage(18));
                }
                break;
            case 5: // expel
                if (partyplayer != null && partyplayer.equals(party.getLeader())) {
                    final MaplePartyCharacter expelled = party.getMemberById(slea.readInt());
                    if (expelled != null) {
                        World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                        if (c.getPlayer().getEventInstance() != null) {
                            /*if leader wants to boot someone, then the whole party gets expelled
                             TODO: Find an easier way to get the character behind a MaplePartyCharacter
                             possibly remove just the expellee.*/
                            if (expelled.isOnline()) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        }
                        if (c.getPlayer().getPyramidSubway() != null && expelled.isOnline()) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                    }
                }
                break;
            case 6: // change leader
                if (party != null) {
                    final MaplePartyCharacter newleader = party.getMemberById(slea.readInt());
                    if (newleader != null && partyplayer.equals(party.getLeader())) {
                        World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                    }
                }
                break;
            default:
                System.err.println("Unhandled Party function." + operation);
                break;
        }
    }

    public static enum PartySearchJob {

        全職業(0x1),
        初心者(0x2),
        狂狼勇士(0x4),
        劍士(0x8),
        十字軍(0x10),
        騎士(0x20),
        龍騎士(0x40),
        聖魂劍士(0x80),
        法師(0x100),
        魔導士_火毒(0x200),
        魔導士_冰雷(0x400),
        祭司(0x800),
        烈焰巫師(0x1000),
        海盜(0x2000),
        格鬥家(0x4000),
        神槍手(0x8000),
        閃雷悍將(0x10000),
        盜賊(0x20000),
        暗殺者(0x40000),
        神偷(0x80000),
        暗夜行者(0x100000),
        弓箭手(0x200000),
        游俠(0x400000),
        狙擊手(0x800000),
        破風使者(0x1000000);

        private int code;

        private PartySearchJob(int code) {
            this.code = code;
        }

        public final boolean check(int mask) {
            return (mask & code) == code;
        }

        public static boolean checkJob(int mask, int job) {
            return 全職業.check(mask)
                    || (初心者.check(mask) && MapleJob.is初心者(job) && !MapleJob.is狂狼勇士(job))
                    || (狂狼勇士.check(mask) && MapleJob.is狂狼勇士(job))
                    || (劍士.check(mask) && MapleJob.is劍士(job) && !MapleJob.is狂狼勇士(job))
                    || (十字軍.check(mask) && MapleJob.is英雄(job))
                    || (騎士.check(mask) && MapleJob.is聖騎士(job))
                    || (騎士.check(mask) && MapleJob.is黑騎士(job))
                    || (聖魂劍士.check(mask) && MapleJob.is聖魂劍士(job))
                    || (法師.check(mask) && MapleJob.is法師(job))
                    || (魔導士_火毒.check(mask) && MapleJob.is大魔導士_火毒(job))
                    || (魔導士_冰雷.check(mask) && MapleJob.is大魔導士_冰雷(job))
                    || (祭司.check(mask) && MapleJob.is主教(job))
                    || (烈焰巫師.check(mask) && MapleJob.is烈焰巫師(job))
                    || (海盜.check(mask) && MapleJob.is海盜(job))
                    || (格鬥家.check(mask) && MapleJob.is拳霸(job))
                    || (神槍手.check(mask) && MapleJob.is槍神(job))
                    || (閃雷悍將.check(mask) && MapleJob.is閃雷悍將(job))
                    || (盜賊.check(mask) && MapleJob.is盜賊(job))
                    || (暗殺者.check(mask) && MapleJob.is夜使者(job))
                    || (神偷.check(mask) && MapleJob.is暗影神偷(job))
                    || (暗夜行者.check(mask) && MapleJob.is暗夜行者(job))
                    || (弓箭手.check(mask) && MapleJob.is弓箭手(job))
                    || (游俠.check(mask) && MapleJob.is箭神(job))
                    || (狙擊手.check(mask) && MapleJob.is神射手(job))
                    || (破風使者.check(mask) && MapleJob.is破風使者(job));
        }
    }

    public static final void PartySearchStart(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        MapleParty party = chr.getParty();
        if (party == null || party.getLeader().getId() != chr.getId()) {
            chr.dropMessage(1, "您並非隊伍的隊長！");
            return;
        }

        int minLevel = slea.readInt();
        int maxLevel = slea.readInt();
        int memberNum = slea.readInt();
        int jobMask = slea.readInt();

        if (minLevel > maxLevel) {
            chr.dropMessage(1, "搜尋等級範圍的下限高出上限！請重新確認！");
            return;
        }
        if (minLevel < 0) {
            chr.dropMessage(1, "等級異常！");
            return;
        }
        if (maxLevel > 200) {
            chr.dropMessage(1, "目前楓之谷的等級上限為200級！");
            return;
        }
        if (maxLevel - minLevel > 30) {
            chr.dropMessage(1, "等級範圍最多可設定到30級！");
            return;
        }
        if (minLevel > chr.getLevel()) {
            chr.dropMessage(1, "所要搜尋的等級範圍中，必須包含自己的等級。");
            return;
        }
        if (memberNum < 2 || memberNum > 6) {
            chr.dropMessage(1, "隊員最多輸入到2~6人！");
            return;
        }
        if (party.getMembers().size() >= memberNum) {
            chr.dropMessage(1, "隊員已達到" + memberNum + "人以上");
            return;
        }
        if (jobMask == 0) {
            chr.dropMessage(1, "請選擇想要組成隊伍的角色職業！");
            return;
        }

        World.PartySearch.startSearch(chr, minLevel, maxLevel, memberNum, jobMask);
    }

    public static final void PartySearchStop(final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        World.PartySearch.stopSearch(chr);
    }
}
