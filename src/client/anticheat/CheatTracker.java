package client.anticheat;

import client.MapleBuffStat;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import constants.GameConstants;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.SkillFactory;
import client.inventory.IItem;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.PiPiConfig;
import handling.world.World;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.AutobanManager;
import server.Timer.CheatTimer;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;

public class CheatTracker {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rL = lock.readLock(), wL = lock.writeLock();
    private final Map<CheatingOffense, CheatingOffenseEntry> offenses = new LinkedHashMap<>();
    private final WeakReference<MapleCharacter> chr;
    // For keeping track of speed attack hack.
    private final Map<Integer, Long> lastAttackTick = new LinkedHashMap<>();
    private byte Attack_tickResetCount = 0;
    private long Server_ClientAtkTickDiff = 0;
    private long lastDamage = 0;
    private long takingDamageSince;
    private int numSequentialDamage = 0;
    private long lastDamageTakenTime = 0;
    private byte numZeroDamageTaken = 0;
    private int numSequentialSummonAttack = 0;
    private long summonSummonTime = 0;
    private int numSameDamage = 0;
    private Point lastMonsterMove;
    private int monsterMoveCount;
    private int attacksWithoutHit = 0;
    private byte dropsPerSecond = 0;
    private long lastDropTime = 0;
    private byte msgsPerSecond = 0;
    private long lastMsgTime = 0;
    private ScheduledFuture<?> invalidationTask;
    private int gm_message = 100;
    private int lastTickCount = 0, tickSame = 0;
    private long lastASmegaTime = 0;
    public long[] lastTime = new long[6];

    public CheatTracker(final MapleCharacter chr) {
        this.chr = new WeakReference<>(chr);
        invalidationTask = CheatTimer.getInstance().register(new InvalidationTask(), 60000);
        takingDamageSince = System.currentTimeMillis();
    }

