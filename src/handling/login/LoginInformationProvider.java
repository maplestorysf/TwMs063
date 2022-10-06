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
package handling.login;

import client.MapleJob;
import constants.PiPiConfig;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Triple;

public class LoginInformationProvider {

    public static enum JobInfoFlag {

        臉型(0x1),
        髮型(0x2),
        臉飾(0x4),
        耳朵(0x8),
        尾巴(0x10),
        帽子(0x20),
        衣服(0x40),
        褲裙(0x80),
        披風(0x100),
        鞋子(0x200),
        手套(0x400),
        武器(0x800),
        副手(0x1000),;
        private final int value;

        private JobInfoFlag(int value) {
            this.value = value;
        }

        public int getVelue() {
            return value;
        }

        public boolean check(int x) {
            return (value & x) != 0;
        }
    }

    public static enum JobType {

        冒險家(1, MapleJob.初心者.getId(), 0),;
        public int type, id, map;
        public int flag = JobInfoFlag.臉型.getVelue() | JobInfoFlag.髮型.getVelue() | JobInfoFlag.衣服.getVelue() | JobInfoFlag.褲裙.getVelue() | JobInfoFlag.鞋子.getVelue() | JobInfoFlag.武器.getVelue();

        private JobType(int type, int id, int map) {
            this.type = type;
            this.id = id;
            this.map = map;
        }

        private JobType(int type, int id, int map, int flag) {
            this.type = type;
            this.id = id;
            this.map = map;
            this.flag |= flag;
        }

        public static JobType getByType(int g) {
            for (JobType e : JobType.values()) {
                if (e.type == g) {
                    return e;
                }
            }
            return null;
        }

        public static JobType getById(int g) {
            for (JobType e : JobType.values()) {
                if (e.id == g) {
                    return e;
                }
            }
            return null;
        }
    }

    private final static LoginInformationProvider instance = new LoginInformationProvider();
    protected final List<String> ForbiddenName = new ArrayList<>();
    protected final Map<String, String> Curse = new HashMap();
    //gender, val, job
    protected final Map<Triple<Integer, Integer, Integer>, List<Integer>> makeCharInfo = new HashMap<>();
    //0 = eyes 1 = hair 2 = haircolor 3 = skin 4 = top 5 = bottom 6 = shoes 7 = weapon

    public static LoginInformationProvider getInstance() {
        return instance;
    }

    protected LoginInformationProvider() {
        System.out.println("【讀取中】 LoginInformationProvider :::");
        final MapleDataProvider prov = MapleDataProviderFactory.getDataProvider("/Etc.wz");
        final MapleData nameData = prov.getData("ForbiddenName.img");
        for (final MapleData data : nameData.getChildren()) {
            ForbiddenName.add(MapleDataTool.getString(data));
        }
        final MapleData curseData = prov.getData("Curse.img");
        for (final MapleData data : curseData.getChildren()) {
            String[] curse = MapleDataTool.getString(data).split(",");
            Curse.put(curse[0], curse[1]);
            ForbiddenName.add(curse[0]);
        }
        Curse.putAll(PiPiConfig.CustomCurseText);
        final MapleData infoData = prov.getData("MakeCharInfo.img");
        for (MapleData dat : infoData) {
            int jobType;
            if (dat.getName().startsWith("Char")) {
                jobType = JobType.冒險家.id;
            } else {
                continue;
            }

            int gender;
            if (dat.getName().endsWith("Female")) {
                gender = 1;
            } else if (dat.getName().endsWith("Male")) {
                gender = 0;
            } else {
                continue;
            }

            for (MapleData d : dat) {
                Triple<Integer, Integer, Integer> key = new Triple<>(gender, Integer.parseInt(d.getName()), jobType);
                List<Integer> our = makeCharInfo.get(key);
                if (our == null) {
                    our = new ArrayList<>();
                    makeCharInfo.put(key, our);
                }
                for (MapleData dd : d) {
                    if (dd.getName().equalsIgnoreCase("color")) {
                        for (MapleData dda : dd) {
                            for (MapleData ddd : dda) {
                                our.add(MapleDataTool.getInt(ddd, -1));
                            }
                        }
                    } else if (!dd.getName().equalsIgnoreCase("name")) {
                        our.add(MapleDataTool.getInt(dd, -1));
                    }
                }
            }
        }
    }

    public final boolean isForbiddenName(final String in) {
        for (final String name : ForbiddenName) {
            if (in.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getCurseMsg(String in) {
        for (Map.Entry<String, String> entry : Curse.entrySet()) {
            String keyStr = "";
            int i = 1;
            for (String s : entry.getKey().split("")) {
                keyStr += s;
                if (entry.getKey().length() > i++) {
                    if (!s.matches("[a-zA-Z]")) {
                        keyStr += "[[^\\pL]a-zA-Z]*";
                    }
                }
            }
            if (keyStr.isEmpty()) {
                continue;
            }
            in = in.replaceAll("(?i)" + keyStr, entry.getValue());
        }
        return in;
    }

    public final boolean isEligibleItem(final int gender, final int val, final int job, final int item) {
        if (item < 0) {
            return false;
        }
        final Triple<Integer, Integer, Integer> key = new Triple<>(gender, val, job);
        final List<Integer> our = makeCharInfo.get(key);
        if (our == null) {
            return false;
        }
        return our.contains(item);
    }
}
