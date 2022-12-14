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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import server.maps.AnimatedMapleMapObject;
import server.movement.*;
import tools.data.LittleEndianAccessor;

public class MovementParse {

    //1 = player, 2 = mob, 3 = pet, 4 = summon, 5 = dragon
    public static final List<LifeMovementFragment> parseMovement(final LittleEndianAccessor lea, int kind) {
        final List<LifeMovementFragment> res = new ArrayList<>();
        final byte numCommands = lea.readByte();

        for (byte i = 0; i < numCommands; i++) {
            byte command = lea.readByte();
            switch (command) {
                case 0:
                case 5: { // Float
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short xwobble = lea.readShort();
                    final short ywobble = lea.readShort();
                    final short unk = lea.readShort();
                    short fh = 0;
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    StaticLifeMovement mov = new StaticLifeMovement(command, new Point(xpos, ypos), duration, newstate, unk);
                    mov.setUnk(unk);
                    mov.setFh(fh);
                    mov.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(mov);
                    break;
                }
                case 1:
                case 2:
                case 6:
                case 12: {
                    final short xwobble = lea.readShort();
                    final short ywobble = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    StaticLifeMovement mov = new StaticLifeMovement(command, null, duration, newstate, 0);
                    mov.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(mov);
                    break;
                }
                case 3:
                case 4:
                case 7:
                case 8:
                case 9:
                case 11: {
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short unk = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    StaticLifeMovement mov = new StaticLifeMovement(command, new Point(xpos, ypos), 0, newstate, 0);
                    mov.setUnk(unk);
                    res.add(mov);
                    break;
                }
                case 10: // Change Equip
                {
                    final byte newstate = 0;
                    final short duration = 0;
                    final int wui = lea.readByte();
                    final StaticLifeMovement mov = new StaticLifeMovement(command, null, duration, newstate, 0);
                    mov.setWui(wui);
                    res.add(mov);
                    break;
                }
                default:
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    StaticLifeMovement mov = new StaticLifeMovement(command, null, duration, newstate, 0);
                    res.add(mov);
                    break;
            }
        }
        if (numCommands != res.size()) {
            System.err.println("error in movement");
            return null; // Probably hack
        }
        return res;
    }

    public static final void updatePosition(final List<LifeMovementFragment> movement, final AnimatedMapleMapObject target, final int yoffset) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof StaticLifeMovement) {
                    Point position = ((StaticLifeMovement) move).getPosition();
                    if (position != null) {
                        position.y += yoffset;
                        target.setPosition(position);
                    }
                }
                target.setFh(((LifeMovement) move).getNewFh());
                target.setStance(((StaticLifeMovement) move).getNewstate());
            }
        }
    }
}