    public final boolean checkAttack(final int skillId, final int tickcount) {
        boolean nulls = true;
        long lastAttackTickCount = 0;

        if (lastAttackTick.containsKey(skillId)) {
            lastAttackTickCount = lastAttackTick.get(skillId);
        } else {
            lastAttackTick.put(skillId, 0l);
        }

        short AtkDelay = GameConstants.getAttackDelay(chr.get(), skillId);
        IItem weapon_item = chr.get().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.???????????? : GameConstants.getWeaponType(weapon_item.getItemId());
        /* ???????????????????????? */
        if ((tickcount - lastAttackTickCount) > 0 && (tickcount - lastAttackTickCount) < AtkDelay) {
            registerOffense(CheatingOffense.????????????, "???????????????????????????: " + SkillFactory.getName(skillId) + "[" + skillId + "]" + " ????????????: " + (tickcount - lastAttackTickCount) + " " + "???????????????: " + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ((chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) == null ? "" : ("????????????:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION)))) + ((chr.get().getBuffedValue(MapleBuffStat.BOOSTER) == null ? "" : ("????????????" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)))));
            nulls = false;
        }
        if (skillId != 3110001 && (tickcount - lastAttackTickCount) >= 0 && ((tickcount - lastAttackTickCount) == 90 || (tickcount - lastAttackTickCount) == 60 || (tickcount - lastAttackTickCount) == 30)) {
            int times = 10;
            if (chr.get().getAttackDebugMessage()) {
                chr.get().dropMessage(6, "??????????????????");
            }
            while (times > 0) {
                registerOffense(CheatingOffense.????????????, "????????????,??????: " + SkillFactory.getName(skillId) + "[" + skillId + "]" + " ????????????: " + (tickcount - lastAttackTickCount) + " " + "???????????????: " + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ((chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) == null ? "" : ("????????????:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION)))) + ((chr.get().getBuffedValue(MapleBuffStat.BOOSTER) == null ? "" : ("????????????" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)))));
                times--;
            }
            nulls = false;
        }
        /* ???????????????????????? */

        /* ??????????????? */
        final long STime_TC = System.currentTimeMillis() - tickcount; // hack = - more
        final long ping = Server_ClientAtkTickDiff - STime_TC;
        if (ping > 1500) { // 250 is the ping, TODO
            registerOffense(CheatingOffense.???????????????, "?????????,??????[" + chr.get().getMapId() + "] ??????: " + SkillFactory.getSkillName(skillId) + " Server_ClientAtkTickDiff: " + Server_ClientAtkTickDiff + " STime_TC: " + STime_TC + " ??????" + (Server_ClientAtkTickDiff - STime_TC));
            nulls = false;
        }
        /* ??????????????? */

        if (chr.get().getAttackDebugMessage()) {
            chr.get().dropMessage(5, "Delay " + SkillFactory.getName(skillId) + "[" + skillId + "] = ????????????: " + (tickcount - lastAttackTickCount) + ", ???????????????:" + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ("????????????:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) + " ????????????:" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)));
        }

        Server_ClientAtkTickDiff = STime_TC;
        chr.get().updateTick(tickcount);
        if (lastAttackTick.containsKey(skillId)) {
            lastAttackTick.remove(skillId);
            lastAttackTick.put(skillId, (long) tickcount);
        }
        return nulls;
    }

    public final void checkTakeDamage(final int damage) {
        numSequentialDamage++;
        lastDamageTakenTime = System.currentTimeMillis();

        // System.out.println("tb" + timeBetweenDamage);
        // System.out.println("ns" + numSequentialDamage);
        // System.out.println(timeBetweenDamage / 1500 + "(" + timeBetweenDamage / numSequentialDamage + ")");
        if (lastDamageTakenTime - takingDamageSince / 500 < numSequentialDamage) {
//            registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
        }
        if (lastDamageTakenTime - takingDamageSince > 4500) {
            takingDamageSince = lastDamageTakenTime;
            numSequentialDamage = 0;
        }
        /*	(non-thieves)
         Min Miss Rate: 2%
         Max Miss Rate: 80%
         (thieves)
         Min Miss Rate: 5%
         Max Miss Rate: 95%*/
        if (damage == 0) {
            numZeroDamageTaken++;
            if (numZeroDamageTaken >= 35) { // Num count MSEA a/b players
                numZeroDamageTaken = 0;
                registerOffense(CheatingOffense.HIGH_AVOID, "??????????????? ");
            }
        } else if (damage != -1) {
            numZeroDamageTaken = 0;
        }
    }

    public final void checkSameDamage(final int dmg, final double expected) {
        if (dmg > 2000 && lastDamage == dmg && chr.get() != null && (chr.get().getLevel() < 175 || dmg > expected * 2)) {
            numSameDamage++;

            if (numSameDamage > 5) {
                numSameDamage = 0;
                registerOffense(CheatingOffense.SAME_DAMAGE, numSameDamage + " ???, ????????????: " + dmg + ", ????????????: " + expected + " [??????: " + chr.get().getLevel() + ", ??????: " + chr.get().getJob() + "]");
            }
        } else {
            lastDamage = dmg;
            numSameDamage = 0;
        }
    }

    public final void resetSummonAttack() {
        summonSummonTime = System.currentTimeMillis();
        numSequentialSummonAttack = 0;
    }

    public final boolean checkSummonAttack() {
        numSequentialSummonAttack++;
        //estimated
        // System.out.println(numMPRegens + "/" + allowedRegens);
        // long time = (System.currentTimeMillis() - summonSummonTime) / (2000 + 1) + 3l;
        //  if (time < numSequentialSummonAttack) {
        //        registerOffense(CheatingOffense.??????????????????, chr.get().getName() + "????????????????????? " + time + " < " + numSequentialSummonAttack);
        //      return false;
        //  }
        return true;
    }

    public final void checkDrop() {
        checkDrop(false);
    }

    public final void checkDrop(final boolean dc) {
        if ((System.currentTimeMillis() - lastDropTime) < 1000) {
            dropsPerSecond++;
            if (dropsPerSecond >= (dc ? 32 : 16) && chr.get() != null) {
//                if (dc) {
//                    chr.get().getClient().getSession().close();
//                } else {
//                chr.get().getClient().setMonitored(true);
//                }
            }
        } else {
            dropsPerSecond = 0;
        }
        lastDropTime = System.currentTimeMillis();
    }

    public boolean canAvatarSmega2() {
        long time = 10 * 1000;
        if (chr.get() != null) {
            if (chr.get().getId() == 845 || chr.get().getId() == 5247 || chr.get().getId() == 12048) {
                time = 20 * 1000;
            }
            if (lastASmegaTime + time > System.currentTimeMillis() && !chr.get().isGM()) {
                return false;
            }
        }
        lastASmegaTime = System.currentTimeMillis();
        return true;
    }

    public synchronized boolean GMSpam(int limit, int type) {
        if (type < 0 || lastTime.length < type) {
            type = 1; // default xD
        }
        if (System.currentTimeMillis() < limit + lastTime[type]) {
            return true;
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }

    public final void checkMsg() { //ALL types of msg. caution with number of  msgsPerSecond
        if ((System.currentTimeMillis() - lastMsgTime) < 1000) { //luckily maplestory has auto-check for too much msging
            msgsPerSecond++;
            /*            if (msgsPerSecond > 10 && chr.get() != null) {
             chr.get().getClient().getSession().close();
             }*/
        } else {
            msgsPerSecond = 0;
        }
        lastMsgTime = System.currentTimeMillis();
    }

    public final int getAttacksWithoutHit() {
        return attacksWithoutHit;
    }

    public final void setAttacksWithoutHit(final boolean increase) {
        if (increase) {
            this.attacksWithoutHit++;
        } else {
            this.attacksWithoutHit = 0;
        }
    }

    public final void registerOffense(final CheatingOffense offense) {
        registerOffense(offense, null);
    }

    public final void registerOffense(final CheatingOffense offense, final String param) {
        final MapleCharacter chrhardref = chr.get();
        if (chrhardref == null || !offense.isEnabled() || chrhardref.isClone()) {
            return;
        }
        if (chr.get().hasGmLevel(5)) {
            chr.get().dropMessage("???????????????" + offense + " ?????????" + param);
        }
        CheatingOffenseEntry entry = null;
        rL.lock();
        try {
            entry = offenses.get(offense);
        } finally {
            rL.unlock();
        }
        if (entry != null && entry.isExpired()) {
            expireEntry(entry);
            entry = null;
        }
        if (entry == null) {
            entry = new CheatingOffenseEntry(offense, chrhardref.getId());
        }
        if (param != null) {
            entry.setParam(param);
        }
        entry.incrementCount();
        if (offense.shouldAutoban(entry.getCount())) {
            final int type = offense.getBanType();
            String outputFileName;
            switch (type) {
                case 1:
                    AutobanManager.getInstance().autoban(chrhardref.getClient(), StringUtil.makeEnumHumanReadable(offense.name()));
                    break;
                case 2:
                    if (PiPiConfig.getAutodc()) {
                        outputFileName = "??????";
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + chrhardref.getName() + " ???????????? ??????: " + offense.toString() + " ??????: " + (param == null ? "" : (" - " + param))));
                        FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " ?????????" + chr.get().getName() + " ?????????" + offense.toString() + " ????????? " + (param == null ? "" : (" - " + param)));
                        chrhardref.getClient().getSession().close();
                    } else {
                        outputFileName = "?????????";
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + chrhardref.getName() + " ??????????????? ??????: " + offense.toString() + " ??????: " + (param == null ? "" : (" - " + param))));
                        FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " ?????????" + chr.get().getName() + " ?????????" + offense.toString() + " ????????? " + (param == null ? "" : (" - " + param)));
                    }
                    break;
                case 3:
                    boolean ban = true;
                    outputFileName = "??????";
                    String show = "????????????????????????";
                    String real = "";
                    switch (offense) {
                        case ITEMVAC_SERVER:
                            outputFileName = "????????????";
                            real = "??????????????????";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                                break;
                            }
                            break;
                        case ??????????????????:
                            outputFileName = "??????????????????";
                            real = "??????????????????????????????";
                            break;
                        case MOB_VAC_X:
                            outputFileName = "X??????";
                            real = "??????X??????";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case ??????:
                            outputFileName = "??????";
                            real = "????????????";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case ATTACK_FARAWAY_MONSTER_BAN:
                            outputFileName = "?????????";
                            real = "???????????????";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case ???????????????????????????:
                            outputFileName = "????????????";
                            real = "???????????????????????????";
                            break;
                        case ?????????????????????:
                            outputFileName = "????????????";
                            real = "????????????????????????";
                            break;
                        case ???MP????????????:
                            outputFileName = "????????????";
                            real = "????????????MP????????????";
                            break;
                        case ?????????????????????????????????:
                            outputFileName = "????????????";
                            real = "?????????????????????????????????";
                            break;
                        case ????????????????????????:
                            outputFileName = "????????????";
                            real = "??????????????????";
                            break;
                        case ????????????????????????:
                            outputFileName = "????????????";
                            real = "??????????????????";
                            break;
                        case ARAN_COMBO_HACK:
                            outputFileName = "????????????";
                            real = "????????????COMBO????????????";
                            break;
                        case ???????????????:
                        case ????????????:
                            outputFileName = "????????????";
                            real = "?????????????????????";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        default:
                            ban = false;
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + MapleCharacterUtil.makeMapleReadable(chrhardref.getName()) + " (??????: " + chrhardref.getId() + " )????????????! " + StringUtil.makeEnumHumanReadable(offense.name()) + (param == null ? "" : (" - " + param))));
                            break;
                    }
                    if (chr.get().hasGmLevel(1)) {
                        chr.get().dropMessage("????????????: " + real + " ??????: " + (param == null ? "" : (" - " + param)));
                    } else {
                        if (ban) {
                            FileoutputUtil.logToFile("logs/Hack/Ban/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " ?????????" + chr.get().getName() + " ?????????" + offense.toString() + " ????????? " + (param == null ? "" : (" - " + param)));
                            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[????????????] " + chrhardref.getName() + " ??????" + show + "??????????????????????????????"));
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + chrhardref.getName() + " " + real + "????????????! "));
                            chrhardref.ban(chrhardref.getName() + real, true, true, false);
                            chrhardref.getClient().getSession().close();
                        } else {
                            FileoutputUtil.logToFile("logs/Hack/?????????-" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " ?????????" + chr.get().getName() + " ?????????" + offense.toString() + " ????????? " + (param == null ? "" : (" - " + param)));
                        }
                    }
                    break;
                default:
                    break;
            }
            gm_message = 100;
            return;
        }

