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
package server.life;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;

import server.Randomizer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;

public class SpawnPointAreaBoss extends Spawns {

    private MapleMonster monster;
    private final Point pos1;
    private final Point pos2;
    private final Point pos3;
    private final int fh1;
    private final int fh2;
    private final int fh3;
    private int rand = -1;
    private long nextPossibleSpawn;
    private final int mobTime;
    private final AtomicBoolean spawned = new AtomicBoolean(false);
    private final String msg;

    public SpawnPointAreaBoss(final MapleMonster monster, final Pair<Integer, Point> pos1, final Pair<Integer, Point> pos2, final Pair<Integer, Point> pos3, final int mobTime, final String msg) {
        this.monster = monster;
        this.fh1 = pos1.getLeft();
        this.fh2 = pos2.getLeft();
        this.fh3 = pos3.getLeft();
        this.pos1 = pos1.getRight();
        this.pos2 = pos2.getRight();
        this.pos3 = pos3.getRight();
        this.mobTime = (mobTime < 0 ? -1 : (mobTime * 1000));
        this.msg = msg;
        this.nextPossibleSpawn = System.currentTimeMillis();
    }

    @Override
    public final int getFh() {
        if (rand == -1) {
            getPosition();
        }
        int fh = rand == 0 ? fh1 : rand == 1 ? fh2 : fh3;
        rand = -1;
        return fh;
    }

    @Override
    public final MapleMonster getMonster() {
        return monster;
    }

    @Override
    public final byte getCarnivalTeam() {
        return -1;
    }

    @Override
    public final int getCarnivalId() {
        return -1;
    }

    @Override
    public final boolean shouldSpawn() {
        if (mobTime < 0) {
            return false;
        }
        if (spawned.get()) {
            return false;
        }
        return nextPossibleSpawn <= System.currentTimeMillis();
    }

    @Override
    public final Point getPosition() {
        rand = Randomizer.nextInt(3);
        return rand == 0 ? pos1 : rand == 1 ? pos2 : pos3;
    }

    @Override
    public final MapleMonster spawnMonster(final MapleMap map) {
        final Point pos = getPosition();
        monster = new MapleMonster(monster);
        monster.setPosition(pos);
        monster.setOriginFh(getFh());
        monster.setFh(monster.getOriginFh());
        spawned.set(true);
        monster.setListener(new MonsterListener() {

            @Override
            public void monsterKilled() {
                nextPossibleSpawn = System.currentTimeMillis();

                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime;
                }
                spawned.set(false);
            }
        });
        map.spawnMonster(monster, -2);

        if (msg != null) {
            map.broadcastMessage(MaplePacketCreator.getItemNotice(msg));
        }
        return monster;
    }

    @Override
    public final int getMobTime() {
        return mobTime;
    }
}
