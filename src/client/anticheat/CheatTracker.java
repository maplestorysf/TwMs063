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
        MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item.getItemId());
        /* 攻擊速度增加判斷 */
        if ((tickcount - lastAttackTickCount) > 0 && (tickcount - lastAttackTickCount) < AtkDelay) {
            registerOffense(CheatingOffense.快速攻擊, "攻擊速度異常，技能: " + SkillFactory.getName(skillId) + "[" + skillId + "]" + " 玩家回傳: " + (tickcount - lastAttackTickCount) + " " + "服務端計算: " + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ((chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) == null ? "" : ("最終極速:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION)))) + ((chr.get().getBuffedValue(MapleBuffStat.BOOSTER) == null ? "" : ("增加功速" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)))));
            nulls = false;
        }
        if (skillId != 3110001 && (tickcount - lastAttackTickCount) >= 0 && ((tickcount - lastAttackTickCount) == 90 || (tickcount - lastAttackTickCount) == 60 || (tickcount - lastAttackTickCount) == 30)) {
            int times = 10;
            if (chr.get().getAttackDebugMessage()) {
                chr.get().dropMessage(6, "觸發功速增量");
            }
            while (times > 0) {
                registerOffense(CheatingOffense.快速攻擊, "攻速異常,技能: " + SkillFactory.getName(skillId) + "[" + skillId + "]" + " 玩家回傳: " + (tickcount - lastAttackTickCount) + " " + "服務端計算: " + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ((chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) == null ? "" : ("最終極速:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION)))) + ((chr.get().getBuffedValue(MapleBuffStat.BOOSTER) == null ? "" : ("增加功速" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)))));
                times--;
            }
            nulls = false;
        }
        /* 攻擊速度增加判斷 */

        /* 無延遲判斷 */
        final long STime_TC = System.currentTimeMillis() - tickcount; // hack = - more
        final long ping = Server_ClientAtkTickDiff - STime_TC;
        if (ping > 1500) { // 250 is the ping, TODO
            registerOffense(CheatingOffense.無延遲攻擊, "無延遲,地圖[" + chr.get().getMapId() + "] 技能: " + SkillFactory.getSkillName(skillId) + " Server_ClientAtkTickDiff: " + Server_ClientAtkTickDiff + " STime_TC: " + STime_TC + " 間隔" + (Server_ClientAtkTickDiff - STime_TC));
            nulls = false;
        }
        /* 無延遲判斷 */

        if (chr.get().getAttackDebugMessage()) {
            chr.get().dropMessage(5, "Delay " + SkillFactory.getName(skillId) + "[" + skillId + "] = 玩家回傳: " + (tickcount - lastAttackTickCount) + ", 服務端計算:" + AtkDelay + " " + (weapon_item == null ? 0 : weapon_item.getItemId()) + "(" + weapon.name() + ")" + ("最終極速:" + chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) + " 增加功速:" + chr.get().getBuffedValue(MapleBuffStat.BOOSTER)));
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
                registerOffense(CheatingOffense.HIGH_AVOID, "迴避率過高 ");
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
                registerOffense(CheatingOffense.SAME_DAMAGE, numSameDamage + " 次, 攻擊傷害: " + dmg + ", 預計傷害: " + expected + " [等級: " + chr.get().getLevel() + ", 職業: " + chr.get().getJob() + "]");
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
        //        registerOffense(CheatingOffense.召喚獸無延遲, chr.get().getName() + "快速召喚獸攻擊 " + time + " < " + numSequentialSummonAttack);
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
            chr.get().dropMessage("觸發違規：" + offense + " 原因：" + param);
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
                        outputFileName = "斷線";
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + chrhardref.getName() + " 自動斷線 類別: " + offense.toString() + " 原因: " + (param == null ? "" : (" - " + param))));
                        FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                        chrhardref.getClient().getSession().close();
                    } else {
                        outputFileName = "未斷線";
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + chrhardref.getName() + " 未自動斷線 類別: " + offense.toString() + " 原因: " + (param == null ? "" : (" - " + param))));
                        FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                    }
                    break;
                case 3:
                    boolean ban = true;
                    outputFileName = "封鎖";
                    String show = "使用違法程式練功";
                    String real = "";
                    switch (offense) {
                        case ITEMVAC_SERVER:
                            outputFileName = "全圖吸物";
                            real = "使用全圖吸物";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                                break;
                            }
                            break;
                        case 召喚獸無延遲:
                            outputFileName = "召喚獸無延遲";
                            real = "使用召喚獸無延遲攻擊";
                            break;
                        case MOB_VAC_X:
                            outputFileName = "X吸怪";
                            real = "使用X吸怪";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case 吸怪:
                            outputFileName = "吸怪";
                            real = "使用吸怪";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case ATTACK_FARAWAY_MONSTER_BAN:
                            outputFileName = "全圖打";
                            real = "使用全圖打";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        case 無鬥氣使用鬥氣技能:
                            outputFileName = "技能異常";
                            real = "無鬥氣使用鬥氣技能";
                            break;
                        case 無箭矢發射弓箭:
                            outputFileName = "箭矢異常";
                            real = "沒有箭矢發射弓箭";
                            break;
                        case 無MP使用技能:
                            outputFileName = "技能異常";
                            real = "沒有足夠MP使用技能";
                            break;
                        case 群體治癒攻擊不死系怪物:
                            outputFileName = "技能異常";
                            real = "群體治癒攻擊不死系怪物";
                            break;
                        case 攻擊怪物數量異常:
                            outputFileName = "技能異常";
                            real = "打怪數量異常";
                            break;
                        case 技能攻擊次數異常:
                            outputFileName = "技能異常";
                            real = "技能攻擊次數";
                            break;
                        case ARAN_COMBO_HACK:
                            outputFileName = "技能異常";
                            real = "沒有足夠COMBO使用技能";
                            break;
                        case 無延遲攻擊:
                        case 快速攻擊:
                            outputFileName = "技能異常";
                            real = "無延遲使用技能";
                            if (!PiPiConfig.getAutoban()) {
                                ban = false;
                            }
                            break;
                        default:
                            ban = false;
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + MapleCharacterUtil.makeMapleReadable(chrhardref.getName()) + " (編號: " + chrhardref.getId() + " )使用外掛! " + StringUtil.makeEnumHumanReadable(offense.name()) + (param == null ? "" : (" - " + param))));
                            break;
                    }
                    if (chr.get().hasGmLevel(1)) {
                        chr.get().dropMessage("觸發封鎖: " + real + " 原因: " + (param == null ? "" : (" - " + param)));
                    } else {
                        if (ban) {
                            FileoutputUtil.logToFile("logs/Hack/Ban/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("[封鎖系統] " + chrhardref.getName() + " 因為" + show + "而被管理員永久停權。"));
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + chrhardref.getName() + " " + real + "自動封鎖! "));
                            chrhardref.ban(chrhardref.getName() + real, true, true, false);
                            chrhardref.getClient().getSession().close();
                        } else {
                            FileoutputUtil.logToFile("logs/Hack/未封鎖-" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
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
            case 快速攻擊:
                gm_message--;
                if (gm_message % 10 == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! 無延遲攻擊" + (param == null ? "" : (" - " + param))));
                    FileoutputUtil.logToFile("logs/Hack/無延遲.txt", "\r\n" + FileoutputUtil.NowTime() + " " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! 無延遲攻擊" + (param == null ? "" : (" - " + param)));
                }
                break;
            case 召喚獸無延遲:
            case ITEMVAC_SERVER:
            case 吸怪:
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
                        show = "全圖打";
                        out_log = "攻擊範圍異常";
                        log = true;
                        break;
                    case "MOB_VAC":
                        show = "使用吸怪";
                        out_log = "吸怪";
                        out_show = false;
                        log = true;
                        break;
                }

                if (gm_message % 5 == 0) {
                    if (out_show) {
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[GM密語] " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! " + show + (param == null ? "" : (" - " + param))));
                    }
                    if (log) {
                        FileoutputUtil.logToFile("logs/Hack/" + out_log + ".txt", "\r\n" + FileoutputUtil.NowTime() + " " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! " + show + (param == null ? "" : (" - " + param)));
                    }
                }

                if (gm_message == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[封號系統] " + chrhardref.getName() + " (編號: " + chrhardref.getId() + " )疑似外掛！" + show + (param == null ? "" : (" - " + param))));
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