        wL.lock();

        try {
            offenses.put(offense, entry);
        } finally {
            wL.unlock();
        }
        switch (offense) {
            case ????????????:
                gm_message--;
                if (gm_message % 10 == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + chrhardref.getName() + " (??????:" + chrhardref.getId() + ")????????????! ???????????????" + (param == null ? "" : (" - " + param))));
                    FileoutputUtil.logToFile("logs/Hack/?????????.txt", "\r\n" + FileoutputUtil.NowTime() + " " + chrhardref.getName() + " (??????:" + chrhardref.getId() + ")????????????! ???????????????" + (param == null ? "" : (" - " + param)));
                }
                break;
            case ??????????????????:
            case ITEMVAC_SERVER:
            case ??????:
            case HIGH_DAMAGE_MAGIC:
            case HIGH_DAMAGE_MAGIC_2:
            case HIGH_DAMAGE:
            case HIGH_DAMAGE_2:
            case ATTACK_FARAWAY_MONSTER:
            //case ATTACK_FARAWAY_MONSTER_SUMMON:
            case SAME_DAMAGE:
                gm_message--;

                String out_log = "";
                String show = offense.name();
                boolean log = false;
                boolean out_show = true;

                switch (show) {
                    case "ATTACK_FARAWAY_MONSTER":
                        show = "?????????";
                        out_log = "??????????????????";
                        log = true;
                        break;
                    case "MOB_VAC":
                        show = "????????????";
                        out_log = "??????";
                        out_show = false;
                        log = true;
                        break;
                }

                if (gm_message % 5 == 0) {
                    if (out_show) {
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM??????] " + chrhardref.getName() + " (??????:" + chrhardref.getId() + ")????????????! " + show + (param == null ? "" : (" - " + param))));
                    }
                    if (log) {
                        FileoutputUtil.logToFile("logs/Hack/" + out_log + ".txt", "\r\n" + FileoutputUtil.NowTime() + " " + chrhardref.getName() + " (??????:" + chrhardref.getId() + ")????????????! " + show + (param == null ? "" : (" - " + param)));
                    }
                }

                if (gm_message == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[????????????] " + chrhardref.getName() + " (??????: " + chrhardref.getId() + " )???????????????" + show + (param == null ? "" : (" - " + param))));
                    AutobanManager.getInstance().autoban(chrhardref.getClient(), StringUtil.makeEnumHumanReadable(offense.name()));
                    gm_message = 100;
                }

                break;
        }
        CheatingOffensePersister.getInstance().persistEntry(entry);
    }

    public void updateTick(int newTick) {
        if (newTick == lastTickCount) { //definitely packet spamming
/*	    if (tickSame >= 5) {
             chr.get().getClient().getSession().close(); //i could also add a check for less than, but i'm not too worried at the moment :)
             } else {*/
            tickSame++;
//	    }
        } else {
            tickSame = 0;
        }
        lastTickCount = newTick;
    }

    public final void expireEntry(final CheatingOffenseEntry coe) {
        wL.lock();
        try {
            offenses.remove(coe.getOffense());
        } finally {
            wL.unlock();
        }
    }

    public final int getPoints() {
        int ret = 0;
        CheatingOffenseEntry[] offenses_copy;
        rL.lock();
        try {
            offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
        } finally {
            rL.unlock();
        }
        for (final CheatingOffenseEntry entry : offenses_copy) {
            if (entry.isExpired()) {
                expireEntry(entry);
            } else {
                ret += entry.getPoints();
            }
        }
        return ret;
    }

    public final Map<CheatingOffense, CheatingOffenseEntry> getOffenses() {
        return Collections.unmodifiableMap(offenses);
    }

    public final String getSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<CheatingOffenseEntry> offenseList = new ArrayList<>();
        rL.lock();
        try {
            for (final CheatingOffenseEntry entry : offenses.values()) {
                if (!entry.isExpired()) {
                    offenseList.add(entry);
                }
            }
        } finally {
            rL.unlock();
        }
        Collections.sort(offenseList, new Comparator<CheatingOffenseEntry>() {

            @Override
            public final int compare(final CheatingOffenseEntry o1, final CheatingOffenseEntry o2) {
                final int thisVal = o1.getPoints();
                final int anotherVal = o2.getPoints();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        final int to = Math.min(offenseList.size(), 4);
        for (int x = 0; x < to; x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).getOffense().name()));
            ret.append(": ");
            ret.append(offenseList.get(x).getCount());
            if (x != to - 1) {
                ret.append(" ");
            }
        }
        return ret.toString();
    }

    public final void dispose() {
        if (invalidationTask != null) {
            invalidationTask.cancel(false);
        }
        invalidationTask = null;

    }

    private final class InvalidationTask implements Runnable {

        @Override
        public final void run() {
            CheatingOffenseEntry[] offenses_copy;
            rL.lock();
            try {
                offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
            } finally {
                rL.unlock();
            }
            for (CheatingOffenseEntry offense : offenses_copy) {
                if (offense.isExpired()) {
                    expireEntry(offense);
                }
            }
            if (chr.get() == null) {
                dispose();
            }
        }
    }

    public long[] getLastGMspam() {
        return lastTime;
    }
}
