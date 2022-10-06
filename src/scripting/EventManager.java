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
 but WITHOUT ANY WARRANTY; w"ithout even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import java.util.Collection;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import javax.script.Invocable;
import javax.script.ScriptException;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import server.MapleSquad;
import server.Randomizer;
import server.Timer.*;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import tools.FilePrinter;

public class EventManager {

    private Invocable iv;
    private int world, channel;
    private Map<String, EventInstanceManager> instances = new WeakHashMap<>();
    private Properties props = new Properties();
    private String name;

    public EventManager(ChannelServer cserv, Invocable iv, String name) {
        this.iv = iv;
        this.channel = cserv.getChannel();
        this.name = name;
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : cancelSchedule:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : cancelSchedule:\n" + ex);
        }
    }

    public ScheduledFuture<?> schedule(final String methodName, long delay) {
        return EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException | NoSuchMethodException ex) {
                    System.err.println("Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                    FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : " + methodName + ":\n" + ex);

                }
            }
        }, delay);
    }

    public ScheduledFuture<?> schedule(final String methodName, long delay, final EventInstanceManager eim) {
        return EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, eim);
                } catch (ScriptException | NoSuchMethodException ex) {
                    System.err.println("Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                    FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : " + methodName + ":\n" + ex);

                }
            }
        }, delay);
    }

    public ScheduledFuture<?> scheduleAtTimestampByAutoEventTimer(final String methodName, long timestamp) {
        return autoEventTimer.getInstance().scheduleAtTimestamp(new Runnable() {

            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException | NoSuchMethodException ex) {
                    System.err.println("Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                    FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                }
            }
        }, timestamp);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, long timestamp) {
        return EventTimer.getInstance().scheduleAtTimestamp(new Runnable() {

            @Override
            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException | NoSuchMethodException ex) {
                    System.err.println("Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                    FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : " + methodName + ":\n" + ex);
                }
            }
        }, timestamp);
    }

    public int getChannel() {
        return channel;
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(world, channel);
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        EventInstanceManager ret = new EventInstanceManager(this, name, world, channel);
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(String name) {
        instances.remove(name);
        if (getProperty("state") != null && instances.isEmpty()) {
            setProperty("state", "0");
        }
        if (getProperty("leader") != null && instances.isEmpty() && getProperty("leader").equals("false")) {
            setProperty("leader", "true");
        }
        if (this.name.equals("CWKPQ")) { //hard code it because i said so
            final MapleSquad squad = ChannelServer.getInstance(world, channel).getMapleSquad("CWKPQ");//so fkin hacky
            if (squad != null) {
                squad.clear();
            }
        }
    }

    public Invocable getIv() {
        return iv;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public final Properties getProperties() {
        return props;
    }

    public String getName() {
        return name;
    }

    public void startInstance() {
        try {
            iv.invokeFunction("setup", (Object) null);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup:\n" + ex);
        }
    }

    public void startInstance(String mapid, MapleCharacter chr) {
        try {
            EventInstanceManager eim = (EventInstanceManager) iv.invokeFunction("setup", (Object) mapid);
            eim.registerCarnivalParty(chr, chr.getMap(), (byte) 0);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup:\n" + ex);
        }
    }

    public void startInstance_Party(String mapid, MapleCharacter chr) {
        try {
            EventInstanceManager eim = (EventInstanceManager) iv.invokeFunction("setup", (Object) mapid);
            eim.registerParty(chr.getParty(), chr.getMap());
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup:\n" + ex);
        }
    }

    //GPQ
    public void startInstance(MapleCharacter character, String leader) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerPlayer(character);
            eim.setProperty("leader", leader);
            eim.setProperty("guildid", String.valueOf(character.getGuildId()));
            setProperty("guildid", String.valueOf(character.getGuildId()));
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-Guild:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-Guild:\n" + ex);
        }
    }

    public void startInstance_CharID(MapleCharacter character) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", character.getId()));
            eim.registerPlayer(character);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-CharID:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-CharID:\n" + ex);
        }
    }

    public void startInstance(MapleCharacter character) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerPlayer(character);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-character:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-character:\n" + ex);
        }
    }

    //PQ method: starts a PQ
    public void startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", party.getId()));
            eim.registerParty(party, map);
        } catch (ScriptException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-partyid:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-partyid:\n" + ex);
        } catch (NoSuchMethodException ex) {
            //ignore
            startInstance_NoID(party, map, ex);
        }
    }

    public void startInstance_NoID(MapleParty party, MapleMap map) {
        startInstance_NoID(party, map, null);
    }

    public void startInstance_NoID(MapleParty party, MapleMap map, final Exception old) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-party:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-party:\n" + ex + "\n" + (old == null ? "no old exception" : old));
        }
    }

    //non-PQ method for starting instance
    public void startInstance(EventInstanceManager eim, String leader) {
        try {
            iv.invokeFunction("setup", eim);
            eim.setProperty("leader", leader);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-leader:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-leader:\n" + ex);
        }
    }

    public void startInstance(MapleSquad squad, MapleMap map) {
        startInstance(squad, map, -1, null);
    }

    public void startInstance(MapleSquad squad, MapleMap map, int questID) {
        startInstance(squad, map, questID, null);
    }

    public void startInstance(MapleSquad squad, MapleMap map, int questID, String bossname) {
        if (squad.getStatus() == 0) {
            return; //we dont like cleared squads
        }
        if (!squad.getLeader().isGM()) {
            if (squad.getMembers().size() < squad.getType().i) { //less than 3
                squad.getLeader().dropMessage(5, "這個遠征隊至少要有 " + squad.getType().i + " 人以上才可以開戰.");
                return;
            }
            if (name.equals("CWKPQ") && squad.getJobs().size() < 5) {
                squad.getLeader().dropMessage(5, "The squad requires members from every type of job.");
                return;
            }
        }
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", squad.getLeaderName()));
            eim.registerSquad(squad, map, questID, bossname);
        } catch (ScriptException | NoSuchMethodException ex) {
            System.err.println("Event name : " + name + ", method Name : setup-squad:\n" + ex);
            FilePrinter.printError("EventManager.txt", "Event name : " + name + ", method Name : setup-squad:\n" + ex);
        }
    }

    public void warpAllPlayer(int from, int to) {
        final MapleMap tomap = getMapFactory().getMap(to);
        final MapleMap frommap = getMapFactory().getMap(from);
        List<MapleCharacter> list = frommap.getCharactersThreadsafe();
        if (tomap != null && list != null && frommap.getCharactersSize() > 0) {
            for (MapleMapObject mmo : list) {
                ((MapleCharacter) mmo).changeMap(tomap, tomap.getPortal(0));
            }
        }
    }

    public MapleMapFactory getMapFactory() {
        return getChannelServer().getMapFactory();
    }

    public OverrideMonsterStats newMonsterStats() {
        return new OverrideMonsterStats();
    }

    public List<MapleCharacter> newCharList() {
        return new ArrayList<>();
    }

    public MapleMonster getMonster(final int id) {
        return MapleLifeFactory.getMonster(id);
    }

    public void broadcastYellowMsg(final String msg) {
        getChannelServer().broadcastPacket(MaplePacketCreator.yellowChat(msg));
    }

    public void broadcastServerMsg(final int type, final String msg, final boolean weather) {
        if (!weather) {
            getChannelServer().broadcastPacket(MaplePacketCreator.broadcastMessage(type, msg));
        } else {
            for (MapleMap load : getMapFactory().getAllMaps()) {
                if (load.getCharactersSize() > 0) {
                    load.startMapEffect(msg, type);
                }
            }
        }
    }

    /*
     * 開啟所有系統活動
     */
    public boolean scheduleSystemEvent() {
        boolean omg = false;
        for (MapleEventType x : MapleEventType.values()) {
            // WZ有問題暫時不開放的活動
            switch (x) {
                case 打瓶蓋:
                case 爬繩子:
                case 是非題大考驗:
                    break;
                default:
                    omg |= scheduleSystemEvent(x.name());
                    break;
            }
        }
        return omg;
    }

    /*
     * 開啟指定系統活動
     */
    public boolean scheduleSystemEvent(String s) {
        MapleEventType type = MapleEventType.getByString(s);

        int eventChannel = 0, eventWorld = 0;
        if (type != null) {
            ChannelServer cs = ChannelServer.getInstance(type.world, type.channel);
            if (cs != null && cs.getEvent() == -1 && !cs.getEvent(type).isRunning()) {
                eventChannel = type.channel;
                eventWorld = type.world;
                return scheduleSystemEventInChannel(s, eventWorld, eventChannel);
            } else {
                type = null;
            }
        }
//        if (type == null) {
//            for (ChannelServer cs : ChannelServer.getAllInstances()) {
//                if (cs == null || cs.getEvent() > -1) {
//                    continue;
//                }
//                eventChannel = cs.getChannel();
//            }
//            if (eventChannel == 0) {
//                eventChannel = 1;
//            }
//        }

        return false;
    }

    /*
     * 在指定頻道開啟隨機系統活動
     */
    public boolean scheduleRandomEventInChannel(int world, int chz) {
        String s = null;
        for (MapleEventType x : MapleEventType.values()) {
            if (x.channel == chz) {
                s = x.name();
                break;
            }
        }
        while (s == null) {
            for (MapleEventType x : MapleEventType.values()) {
                if (Randomizer.nextInt(MapleEventType.values().length) == 0) {
                    s = x.name();
                    break;
                }
            }
        }
        return scheduleSystemEventInChannel(s, world, chz);
    }

    /*
     * 在指定頻道開啟指定系統活動
     */
    public boolean scheduleSystemEventInChannel(String s, int world, int chz) {
        MapleEventType t = MapleEventType.getByString(s);
        final ChannelServer cs = ChannelServer.getInstance(world, chz);
        if (t == null || chz < 1 || cs == null || cs.getEvent() > -1 || cs.getEvent(t).isRunning()) {
            return false;
        }

        final String msg = MapleEvent.scheduleEvent(t, cs);
        if (msg.length() > 0) {
            broadcastYellowMsg(msg);
            return false;
        }
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (cs.getEvent() >= 0) {
                    MapleEvent.setEvent(cs, true);
                }
            }
        }, 180000);
        return true;
    }
}
