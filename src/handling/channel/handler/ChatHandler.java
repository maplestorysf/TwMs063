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

import client.MapleClient;
import client.MapleCharacter;
import client.messages.CommandProcessor;
import constants.ServerConfig;
import constants.ServerConstants.CommandType;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.World;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import server.maps.MapleMap;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

public class ChatHandler {

    public static final void GeneralChat(final String text, final byte unk, final MapleClient c, final MapleCharacter chr) {

        if (chr != null && !CommandProcessor.processCommand(c, text, CommandType.NORMAL)) {
            if (!chr.isGM() && text.length() >= 80) {
                return;
            }

            if ((chr.getCanTalk_event() && chr.getCanTalk()) || chr.isStaff()) {
                byte[] colorChatPacket = ChatPacket(chr, text, chr.isHidden());
                // 過濾髒字
                String curseText = LoginInformationProvider.getInstance().getCurseMsg(text);
                byte[] colorCurseChatPacket = ChatPacket(chr, curseText, chr.isHidden());
                MapleMap map = chr.getMap();
                if (chr.isHidden()) {
                    chr.getCheatTracker().checkMsg();
                    if (colorChatPacket == null || !chr.isGM()) {
                        map.broadcastGMMessage(chr, MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isAdmin(), unk), true);
                    } else {
                        if (unk == 0) {
                            if (chr.isHiddenChatCanSee()) {
                                c.getSession().writeAndFlush(colorChatPacket);
                                map.broadcastMessage(chr, chr.isGM() ? colorChatPacket : colorCurseChatPacket, false);
                            } else {
                                map.broadcastGMMessage(chr, colorChatPacket, true);
                            }
                        }
                        map.broadcastGMMessage(chr, MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isAdmin(), 1), true);
                    }
                } else {
                    chr.getCheatTracker().checkMsg();
                    if (colorChatPacket == null) {
                        c.getSession().writeAndFlush(MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isAdmin(), unk));
                        map.broadcastMessage(chr, MaplePacketCreator.getChatText(chr.getId(), chr.isGM() ? text : curseText, c.getPlayer().isAdmin(), unk), false);
                    } else {
                        if (unk == 0) {
                            c.getSession().writeAndFlush(colorChatPacket);
                            map.broadcastMessage(chr, chr.isGM() ? colorChatPacket : colorCurseChatPacket, false);
                        }
                        c.getSession().writeAndFlush(MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isAdmin(), 1));
                        map.broadcastMessage(chr, MaplePacketCreator.getChatText(chr.getId(), chr.isGM() ? text : curseText, c.getPlayer().isAdmin(), 1), false);
                    }
                }

                if (chr.gmLevel() == 0 && !chr.isHidden() || chr.isGod()) {
                    if (ServerConfig.LOG_CHAT) {
                        FileoutputUtil.logToFile("logs/聊天/普通聊天.txt", "\r\n" + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 『" + chr.getName() + "』 地圖『" + chr.getMapId() + "』：  " + text);
                    }
                    final StringBuilder sb = new StringBuilder("[GM 密語]『" + chr.getName() + "』(" + chr.getId() + ")地圖『" + chr.getMapId() + "』普聊：  " + text);
                    try {
                        for (ChannelServer cserv :  LoginServer.getWorldStatic(chr.getMap().getWorld()).getChannels()) {
                            for (MapleCharacter chr_ : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                                if (chr_ == null) {
                                    break;
                                }
                                if (chr_.getmsg_Chat()) {
                                    chr_.dropMessage(sb.toString());
                                }
                            }
                        }
                    } catch (ConcurrentModificationException CME) {

                    }

                }
            } else {
                c.sendPacket(MaplePacketCreator.getItemNotice("在這個地方不能說話。"));
            }
        }
    }

    public static byte[] ChatPacket(final MapleCharacter chr, String text) {
        return ChatPacket(chr, text, false);
    }

    public static byte[] ChatPacket(final MapleCharacter chr, String text, boolean hidden) {
        boolean yellow = false;
        String rank = "";
        if (chr.isAdmin()) {
            if ((hidden && chr.isHiddenChatCanSee()) || !hidden) {
                rank = "<超級管理員> ";
            }
            if (hidden) {
                yellow = true;
            }
        } else if (chr.gmLevel() == 4) {
            if ((hidden && chr.isHiddenChatCanSee()) || !hidden) {
                rank = "<領導者> ";
            }
            if (hidden) {
                yellow = true;
            }
        } else if (chr.gmLevel() == 3) {
            if ((hidden && chr.isHiddenChatCanSee()) || !hidden) {
                rank = "<管理員> ";
            }
        } else if (chr.gmLevel() == 2) {
            if ((hidden && chr.isHiddenChatCanSee()) || !hidden) {
                rank = "<巡察員> ";
            }
        } else if (chr.gmLevel() == 1) {
            if ((hidden && chr.isHiddenChatCanSee()) || !hidden) {
                rank = "<新實習生> ";
            }
        }

        if (rank.isEmpty() && !yellow) {
            return null;
        } else {
            return MaplePacketCreator.yellowChat(rank + chr.getName() + " : " + text);
        }
    }

    public static final void Others(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int type = slea.readByte();
        final byte numRecipients = slea.readByte();
        if (numRecipients <= 0) {
            return;
        }
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        final String chattext = slea.readMapleAsciiString();
        if (chr == null || !chr.getCanTalk() || !chr.getCanTalk_event()) {
            c.sendPacket(MaplePacketCreator.getItemNotice("在這個地方不能說話。"));
            return;
        }
        if (CommandProcessor.processCommand(c, chattext, CommandType.NORMAL)) {
            return;
        }
        chr.getCheatTracker().checkMsg();
        switch (type) {
            case 0:
                if (ServerConfig.LOG_CHAT) {
                    FileoutputUtil.logToFile("logs/聊天/好友聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 好友ID: " + Arrays.toString(recipients) + "玩家: " + chr.getName() + " 說了 :" + chattext);
                }
                World.Buddy.buddyChat(recipients, chr.getId(), chr.getName(), chattext);
                break;
            case 1:
                if (chr.getParty() == null) {
                    break;
                }
                if (ServerConfig.LOG_CHAT) {
                    FileoutputUtil.logToFile("logs/聊天/隊伍聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 隊伍: " + chr.getParty().getId() + " 玩家: " + chr.getName() + " 說了 :" + chattext);
                }
                World.Party.partyChat(chr.getParty().getId(), chattext, chr.getName());
                break;
            case 2:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                if (ServerConfig.LOG_CHAT) {
                    FileoutputUtil.logToFile("logs/聊天/公會聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 公會: " + chr.getGuildId() + " 玩家: " + chr.getName() + " 說了 :" + chattext);
                }
                World.Guild.guildChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
            case 3:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                if (ServerConfig.LOG_CHAT) {
                    FileoutputUtil.logToFile("logs/聊天/聯盟聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 公會: " + chr.getGuildId() + " 玩家: " + chr.getName() + " 說了 :" + chattext);
                }
                World.Alliance.allianceChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
        }
    }

    public static final void Messenger(final LittleEndianAccessor slea, final MapleClient c) {
        String input;
        MapleMessenger messenger = c.getPlayer().getMessenger();
        byte mode = slea.readByte();
        if (!c.getPlayer().getCanTalk() || !c.getPlayer().getCanTalk_event()) {
            c.getPlayer().dropMessage(5, "目前喇叭停止使用.");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        switch (mode) {
            case 0x00: // open
                if (messenger == null) {
                    int messengerid = slea.readInt();
                    if (messengerid == 0) { // create
                        c.getPlayer().setMessenger(World.Messenger.createMessenger(new MapleMessengerCharacter(c.getPlayer())));
                    } else { // join
                        messenger = World.Messenger.getMessenger(messengerid);
                        if (messenger != null) {
                            final int position = messenger.getLowestPosition();
                            if (position > -1 && position < 4) {
                                c.getPlayer().setMessenger(messenger);
                                World.Messenger.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getWorld(), c.getChannel());
                            }
                        }
                    }
                }
                break;
            case 0x02: // exit
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    World.Messenger.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                break;
            case 0x03: // invite

                if (messenger != null) {
                    final int position = messenger.getLowestPosition();
                    if (position <= -1 || position >= 4) {
                        return;
                    }
                    input = slea.readMapleAsciiString();
                    final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);

                    if (target != null) {
                        if (target.getMessenger() == null) {
                            if (!target.isGM() || c.getPlayer().isGM()) {
                                c.sendPacket(MaplePacketCreator.messengerNote(input, 4, 1));
                                target.getClient().sendPacket(MaplePacketCreator.messengerInvite(c.getPlayer().getName(), messenger.getId()));
                            } else {
                                c.sendPacket(MaplePacketCreator.messengerNote(input, 4, 0));
                            }
                        } else {
                            c.sendPacket(MaplePacketCreator.messengerChat(c.getPlayer().getName() + " : " + target.getName() + " 忙碌中."));
                        }
                    } else {
                        if (World.isConnected(input)) {
                            World.Messenger.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel(), c.getPlayer().isGM());
                        } else {
                            c.sendPacket(MaplePacketCreator.messengerNote(input, 4, 0));
                        }
                    }
                }
                break;
            case 0x05: // decline
                final String targeted = slea.readMapleAsciiString();
                final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { // This channel
                    if (target.getMessenger() != null) {
                        target.getClient().sendPacket(MaplePacketCreator.messengerNote(c.getPlayer().getName(), 5, 0));
                    }
                } else { // Other channel
                    if (!c.getPlayer().isGM()) {
                        World.Messenger.declineChat(targeted, c.getPlayer().getName());
                    }
                }
                break;
            case 0x06: // message
                if (messenger != null) {
                    String msg = slea.readMapleAsciiString();
                    if (ServerConfig.LOG_CHAT) {
                        FileoutputUtil.logToFile("logs/聊天/Messenger聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " Messenger: " + messenger.getId() + " " + msg);
                    }
                    World.Messenger.messengerChat(messenger.getId(), msg, c.getPlayer().getName());
                }
                break;
            default:
                System.err.println("Unhandled Messenger operation : " + String.valueOf(mode));

        }
    }

    public static final void WhisperFind(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        if (!c.getPlayer().getCanTalk()) {
            c.sendPacket(MaplePacketCreator.getItemNotice("在這個地方不能說話。"));
            return;
        }
        switch (mode) {
            case 68: //buddy
            case 5: { // Find
                final String recipient = slea.readMapleAsciiString();
                MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    if (!player.isGM() || c.getPlayer().isGM() && player.isGM()) {
                        c.sendPacket(MaplePacketCreator.getFindReplyWithMap(player.getName(), player.getMap().getId(), mode == 68));
                    } else {
                        c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } else { // Not found
                    int ch = World.Find.findChannel(recipient);
                    int wl = World.Find.findWorld(recipient);
                    if (ch > 0) {
                        player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(recipient);
                        if (player == null) {
                            break;
                        }

                        if (player != null) {
                            if (!player.isGM() || (c.getPlayer().isGM() && player.isGM())) {
                                c.sendPacket(MaplePacketCreator.getFindReply(recipient, (byte) ch, mode == 68));
                            } else {
                                c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            }
                            return;
                        }
                    }
                    switch (ch) {
                        case -10:
                            c.sendPacket(MaplePacketCreator.getFindReplyWithCS(recipient, mode == 68));
                            break;
                        case -20:
                            c.sendPacket(MaplePacketCreator.getFindReplyWithMTS(recipient, mode == 68));
                            break;
                        default:
                            c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            break;
                    }
                }
                break;
            }
            case 6: { // Whisper
                if (!c.getPlayer().getCanTalk()) {
                    c.sendPacket(MaplePacketCreator.getItemNotice("在這個地方不能說話。"));
                    return;
                }
                c.getPlayer().getCheatTracker().checkMsg();
                final String recipient = slea.readMapleAsciiString();
                final String text = slea.readMapleAsciiString();
                final int ch = World.Find.findChannel(recipient);
                final int wl = World.Find.findWorld(recipient);
                if (ch > 0 && (c.getPlayer().getWorld() == wl)) {
                    MapleCharacter player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(recipient);
                    if (player == null) {
                        break;
                    }
                    player.getClient().sendPacket(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    if (!c.getPlayer().isGM() && player.isGM()) {
                        c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    } else {
                        c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    }
                } else {
                    c.sendPacket(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
                // World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice(c.getPlayer().getName() + " 密語 " + recipient + " : " + text).getBytes());
                if (ServerConfig.LOG_CHAT) {
                    FileoutputUtil.logToFile("logs/聊天/私密聊天.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " <私密>: " + c.getPlayer().getName() + " 密語 " + recipient + " 說了 :" + text);
                }
            }
            break;
        }
    }
}
