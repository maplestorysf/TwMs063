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
                c.getPlayer().dropMessage(5, "????????????????????????");
            }
        } else {
            c.getPlayer().dropMessage(5, "????????????????????????????????????????????????");
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
                        c.getPlayer().dropMessage(5, "????????????????????????????????????????????????");
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
                        c.getPlayer().dropMessage(5, "????????????????????????");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "????????????????????????????????????????????????");
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
                            c.getPlayer().dropMessage(5, "???????????????????????????10???????????????????????????????????????");
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

        ?????????(0x1),
        ?????????(0x2),
        ????????????(0x4),
        ??????(0x8),
        ?????????(0x10),
        ??????(0x20),
        ?????????(0x40),
        ????????????(0x80),
        ??????(0x100),
        ?????????_??????(0x200),
        ?????????_??????(0x400),
        ??????(0x800),
        ????????????(0x1000),
        ??????(0x2000),
        ?????????(0x4000),
        ?????????(0x8000),
        ????????????(0x10000),
        ??????(0x20000),
        ?????????(0x40000),
        ??????(0x80000),
        ????????????(0x100000),
        ?????????(0x200000),
        ??????(0x400000),
        ?????????(0x800000),
        ????????????(0x1000000);

        private int code;

        private PartySearchJob(int code) {
            this.code = code;
        }

        public final boolean check(int mask) {
            return (mask & code) == code;
        }

        public static boolean checkJob(int mask, int job) {
            return ?????????.check(mask)
                    || (?????????.check(mask) && MapleJob.is?????????(job) && !MapleJob.is????????????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job))
                    || (??????.check(mask) && MapleJob.is??????(job) && !MapleJob.is????????????(job))
                    || (?????????.check(mask) && MapleJob.is??????(job))
                    || (??????.check(mask) && MapleJob.is?????????(job))
                    || (??????.check(mask) && MapleJob.is?????????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job))
                    || (??????.check(mask) && MapleJob.is??????(job))
                    || (?????????_??????.check(mask) && MapleJob.is????????????_??????(job))
                    || (?????????_??????.check(mask) && MapleJob.is????????????_??????(job))
                    || (??????.check(mask) && MapleJob.is??????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job))
                    || (??????.check(mask) && MapleJob.is??????(job))
                    || (?????????.check(mask) && MapleJob.is??????(job))
                    || (?????????.check(mask) && MapleJob.is??????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job))
                    || (??????.check(mask) && MapleJob.is??????(job))
                    || (?????????.check(mask) && MapleJob.is?????????(job))
                    || (??????.check(mask) && MapleJob.is????????????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job))
                    || (?????????.check(mask) && MapleJob.is?????????(job))
                    || (??????.check(mask) && MapleJob.is??????(job))
                    || (?????????.check(mask) && MapleJob.is?????????(job))
                    || (????????????.check(mask) && MapleJob.is????????????(job));
        }
    }

    public static final void PartySearchStart(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        MapleParty party = chr.getParty();
        if (party == null || party.getLeader().getId() != chr.getId()) {
            chr.dropMessage(1, "???????????????????????????");
            return;
        }

        int minLevel = slea.readInt();
        int maxLevel = slea.readInt();
        int memberNum = slea.readInt();
        int jobMask = slea.readInt();

        if (minLevel > maxLevel) {
            chr.dropMessage(1, "????????????????????????????????????????????????????????????");
            return;
        }
        if (minLevel < 0) {
            chr.dropMessage(1, "???????????????");
            return;
        }
        if (maxLevel > 200) {
            chr.dropMessage(1, "?????????????????????????????????200??????");
            return;
        }
        if (maxLevel - minLevel > 30) {
            chr.dropMessage(1, "??????????????????????????????30??????");
            return;
        }
        if (minLevel > chr.getLevel()) {
            chr.dropMessage(1, "???????????????????????????????????????????????????????????????");
            return;
        }
        if (memberNum < 2 || memberNum > 6) {
            chr.dropMessage(1, "?????????????????????2~6??????");
            return;
        }
        if (party.getMembers().size() >= memberNum) {
            chr.dropMessage(1, "???????????????" + memberNum + "?????????");
            return;
        }
        if (jobMask == 0) {
            chr.dropMessage(1, "?????????????????????????????????????????????");
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
