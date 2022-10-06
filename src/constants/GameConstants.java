package constants;

import constants.skills.SkillType;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.inventory.IItem;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;
import handling.channel.handler.AttackInfo;
import handling.login.LoginServer;
import java.util.List;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import server.MapleStatEffect;
import server.Randomizer;
import server.maps.MapleMapObjectType;

public class GameConstants {

    public static List<Integer> blockedNpcs = new LinkedList<>();

    public static final int HP_ITEM = 122221;
    public static final int MP_ITEM = 122222;

    public static boolean isLinkedAttackSkill(final int id) {
        return getLinkedAttackSkill(id) != id;
    }

    public static int getLinkedAttackSkill(final int id) {
        switch (id) {
            case 11101220: // 皇家衝擊
                return 11101120; // 潛行突襲
            case 11101221: // 焚影
                return 11101121; // 殘像追擊
            case 11111120: // 月影
                return 11111220; // 光芒四射
            case 11111121: // 月光十字架
                return 11111221; // 日光十字架
            case 11121201: // 疾速黃昏
            case 11121102: // 月光之舞（空中）
            case 11121202: // 疾速黃昏（空中
                return 11121101; // 月光之舞
            case 11121103: // 新月分裂
                return 11121203; // 太陽穿刺
            case 21110007:
            case 21110008:
                return 21110002;
            case 21120009:
            case 21120010:
                return 21120002;
            case 4321001:
                return 4321000;
            case 5300007:
                return 5301001;
            case 5320011:
                return 5321004;
            case 5211015:
            case 5211016:
                return 5211011;
            case 5001008:
                return 5200010;
            case 5001009:
                return 5101004;
        }
        return id;
    }
    public static final List<MapleMapObjectType> rangedMapobjectTypes = Collections.unmodifiableList(Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MIST));
    private static final int[] ExpTable = {0, 15, 34, 57, 92, 135, 372, 560, 840, 1242, 1716,
        2360, 3216, 4200, 5460, 7050, 8840, 11040, 13716, 16680, 20216,
        24402, 28980, 34320, 40512, 47216, 54900, 63666, 73080, 83720, 95700,
        108480, 122760, 138666, 155540, 174216, 194832, 216600, 240500, 266682, 294216,
        324240, 356916, 391160, 428280, 468450, 510420, 555680, 604416, 655200, 709716, // 51等到這
        748608, 789631, 832902, 878545, 926689, 977471, 1031036, 1087536, 1147132, 1209994,
        1276301, 1346242, 1420016, 1497832, 1579913, 1666492, 1757815, 1854143, 1955750, 2062925, // 71等到這
        2175973, 2295216, 2410993, 2553663, 2693603, 2841212, 2996910, 3161140, 3334370, 3517093,
        3709829, 3913127, 4127566, 4353756, 4592341, 4844001, 5109452, 5389449, 5684790, 5996316,
        6324914, 6671519, 7037118, 7422752, 7829518, 8258575, 8711144, 9188514, 9692044, 10223168, // 101等到這
        10783397, 11374327, 11997640, 12655110, 13348610, 14080113, 14851703, 15665576, 16524049, 17429566,
        18384706, 19392187, 20454878, 21575805, 22758159, 24005306, 25320796, 26708375, 28171993, 29715818,//121等到這
        31344244, 33061908, 34873700, 36784778, 38800583, 40926854, 43169645, 45535341, 48030677, 50662758,//131等到這
        53439077, 56367538, 59456479, 62714694, 66151459, 69776558, 73600313, 77633610, 81887931, 86375389,//141等到這
        91108760, 96101520, 101367883, 106922842, 112782213, 118962678, 125481832, 132358236, 139611467, 147262175,//151等到這
        155332142, 163844343, 172823012, 182293713, 192283408, 202820538, 213935103, 225658746, 238024845, 251068606, //160
        264827165, 279339693, 294647508, 310794191, 327825712, 345790561, 364739883, 384727628, 405810702, 428049128, //170
        451506220, 476248760, 502347192, 529875818, 558913012, 589541445, 621848316, 655925603, 691870326, 729784819,
        769777027, 811960808, 856456260, 903390063, 952895838, 1005114529, 1060194805, 1118293480, 1179575962, 1244216724,
        1312399800, 1384319309, 1460180007, 1540197871, 1624600714, 1713628833, 1807535693, 1906588648, 2011069705, 2121276324};
    private static final int[] ClosenessTable = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
        3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
        30000};
    private static final int[] MountExpTable = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
        2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550};

    public static final int[] itemBlock = {2340000, 2049100, 4001129, 2040037, 2040006, 2040007, 2040303, 2040403, 2040506, 2040507, 2040603, 2040709, 2040710, 2040711, 2040806, 2040903, 2041024, 2041025, 2043003, 2043103, 2043203, 2043303, 2043703, 2043803, 2044003, 2044103, 2044203, 2044303, 2044403, 2044503, 2044603, 2044908, 2044815, 2044019, 2044703, 1004001, 4007008, 1004002, 5152053, 5150040};
    public static final int[] cashBlock = {5222000, 5500001, 5500002, 5600001, 5252000, 5350003, 5401000, 5490000, 5490001, 5500000, 5252001, 5252003, 5220001, 5220002, 5200000, 5200001, 5200002, 5320000, 5440000, 5201001, 5201002};

    public static final int OMOK_SCORE = 122200;
    public static final int MATCH_SCORE = 122210;
    public static final int[] blockedSkills = {4341003};
    public static int[] blockedMaps = {109050000, 200000112, 200090020, 240060200, 280030000, 280090000, 280030001, 240060201, 900090021, 950101100, 950101010};
    public static final String[] RESERVED = {"Rental"};
    public static final String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "charmEXP", "limitedLv", "imdR", "bdR", "superiorEqp", "maxSuperiorEqp", "recover", "reset", "perfectReset", "effectItemID"};

    public static int[] Equipments_Bonus = {1122017};

    public static int Equipment_Bonus_EXP(final int itemid) { // TODO : Add Time for more exp increase
        switch (itemid) {
            case 1122017:
                return 10;
        }
        return 0;
    }

    public static int getExpNeededForLevel(final int level) {
        if (level < 0 || level >= ExpTable.length) {
            return Integer.MAX_VALUE;
        }
        return ExpTable[level];
    }

    public static boolean isNoDelaySkill(int skillId) {
        return skillId == SkillType.格鬥家.蓄能激發
                || skillId == SkillType.狂狼勇士2.強化連擊
                || skillId == SkillType.閃雷悍將2.蓄能激發
                || skillId == 2111007 || skillId == 2211007 || skillId == 2311007 || skillId == 32121003 || skillId == 35121005 || skillId == 35111004 || skillId == 35121013 || skillId == 35121003 || skillId == 22150004 || skillId == 22181004 || skillId == 11101002 || skillId == 51100002 || skillId == 13101002 || skillId == 24121000 || skillId == 112001008 || skillId == 22161005 || skillId == 22161005;
    }

    public static boolean isMarrigeRing(int itemid) {
        switch (itemid) {
            case 1112300:
            case 1112301:
            case 1112302:
            case 1112303:
            case 1112304:
            case 1112305:
            case 1112306:
            case 1112307:
            case 1112308:
            case 1112309:
            case 1112310:
            case 1112311:
            case 1112315:
            case 1112316:
            case 1112317:
            case 1112318:
            case 1112319:
            case 1112320:
            case 1112803:
            case 1112806:
            case 1112807:
            case 1112808:
            case 1112809:
                return true;
        }
        return false;
    }

    public static int getClosenessNeededForLevel(final int level) {
        return ClosenessTable[level - 1];
    }

    public static int getMountExpNeededForLevel(final int level) {
        return MountExpTable[level - 1];
    }

    public static int getBookLevel(final int level) {
        return (int) ((5 * level) * (level + 1));
    }

    public static int getTimelessRequiredEXP(final int level) {
        return 70 + (level * 10);
    }

    public static int getReverseRequiredEXP(final int level) {
        return 60 + (level * 5);
    }

    public static int maxViewRangeSq() {
        return 800000; // 800 * 800
    }

    public static boolean isRecoveryIncSkill(final int id) {
        switch (id) {
            case SkillType.十字軍.魔力恢復:
            case SkillType.法師.魔力淨化:
            case SkillType.騎士.魔力恢復:
            case SkillType.聖魂劍士3.魔力恢復:
            case SkillType.刺客.恢復術:
            case SkillType.俠盜.恢復術:
                return true;
        }
        return false;
    }

    public static boolean isLinkedSkill(final int id) {
        return getLinkedSkill(id) != id;
    }

    public static int getLinkedSkill(final int id) {
        switch (id) {
            case 21110007:
            case 21110008:
                return 21110002;
            case 21120009:
            case 21120010:
                return 21120002;
            case 4321001:
                return 4321000;
        }
        return id;
    }

    public static boolean isElementAmpSkill(final int skill) {
        switch (skill) {
            case 2110001:
            case 2210001:
            case 12110001:
                return true;
        }
        return false;
    }

    public static int getMPEaterForJob(final int job) {
        switch (job) {
            case 210:
            case 211:
            case 212:
                return 2100000;
            case 220:
            case 221:
            case 222:
                return 2200000;
            case 230:
            case 231:
            case 232:
                return 2300000;
        }
        return 2100000; // Default, in case GM
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        switch (job) {
            case 4:
                // For some reason dagger/ claw is 8.. IDK
                job *= 2;
                break;
            case 3:
                job += 1;
                break;
            case 5:
                job += 11; // 16
                break;
            default:
                break;
        }
        return job;
    }

    public static boolean isPyramidSkill(final int skill) {
        switch (skill) {
            case 1020:
            case 10001020:
            case 20001020:
            case 20011020:
                return true;
        }
        return false;
    }

    public static boolean isMulungSkill(final int skill) {
        switch (skill) {
            case 1009:
            case 1010:
            case 1011:
            case 10001009:
            case 10001010:
            case 10001011:
            case 20001009:
            case 20001010:
            case 20001011:
            case 20011009:
            case 20011010:
            case 20011011:
                return true;
        }
        return false;
    }

    public static boolean isThrowingStar(final int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isBullet(final int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(final int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static boolean isSpaceItem(final int itemid) {
        switch (itemid) {
            case 5350003:
                return true;
        }
        return false;
    }

    public static boolean isOverall(final int itemId) {
        return itemId / 10000 == 105;
    }

    public static boolean isPet(final int itemId) {
        return itemId / 10000 == 500;
    }

    public static boolean isArrowForCrossBow(final int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public static boolean isArrowForBow(final int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public static boolean isMagicWeapon(final int itemId) {
        final int s = itemId / 10000;
        return s == 137 || s == 138;
    }

    public static boolean isWeapon(final int itemId) {
        return itemId >= 1300000 && itemId < 1500000;
    }

    public static MapleInventoryType getInventoryType(final int itemId) {
        MapleInventoryType type = MapleInventoryType.getByType((byte) (itemId / 1000000));
        if (type == MapleInventoryType.UNDEFINED || type == null) {
            final byte type2 = (byte) (itemId / 10000);
            switch (type2) {
                case 2:
                    type = MapleInventoryType.FACE;
                    break;
                case 3:
                case 4:
                    type = MapleInventoryType.HAIR;
                    break;
                default:
                    type = MapleInventoryType.UNDEFINED;
                    break;
            }
        }
        return type;
    }

    public static MapleWeaponType getWeaponType(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) {
            case 30:
                return MapleWeaponType.單手劍;
            case 31:
                return MapleWeaponType.單手斧;
            case 32:
                return MapleWeaponType.單手棍;
            case 33:
                return MapleWeaponType.短劍;
            case 38:
                return MapleWeaponType.長杖;
            case 37:
                return MapleWeaponType.短杖;
            case 40:
                return MapleWeaponType.雙手劍;
            case 41:
                return MapleWeaponType.雙手斧;
            case 42:
                return MapleWeaponType.雙手棍;
            case 43:
                return MapleWeaponType.槍;
            case 44:
                return MapleWeaponType.矛;
            case 45:
                return MapleWeaponType.弓;
            case 46:
                return MapleWeaponType.弩;
            case 47:
                return MapleWeaponType.拳套;
            case 48:
                return MapleWeaponType.指虎;
            case 49:
                return MapleWeaponType.火槍;
        }
        return MapleWeaponType.沒有武器;
    }

    public static boolean isShield(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public static boolean isEquip(final int itemId) {
        return itemId / 1000000 == 1;
    }

    public static boolean isCleanSlate(int itemId) {
        return itemId / 100 == 20490;
    }

    public static boolean isAccessoryScroll(int itemId) {
        return itemId / 100 == 20492;
    }

    public static boolean isChaosScroll(int itemId) {
        if (itemId >= 2049105 && itemId <= 2049110) {
            return false;
        }
        return itemId / 100 == 20491;
    }

    public static int getChaosNumber(int itemId) {
        return itemId == 2049116 ? 10 : 5;
    }

    public static boolean isOpScroll(int scrollId) {
        switch (scrollId) {
            case 2340000:
                return true;
        }
        return false;
    }

    public static boolean isAllScroll(int scrollId) {
        return scrollId / 10000 == 204;
    }

    public static boolean isEquipScroll(int scrollId) {
        return scrollId / 100 == 20493;
    }

    public static boolean isPotentialScroll(int scrollId) {
        return scrollId / 100 == 20494;
    }

    public static boolean isSpecialScroll(final int scrollId) {
        switch (scrollId) {
            case 2040727: // Spikes on show
            case 2041058: // Cape for Cold protection
                return true;
        }
        return false;
    }

    public static boolean isTwoHanded(final int itemId) {
        switch (getWeaponType(itemId)) {
            case 雙手斧:
            case 火槍:
            case 指虎:
            case 雙手棍:
            case 弓:
            case 拳套:
            case 弩:
            case 槍:
            case 矛:
            case 雙手劍:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTownScroll(final int id) {
        return id >= 2030000 && id < 2040000;
    }

    public static boolean isUpgradeScroll(final int id) {
        return id >= 2040000 && id < 2050000;
    }

    public static boolean isGun(final int id) {
        return id >= 1492000 && id < 1500000;
    }

    public static boolean isUse(final int id) {
        return id >= 2000000 && id <= 2490000;
    }

    public static boolean isSummonSack(final int id) {
        return id / 10000 == 210;
    }

    public static boolean isMonsterCard(final int id) {
        return id / 10000 == 238;
    }

    public static boolean isSpecialCard(final int id) {
        return id / 1000 >= 2388;
    }

    public static int getCardShortId(final int id) {
        return id % 10000;
    }

    public static boolean isGem(final int id) {
        return id >= 4250000 && id <= 4251402;
    }

    public static boolean isOtherGem(final int id) {
        switch (id) {
            case 4001174:
            case 4001175:
            case 4001176:
            case 4001177:
            case 4001178:
            case 4001179:
            case 4001180:
            case 4001181:
            case 4001182:
            case 4001183:
            case 4001184:
            case 4001185:
            case 4001186:
            case 4031980:
            case 2041058:
            case 2040727:
            case 1032062:
            case 4032334:
            case 4032312:
            case 1142156:
            case 1142157:
                return true; //mostly quest items
        }
        return false;
    }

    public static boolean isCustomQuest(final int id) {
        return id > 99999;
    }

    public static int getTaxAmount(final int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.008 * meso);
        }
        return 0;
    }

    public static int EntrustedStoreTax(final int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.025 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.02 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.015 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.009 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.004 * meso);
        }
        return 0;
    }

    public static short getSummonAttackDelay(final int id) {
        switch (id) {
            case 15001004: // Lightning
            case 14001005: // Darkness
            case 13001004: // Storm
            case 12001004: // Flame
            case 11001004: // Soul
            case 3221005: // Freezer
            case 3211005: // Golden Eagle
            case 3121006: // Phoenix
            case 3111005: // Silver Hawk
            case 2321003: // Bahamut
            case 2311006: // Summon Dragon
            case 2221005: // Infrit
            case 2121005: // Elquines
                return 3030;
            case 5211001: // Octopus
            case 5211002: // Gaviota
            case 5220002: // Support Octopus
                return 1230;
            case 3211002: // Puppet
            case 3111002: // Puppet
            case 1321007: // Beholder
                return 0;
        }
        return 0;
    }

    public static byte gachaponRareItem(final int id) {
        switch (id) {
            /* case 2022217: // 殘暴炎魔的御守
            case 2022221: // 緞帶肥肥的御守
            case 2022222: // 遠古精靈的御守
            case 2022223: // 企鵝王的御守*/
            case 2000000:
                return 1;
            case 2370000: // 兵法書(孫子)
            case 2370001: // 兵法書(吳子)
            case 2370002: // 兵法書(尉繚子)
            case 2370003: // 兵法書(六韜)
            case 2370004: // 兵法書(三略)
            case 2370005: // 兵法書(司馬法）
            case 2370006: // 兵法書(李衛公問對)
            case 2370007: // 兵法書(孫兵兵法)
            case 3010054: // 綿羊單人床
            case 2022483: // 加持道具
            case 2210029: // 黃金豬  變身道具
                return 2;
            case 2049100: // 渾沌卷軸60%
            case 1372039: // 炎燄短杖
            case 1372040: // 劇毒短杖
            case 1372041: // 極冰短杖
            case 1372042: // 強雷短杖
            case 1092049: // 致命劍盾
            case 1382037: // 偃月之杖
                return 3;
            case 1102084: // 粉紅蓋亞披風
            case 1102041: // 粉紅冒險家披風
            case 1102086: // 紫色蓋亞披風
            case 1102042: // 紫色冒險家披風
            case 1082149: // 褐色工作手套
            case 1082179: // 黃擊中手套
            case 1402044: // 南瓜燈籠
            case 3010065: // 粉紅海灘遮陽椅
            case 3010064: // 棕色砂兔抱枕椅
            case 3010068: // 荷葉下椅子
            case 3012001: // 營火
            case 3012002: // 檜木泡澡桶
            case 3010020: // 澎澎檜木桶
            case 3010041: // 骷髏寶座
                return 3;
        }
        return 0;
    }
    // 金寶箱獎勵
    public final static int[] goldrewards = {
        // 裝備
        1302059, 3, // 龍泉劍
        1402037, 1, // 龍背刃
        1092049, 1, // 致命劍盾
        1102041, 1, // 粉紅冒險家披風
        1432018, 3, // 藍色滑雪板
        1022047, 3, // 貓頭鷹
        1432011, 3, // 佛羅利刃
        1442020, 3, // 飛翔斧
        1382035, 3, // 怒濤之杖
        1372010, 3, // 鬼頭杖
        1332027, 3, // 霸傑之刃
        1302056, 3, // 紫凌劍
        1402005, 3, // 斬魔刀
        1472053, 3, // 克利思拳套
        1462018, 3, // 可撒之弩
        1452017, 3, // 梅杜斯
        1422013, 3, // 鐳奧釘錘
        1322029, 3, // 毀滅之鎚
        1412010, 3, // 格洛斧
        1472051, 1, // 綠色龍牙拳刃
        1482013, 1, // 龍王之爪
        1492013, 1, // 聖龍金槍
        1382050, 1, // 玄武之杖
        1382045, 1, // 火雲長杖
        1382047, 1, // 冰魄長杖
        1382048, 1, // 狂雷長杖
        1382046, 1, // 毒龍長杖
        1442018, 3, // 冷凍金槍魚
        1332032, 4, // 聖誕樹
        1482025, 3, // 粉紅色花紋游泳圈
        // 技能書
        2290096, 1, // 楓葉祝福 20
        2290049, 1, // 天怒 30
        2290041, 1, // 火流星 30
        2290047, 1, // 暴風雪 30
        2290095, 1, // 煙霧彈 30
        2290017, 1, // 鬥氣爆發 30
        2290075, 1, // 必殺狙擊 30
        2290085, 1, // 三飛閃 30
        2290116, 1, // 海鷗特戰隊 30
        // 卷軸
        2049100, 1, // 混沌卷軸60%
        2040914, 1, // 盾牌攻擊卷軸60%
        2040900, 4, // 盾牌防禦卷軸100%
        2030008, 5, // 咖啡牛奶
        // 藥水
        2000005, 10, // 超級藥水
        2000004, 10, // 特殊藥水
        // 椅子
        3010051, 1, // 公砂兔椅
        3010020, 1, // 澎澎檜木桶
        // 其他
        4001011, 4, // 猴子橡皮擦
        4001010, 4, // 蘑菇王橡皮擦
        4001009, 4, // 木妖橡皮擦
        4280000, 4}; // 金寶箱

    // 銀寶箱獎勵
    public final static int[] silverrewards = {
        // 裝備
        1002452, 3, // 黑星白頭巾
        1002455, 3, // 黑星紅頭巾
        1102082, 1, // 破舊的黑色披風
        1302049, 1, // 光線鞭子
        1102041, 1, // 粉紅冒險家披風
        1452019, 2, // 白色雷鳥弓
        1022060, 2, // 狐猴眼部裝飾
        1432011, 3, // 佛羅利刃
        1442020, 3, // 飛翔斧
        1382035, 3, // 怒濤之杖
        1372010, 3, // 鬼頭杖
        1332027, 3, // 霸傑之刃
        1302056, 3, // 紫凌劍
        1402005, 3, // 斬魔刀
        1472053, 3, // 克利思拳套
        1462018, 3, // 可撒之弩
        1452017, 3, // 梅杜斯
        1422013, 3, // 鐳奧釘錘
        1322029, 3, // 毀滅之鎚
        1412010, 3, // 格洛斧
        1002587, 3, // 黑色烤栗販帽子
        1402044, 1, // 南瓜燈籠
        1442046, 1, // 超級滑雪板
        1422031, 1, // 藍色海豹抱枕
        1332054, 3, // 閃電飛刀
        1012056, 3, // 狗鼻
        1022047, 3, // 貓頭鷹
        1442012, 3, // 天空雪板
        1442018, 3, // 冷凍金槍魚
        1432010, 3, // 奧丁手戟
        // 技能書
        2290084, 1, // 三飛閃 20
        2290048, 1, // 天怒 20
        2290040, 1, // 火流星 20
        2290046, 1, // 暴風雪 20
        2290074, 1, // 必殺狙擊 20
        2290064, 1, // 念力集中 20
        2290094, 1, // 煙霧彈 20
        2290022, 1, // 黑暗力量 20
        2290056, 1, // 弓術精通 30
        2290066, 1, // 弩術精通 30
        2290020, 1, // 鬼神之擊 20
        // 藥水
        2000005, 10, // 超級藥水
        2000004, 10, // 特殊藥水
        // 椅子
        3010041, 1, // 骷髏寶座
        3012002, 1, // 檜木泡澡桶
        // 其他
        4001116, 3, // 六角水晶項鍊
        4001012, 3, // 大幽靈橡皮擦
        4280001, 4}; // 銀寶箱
    public static int[] eventCommonReward = {
        0, 40,
        1, 10,
        4031019, 5,
        4280000, 3,
        4280001, 4,
        5490000, 3,
        5490001, 4,
        
    };
    public static int[] eventUncommonReward = {
        2, 4,
        3, 4,
        5160000, 5,
        5160001, 5,
        5160002, 5,
        5160003, 5,
        5160004, 5,
        5160005, 5,
        5160006, 5,
        5160007, 5,
        5160008, 5,
        5160009, 5,
        5160010, 5,
        5160011, 5,
        5160012, 5,
        5160013, 5,
        5240017, 5,
        5240000, 5,
        4080000, 5,
        4080001, 5,
        4080002, 5,
        4080003, 5,
        4080004, 5,
        4080005, 5,
        4080006, 5,
        4080007, 5,
        4080008, 5,
        4080009, 5,
        4080010, 5,
        4080011, 5,
        4080100, 5,
        4031019, 5,
        5121003, 5,
        5150000, 5,
        5150001, 5,
        5150002, 1,
        5150003, 1,
        5150004, 1,
        5150005, 2,
        5150006, 2,
        5150007, 2,
        5150008, 2,
        5150009, 14,
        2022459, 5,
        2022460, 5,
        2022461, 5,
        2022462, 5,
        2022463, 5,
        2450000, 2,
        5152000, 5,
        5152001, 5,
  
    };
    public static int[] eventRareReward = {
        4031019, 5,
        2049100, 5,
        2049401, 10,
        2049301, 20,
        2049400, 3,
        3010130, 5,
        3010131, 5,
        3010132, 5,
        3010133, 5,
        3010136, 5,
        3010116, 5,
        3010117, 5,
        3010118, 5,
        1112405, 1,
        1112413, 1,
        1112414, 1,
        2040211, 1,
        2040212, 1,
        2049000, 2,
        2049001, 2,
        2049002, 2,
        2049003, 2,
        1012058, 2,
        1012059, 2,
        1012060, 2,
        1012061, 2,
        1102372, 2
    };
    public static int[] eventSuperReward = {
        4031019, 5,
        4031307, 50,
        3010127, 10,
        3010128, 10,
        3010137, 10,
        2049300, 10,
        1012139, 10,
        1012140, 10,
        1012141, 10,
        1102372, 5
    };
    public static int[] tenPercent = {
        //10% scrolls
        2040002,
        2040005,
        2040026,
        2040031,
        2040100,
        2040105,
        2040200,
        2040205,
        2040302,
        2040310,
        2040318,
        2040323,
        2040328,
        2040329,
        2040330,
        2040331,
        2040402,
        2040412,
        2040419,
        2040422,
        2040427,
        2040502,
        2040505,
        2040514,
        2040517,
        2040534,
        2040602,
        2040612,
        2040619,
        2040622,
        2040627,
        2040702,
        2040705,
        2040708,
        2040727,
        2040802,
        2040805,
        2040816,
        2040825,
        2040902,
        2040915,
        2040920,
        2040925,
        2040928,
        2040933,
        2041002,
        2041005,
        2041008,
        2041011,
        2041014,
        2041017,
        2041020,
        2041023,
        2041058,
        2041102,
        2041105,
        2041108,
        2041111,
        2041302,
        2041305,
        2041308,
        2041311,
        2043002,
        2043008,
        2043019,
        2043102,
        2043114,
        2043202,
        2043214,
        2043302,
        2043402,
        2043702,
        2043802,
        2044002,
        2044014,
        2044015,
        2044102,
        2044114,
        2044202,
        2044214,
        2044302,
        2044314,
        2044402,
        2044414,
        2044502,
        2044602,
        2044702,
        2044802,
        2044809,
        2044902,
        2045302,
        2048002,
        2048005
    };
    public static int[] fishingReward = {
        0, 40, // Meso
        1, 40, // EXP
        1302021, 5, // Pico Pico Hammer
        1072238, 1, // Voilet Snowshoe
        1072239, 1, // Yellow Snowshoe
        2049100, 1, // Chaos Scroll
        //2049301, 1, // Equip Enhancer Scroll
        //2049401, 1, // Potential Scroll
        1302000, 3, // Sword
        1442011, 1, // Surfboard
        //4000517, 8, // Golden Fish
        //4000518, 25, // Golden Fish Egg
        4031627, 2, // White Bait (3cm)
        4031628, 1, // Sailfish (120cm)
        4031630, 1, // Carp (30cm)
        4031631, 1, // Salmon(150cm)
        4031632, 1, // Shovel
        4031633, 2, // Whitebait (3.6cm)
        4031634, 1, // Whitebait (5cm)
        4031635, 1, // Whitebait (6.5cm)
        4031636, 1, // Whitebait (10cm)
        4031637, 2, // Carp (53cm)
        4031638, 2, // Carp (60cm)
        4031639, 1, // Carp (100cm)
        4031640, 1, // Carp (113cm)
        4031641, 2, // Sailfish (128cm)
        4031642, 2, // Sailfish (131cm)
        4031643, 1, // Sailfish (140cm)
        4031644, 1, // Sailfish (148cm)
        4031645, 2, // Salmon (166cm)
        4031646, 2, // Salmon (183cm)
        4031647, 1, // Salmon (227cm)
        4031648, 1, // Salmon (288cm)
        4031629, 1, // Pot
        1102041, 1, // 粉披
        1102042, 1, // 紫披
        2101120, 1 // 魚怪召喚袋
    };

    public static int[] xmaxsReward = {
        20300223, 1,
        20300221, 1,
        20300275, 1
    };

    public static boolean isDragonItem(int itemId) {
        switch (itemId) {
            case 1372032:
            case 1312031:
            case 1412026:
            case 1302059:
            case 1442045:
            case 1402036:
            case 1432038:
            case 1422028:
            case 1472051:
            case 1472052:
            case 1332049:
            case 1332050:
            case 1322052:
            case 1452044:
            case 1462039:
            case 1382036:
            case 1342010:
                return true;
            default:
                return false;
        }
    }

    public static boolean isReverseItem(int itemId) {
        switch (itemId) {
            case 1002790:
            case 1002791:
            case 1002792:
            case 1002793:
            case 1002794:
            case 1082239:
            case 1082240:
            case 1082241:
            case 1082242:
            case 1082243:
            case 1052160:
            case 1052161:
            case 1052162:
            case 1052163:
            case 1052164:
            case 1072361:
            case 1072362:
            case 1072363:
            case 1072364:
            case 1072365:

            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            case 1342012:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTimelessItem(int itemId) {
        switch (itemId) {
            case 1032031: //shield earring, but technically
            case 1102172:
            case 1002776:
            case 1002777:
            case 1002778:
            case 1002779:
            case 1002780:
            case 1082234:
            case 1082235:
            case 1082236:
            case 1082237:
            case 1082238:
            case 1052155:
            case 1052156:
            case 1052157:
            case 1052158:
            case 1052159:
            case 1072355:
            case 1072356:
            case 1072357:
            case 1072358:
            case 1072359:
            case 1092057:
            case 1092058:
            case 1092059:

            case 1122011:
            case 1122012:

            case 1302081:
            case 1312037:
            case 1322060:
            case 1332073:
            case 1332074:
            case 1372044:
            case 1382057:
            case 1402046:
            case 1412033:
            case 1422037:
            case 1432047:
            case 1442063:
            case 1452057:
            case 1462050:
            case 1472068:
            case 1482023:
            case 1492023:
            case 1342011:
                return true;
            default:
                return false;
        }
    }

    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1113000;
    }// 112xxxx - pendants, 113xxxx - belts

    //if only there was a way to find in wz files -.-
    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isMarriageRing(itemid);
    }

    public static boolean isMarriageRing(int itemId) {
        switch (itemId) {
            case 1112300:
            case 1112301:
            case 1112302:
            case 1112303:
            case 1112304:
            case 1112305:
            case 1112306:
            case 1112307:
            case 1112308:
            case 1112309:
            case 1112310:
            case 1112311:
            case 1112315:
            case 1112316:
            case 1112317:
            case 1112318:
            case 1112319:
            case 1112320:
            case 1112803:

            case 1112806:
            case 1112807:
            case 1112808:
            case 1112809:

                return true;
        }
        return false;
    }

    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112804:
            case 1112810: //new
            case 1112811: //new, doesnt work in friendship?
            case 1112812: //new, im ASSUMING it's friendship cuz of itemID, not sure.
            case 1112015:
            case 1112816:
            case 1112817:
            case 1112822:
            case 1049000:
                return true;
        }
        return false;
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005:
            case 1112006:
            case 1112007:
            case 1112012:
            case 1112013: // 愛情紅線戒指
            case 1112015:

            case 1048000:
            case 1048001:
            case 1048002:
                return true;
        }
        return false;
    }

    public static int getExpForLevel(int i, int itemId) {
        if (isReverseItem(itemId)) {
            return getReverseRequiredEXP(i);
        } else if (getMaxLevel(itemId) > 0) {
            return getTimelessRequiredEXP(i);
        }
        return 0;
    }

    public static int getMaxLevel(final int itemId) {
        if (isTimelessItem(itemId)) {
            return 5;
        } else if (isReverseItem(itemId)) {
            return 3;
        } else {
            switch (itemId) {
                case 1302109:
                case 1312041:
                case 1322067:
                case 1332083:
                case 1372048:
                case 1382064:
                case 1402055:
                case 1412037:
                case 1422041:
                case 1432052:
                case 1442073:
                case 1452064:
                case 1462058:
                case 1472079:
                case 1482035:

                case 1302108:
                case 1312040:
                case 1322066:
                case 1332082:
                case 1372047:
                case 1382063:
                case 1402054:
                case 1412036:
                case 1422040:
                case 1432051:
                case 1442072:
                case 1452063:
                case 1462057:
                case 1472078:
                case 1482036:
                    return 1;

                case 1072376:
                    return 2;
            }
        }
        return 0;
    }

    public static int getStatChance() {
        return 25;
    }

    public static MonsterStatus getStatFromWeapon(final int itemid) {
        switch (itemid) {
            case 1302109:
            case 1312041:
            case 1322067:
            case 1332083:
            case 1372048:
            case 1382064:
            case 1402055:
            case 1412037:
            case 1422041:
            case 1432052:
            case 1442073:
            case 1452064:
            case 1462058:
            case 1472079:
            case 1482035:
                return MonsterStatus.ACC;
            case 1302108:
            case 1312040:
            case 1322066:
            case 1332082:
            case 1372047:
            case 1382063:
            case 1402054:
            case 1412036:
            case 1422040:
            case 1432051:
            case 1442072:
            case 1452063:
            case 1462057:
            case 1472078:
            case 1482036:
                return MonsterStatus.SPEED;
        }
        return null;
    }

    public static int getXForStat(MonsterStatus stat) {
        switch (stat) {
            case ACC:
                return -70;
            case SPEED:
                return -50;
        }
        return 0;
    }

    public static int getSkillForStat(MonsterStatus stat) {
        switch (stat) {
            case ACC:
                return 3221006;
            case SPEED:
                return 3121007;
        }
        return 0;
    }
    public final static int[] normalDrops = {
        4001009, //real
        4001010,
        4001011,
        4001012,
        4001013,
        4001014, //real
        4001021,
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007}; //end
    public final static int[] rareDrops = {
        2049100,
        2049301,
        2049401,
        2022326,
        2022193,
        2049000,
        2049001,
        2049002};
    public final static int[] superDrops = {
        2040804,
        2049400,
        2049100};

    public static int getSkillBook(final int job) {
        return 0;
    }

    public static int getSkillBookForSkill(final int skillid) {
        return getSkillBook(skillid / 10000);
    }

    public static int getMountItem(final int sourceid) {
        switch (sourceid) {
            case 5221006: //海盜船
                return 1932000;
            case 1013: //宇宙船
            case 10001014:
                return 1932001;
            case 1014: // 宇宙衝鋒
            case 10001015:
                return 1932002;
            case 1015: //宇宙光束
            case 10001016:
                return 1932007;
            case 1017: //雪吉拉騎士
            case 10001019:
            case 20001019:
                return 1932003;
            default:
                return 0;
        }
    }

    public static boolean isKatara(int itemId) {
        return itemId / 10000 == 134;
    }

    public static boolean isDagger(int itemId) {
        return itemId / 10000 == 133;
    }

    public static boolean isApplicableSkill(int skil) {
        return skil < 40000000 && (skil % 10000 < 8000 || skil % 10000 > 8003); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
        return skil >= 90000000 || (skil % 10000 >= 8000 && skil % 10000 <= 8003);
    }

    public static boolean isTablet(int itemId) {
        return itemId / 1000 == 2047;
    }

    public static int getSuccessTablet(final int scrollId, final int level) {
        switch (scrollId % 1000 / 100) {
            case 2:
                //2047_2_00 = armor, 2047_3_00 = accessory
                switch (level) {
                    case 0:
                        return 70;
                    case 1:
                        return 55;
                    case 2:
                        return 43;
                    case 3:
                        return 33;
                    case 4:
                        return 26;
                    case 5:
                        return 20;
                    case 6:
                        return 16;
                    case 7:
                        return 12;
                    case 8:
                        return 10;
                    default:
                        return 7;
                }
            case 3:
                switch (level) {
                    case 0:
                        return 70;
                    case 1:
                        return 35;
                    case 2:
                        return 18;
                    case 3:
                        return 12;
                    default:
                        return 7;
                }
            default:
                switch (level) {
                    case 0:
                        return 70;
                    case 1:
                        return 50; //-20
                    case 2:
                        return 36; //-14
                    case 3:
                        return 26; //-10
                    case 4:
                        return 19; //-7
                    case 5:
                        return 14; //-5
                    case 6:
                        return 10; //-4
                    default:
                        return 7;  //-3
                }
        }
    }

    public static int getCurseTablet(final int scrollId, final int level) {
        switch (scrollId % 1000 / 100) {
            case 2:
                //2047_2_00 = armor, 2047_3_00 = accessory
                switch (level) {
                    case 0:
                        return 10;
                    case 1:
                        return 12;
                    case 2:
                        return 16;
                    case 3:
                        return 20;
                    case 4:
                        return 26;
                    case 5:
                        return 33;
                    case 6:
                        return 43;
                    case 7:
                        return 55;
                    case 8:
                        return 70;
                    default:
                        return 100;
                }
            case 3:
                switch (level) {
                    case 0:
                        return 12;
                    case 1:
                        return 18;
                    case 2:
                        return 35;
                    case 3:
                        return 70;
                    default:
                        return 100;
                }
            default:
                switch (level) {
                    case 0:
                        return 10;
                    case 1:
                        return 14; //+4
                    case 2:
                        return 19; //+5
                    case 3:
                        return 26; //+7
                    case 4:
                        return 36; //+10
                    case 5:
                        return 50; //+14
                    case 6:
                        return 70; //+20
                    default:
                        return 100;  //+30
                }
        }
    }

    public static boolean isAccessory(final int itemId) {
        return (itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1153000) || (itemId >= 1112000 && itemId < 1113000);
    }

    public static boolean potentialIDFits(final int potentialID, final int newstate, final int i) {
        //first line is always the best
        //but, sometimes it is possible to get second/third line as well
        //may seem like big chance, but it's not as it grabs random potential ID anyway
        switch (newstate) {
            case 7:
                return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 30000 : potentialID >= 20000 && potentialID < 30000);
            case 6:
                return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 20000 && potentialID < 30000 : potentialID >= 10000 && potentialID < 20000);
            case 5:
                return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 10000 && potentialID < 20000 : potentialID < 10000);
            default:
                return false;
        }
    }

    public static boolean optionTypeFits(final int optionType, final int itemId) {
        switch (optionType) {
            case 10: //weapon
                return isWeapon(itemId);
            case 11: //any armor
                return !isWeapon(itemId);
            case 20: //shield??????????
                return itemId / 10000 == 109; //just a gues
            case 21: //pet equip?????????
                return itemId / 10000 == 180; //???LOL
            case 40: //face accessory
                return isAccessory(itemId);
            case 51: //hat
                return itemId / 10000 == 100;
            case 52: //cape
                return itemId / 10000 == 110;
            case 53: //top/bottom/overall
                return itemId / 10000 == 104 || itemId / 10000 == 105 || itemId / 10000 == 106;
            case 54: //glove
                return itemId / 10000 == 108;
            case 55: //shoe
                return itemId / 10000 == 107;
            case 90:
                return false; //half this stuff doesnt even work
            default:
                return true;
        }
    }

    public static boolean isJobFamily(final int baseJob, final int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isKOC(final int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isAran(final int job) {
        return job >= 2000 && job <= 2112 && job != 2001;
    }

    public static boolean isAdventurer(final int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCygnus(final int job) {
        return job >= 1000 && job <= 1512;
    }

    public static int getBofForJob(final int job) {
        if (isAdventurer(job)) {
            return 12;
        } else if (isKOC(job)) {
            return 10000012;
        }
        return 20000012;
    }

    public static final boolean isMountItemAvailable(final int mountid, final int jobid) {
      /*  if (jobid != 900 && mountid / 10000 == 190) {
            if (isKOC(jobid)) {
                if (mountid < 1902005 || mountid > 1902007) {
                    return false;
                }
            } else if (isAdventurer(jobid)) {
                if (mountid < 1902000 || mountid > 1902002) {
                    return false;
                }
            } else if (isAran(jobid)) {
                if (mountid < 1902015 || mountid > 1902018) {
                    return false;
                }
            }
        }*/
        return true;
    }

    public static boolean isEvanDragonItem(final int itemId) {
        return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
    }

    public static boolean canScroll(final int itemId) {
        return itemId / 100000 != 19 && itemId / 100000 != 16; //no mech/taming/dragon
    }

    public static boolean canHammer(final int itemId) {
        switch (itemId) {
            case 1122000:
            case 1122076: //ht, chaos ht
                return false;
        }
        if (!canScroll(itemId)) {
            return false;
        }
        return true;
    }

    public static int getMasterySkill(final int job) {
        if (job >= 1410 && job <= 1412) {
            return 14100000;
        } else if (job >= 410 && job <= 412) {
            return 4100000;
        } else if (job >= 520 && job <= 522) {
            return 5200000;
        }
        return 0;
    }

    public static int getExpRate_Below10(final int job) {
        if (GameConstants.isAran(job) || GameConstants.isKOC(job)) {
            return 5;
        }
        return 1;
    }

    public static int getExpRate_Quest(final int level) {
        return 1;
    }

    public static String getCashBlockedMsg(final int id) {
        switch (id) {
            case 5062000:
                //cube
                return "這個東西只能通過自由市場玩家NPC";
        }
        return "這個道具無法購買\r\n未來有機會開放購買。";
    }

    public static boolean isCustomReactItem(final int rid, final int iid, final int original) {
        if (rid == 2008006) { //orbis pq LOL
            return iid == (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 4001055);
            //4001056 = sunday. 4001062 = saturday
        } else {
            return iid == original;
        }
    }
// Custom Balloon Tips on the Login Screen
    private static final List<Balloon> lBalloon = Arrays.asList(
            new Balloon("歡迎來到" + LoginServer.getServerName(), 236, 122),
            new Balloon("禁止開外掛", 0, 276),
            new Balloon("遊戲愉快", 196, 263));

    public static List<Balloon> getBalloons() {
        return lBalloon;
    }

    public static int getJobNumber(int jobz) {
        int job = (jobz % 1000);
        if (job / 100 == 0) {
            return 0; //beginner
        } else if (job / 10 == 0) {
            return 1;
        } else {
            return 2 + (job % 10);
        }
    }

    public static boolean isCarnivalMaps(int mapid) {
        return mapid / 100000 == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100);

    }

    public static boolean isForceRespawn(int mapid) {
        switch (mapid) {
            case 925100100: //crocs and stuff
                return true;
            default:
                return false;
        }
    }

    public static int getFishingTime(boolean vip, boolean gm) {
        return gm ? 1000 : (vip ? 240000 : 360000);
    }

    public static int getCustomSpawnID(int summoner, int def) {
        switch (summoner) {
            case 9400589:
            case 9400748: //MV
                return 9400706; //jr
            default:
                return def;
        }
    }

    public static boolean canForfeit(int questid) {
        switch (questid) {
            case 20000:
            case 20010:
            case 20015: //cygnus quests
            case 20020:
                return false;
            default:
                return true;
        }
    }

    public static boolean isGMEquip(final int itemId) {
        switch (itemId) {
            case 1002140://維澤特帽
            case 1042003://維澤特西裝
            case 1062007://維澤特西褲
            case 1322013://維澤特手提包
                return true;
        }
        return false;
    }

    public static int isMonsterRiding(final int sourceid) {
        switch (sourceid) {
            case 1004:
            case 10001004:
            case 20001004:
                return sourceid;
        }
        return 0;
    }

    public static boolean isEventMap(final int mapid) {
        return (mapid >= 109010000 && mapid < 109050000) || (mapid > 109050001 && mapid < 109090000) || (mapid >= 809040000 && mapid <= 809040100);
    }

    public static boolean isExpChair(final int itemid) {

        switch (itemid / 10000) {
            case 302:
                return true;
            default:
                return false;
        }
    }

    public static boolean isFishingMap(int mapId) {
        switch (mapId) {
            case 749050500:
            case 749050501:
            case 749050502:
           
                return true;
            default:
                return false;
        }
    }

    public static boolean isChair(final int itemid) {
        return itemid / 10000 == 302;
    }

    public static int getMaxDamage(int level, int jobid, int skillid) {
        int max = 0;

        if (level < 20) {
            max += 900;
        } else if (level < 30) {
            max += 1800;
        } else if (level < 40) {
            max += 5000;
        } else if (level < 50) {
            max += 7000;
        } else if (level < 60) {
            max += 8000;
        } else if (level < 70) {
            max += 9000;
        } else if (level < 80) {
            max += 10000;
        } else if (level < 90) {
            max += 11000;
        } else if (level < 100) {
            max += 12000;
        } else if (level < 110) {
            max += 13000;
        } else {
            max = 200000;
        }
        if (isCygnus(jobid)) {
            max += 1000;
        }
        switch (skillid) {
            case 21110004:
                max *= 3;
                break;
            case 1111005:
                max *= 2;
                break;
            case 21100004:
            case 4211006:
                max *= 1.5;
                break;
            default:
                break;
        }
        return max;
    }

    public static boolean isElseSkill(int id) {
        switch (id) {
            case 10001009:
            case 20001009:
            case 1009:   // 武陵道場技能
            case 1020:   // 金字塔技能
            case 10001020:
            case 20001020:
            case 3221001:// 光速神弩
            case 4211006:// 楓幣炸彈
                return true;
        }
        return false;
    }

    public static boolean Novice_Skill(int skill) {
        switch (skill) {
            case 1000://新手 蝸牛殼
            case 10001000://新手 蝸牛殼
            case 20001000://狂郎  蝸牛殼
                return true;
        }
        return false;
    }

    private static double getAttackRangeBySkill(AttackInfo attack) {
        double defRange = 0;
        switch (attack.skill) {
            case 21120006: // 極冰暴風
                defRange = 800000.0;
                break;
            case 2121007: // 火流星
            case 2221007: // 暴風雪
            case 2321008: // 天怒
                defRange = 750000.0;
                break;
            case 2221006: // 閃電連擊
            case 3101005: // 炸彈箭
            case 21101003:// 強化連擊
                defRange = 600000.0;
                break;
            case 15111006:// 閃光擊
                defRange = 500000.0;
                break;
            case 12111006:// 火風暴
            case 2111003: // 致命毒
                defRange = 400000.0;
                break;
            case 5221004:// 迅雷
            case 4001344: // 雙飛斬
            case 2101004: // 火焰箭 
            case 1121008: // 無雙劍舞
                defRange = 350000.0;
                break;
            case 2211002: // 冰風暴
                defRange = 300000.0;
                break;
            case 5110001: // 蓄能激發
            case 2311004: // 聖光
            case 2211003: // 落雷凝聚
            case 2001005: // 魔力爪
                defRange = 250000.0;
                break;
            case 2321007: // 天使之箭
                defRange = 200000.0;
                break;
            case 20001000: // 蝸牛投擲術
            case 1000: // 蝸牛投擲術
                defRange = 180000.0;
                break;
            default:
                break;
        }
        return defRange;
    }

    private static double getAttackRangeByWeapon(MapleCharacter chr) {
        IItem weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item.getItemId());
        switch (weapon) {
            case 槍:       // 矛
                return 200000;
            case 拳套:     // 拳套
                return 250000;
            case 火槍:     // 火槍
            case 弩:       // 弩
            case 弓:       // 弓
                return 220000;
            case 矛:
                return 180000;
            default:
                return 100000;
        }
    }

    public static double getAttackRange(MapleCharacter chr, MapleStatEffect def, AttackInfo attack) {
        int rangeInc = chr.getStat().defRange;// 處理遠程職業
        double base = 450.0;// 基礎
        double defRange = ((base + rangeInc) * (base + rangeInc));// 基礎範圍
        if (def != null) {
            // 計算範圍((maxX * maxX) + (maxY * maxY)) + (技能範圍 * 技能範圍))
            defRange += def.getMaxDistanceSq() + (def.getRange() * def.getRange());
            if (getAttackRangeBySkill(attack) != 0) {// 直接指定技能範圍
                defRange = getAttackRangeBySkill(attack);
            }
        } else {// 普通攻擊
            defRange = getAttackRangeByWeapon(chr);// 從武器獲取範圍
        }
        return defRange;
    }

    public static short getAttackDelay(MapleCharacter chr, int id) {
        short delay = 0;
        IItem weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);

        if (id != 0) {
            if (getAttackDelayBySkill(id) != 0) {
                delay = getAttackDelayBySkill(id);
                if (weapon_item != null) {
                    switch (id) {
                        case SkillType.劍士.魔天一擊:
                        case SkillType.聖魂劍士1.魔天一擊:
                            switch (weapon_item.getItemId()) {
                                case 1332010:
                                case 1322006:
                                case 1322004:
                                    delay = 660;
                                    break;
                            }
                            break;
                    }
                }
            }
        } else {// 普通攻擊
            delay = getAttackDelayByWeapon(chr);
        }

        delay = handleAttackDelayBuff(chr, delay);

        if (chr.getBuffedValue(MapleBuffStat.BOOSTER) != null || (chr.getBuffedValue(MapleBuffStat.SPEED_INFUSION) != null)) {
            if (id != 0) {
                if (getAttackDelayBySkillAfterBuff(id) != 0) {
                    delay = getAttackDelayBySkillAfterBuff(id);
                }
            } else// 普通攻擊
            if (getAttackDelayAfterBuff(chr) != 0) {
                delay = getAttackDelayAfterBuff(chr);
            }
        }

        // 強化連擊
        if (id == 21101003 || id == 5110001) {
            delay = 0;
        }

        return delay;
    }

    public static short getAttackDelayBySkill(int id) {
        switch (id) {
            case SkillType.槍手.炸彈投擲:
                return 60;
            case SkillType.箭神.暴風神射:
            case SkillType.暗影神偷.致命暗殺:
            case SkillType.槍神.瞬迅雷:
            case SkillType.槍手.脫離戰場:
            case SkillType.破風使者3.暴風神射:
                return 120;
            case SkillType.破風使者2.暴風射擊:
                return 360;
            case SkillType.海盜.雙子星攻擊:
                return 450;
            case SkillType.閃雷悍將3.損人利己:
            case SkillType.英雄.究極突刺:
            case SkillType.聖騎士.究極突刺:
            case SkillType.黑騎士.究極突刺:
            case SkillType.海盜.衝擊拳:
            case SkillType.閃雷悍將1.衝擊拳:
            case SkillType.格鬥家.能量暴擊:
            case SkillType.閃雷悍將2.能量暴擊:
            case SkillType.聖魂劍士1.魔天一擊:
            case SkillType.聖魂劍士1.劍氣縱橫:
                return 570;
            case SkillType.烈焰巫師2.火焰箭:
            case SkillType.破風使者3.箭雨:
            case SkillType.暗夜行者1.雙飛斬:
            case SkillType.暗殺者.楓幣攻擊:
            case SkillType.盜賊.劈空斬:
            case SkillType.暗夜行者3.風魔手裏劍:
            case SkillType.閃雷悍將2.狂暴衝擊:
            case SkillType.聖魂劍士3.雙連斬:
            case SkillType.僧侶.群體治癒:
                return 600;
            case SkillType.劍士.劍氣蹤橫:
            case SkillType.夜使者.三飛閃:
            case SkillType.海盜.旋風斬:
            case SkillType.閃雷悍將1.旋風斬:
            case SkillType.盜賊.雙飛斬:
            case SkillType.刺客.吸血術:
            case SkillType.俠盜.妙手術:
            case SkillType.神偷.分身術:
                return 660;
            case SkillType.暗殺者.風魔手裏劍:
            case SkillType.槍神.海盜加農炮:
            case SkillType.槍手.散射:
                return 690;
            case SkillType.劍士.魔天一擊:
            case SkillType.騎士.屬性攻擊:
            case SkillType.龍騎士.龍之獻祭:
            case SkillType.遊俠.致命箭:
                return 720;
            case SkillType.聖騎士.騎士衝擊波:
            case SkillType.龍騎士.無雙矛:
            case SkillType.龍騎士.無雙槍:
            case SkillType.弓箭手.二連箭:
            case SkillType.弓箭手.斷魂箭:
            case SkillType.獵人.炸彈箭:
            case SkillType.獵人.強弓:
            case SkillType.遊俠.四連箭:
            case SkillType.遊俠.烈火箭:
            case SkillType.遊俠.箭雨:
            case SkillType.狙擊手.四連箭:
            case SkillType.神槍手.三連發:
            case SkillType.箭神.龍魂之箭:
            case SkillType.槍手.偽裝射擊:
            case SkillType.破風使者1.二連箭:
            case SkillType.破風使者3.四連箭:
            case SkillType.英雄.無雙劍舞:
            case SkillType.烈焰巫師1.魔力爪:
            case SkillType.烈焰巫師3.火風暴:
                return 750;
            case SkillType.冰雷大魔導士.閃電連擊:
            case SkillType.弩弓手.穿透之箭:
            case SkillType.狙擊手.寒冰箭:
            case SkillType.狙擊手.升龍弩:
            case SkillType.神射手.必殺狙擊:
            case SkillType.俠盜.迴旋斬:
            case SkillType.槍神.精準砲擊:
                return 780;
            case SkillType.冒險之技.嫩寶丟擲術:
            case SkillType.貴族.嫩寶丟擲術:
            case SkillType.傳說.嫩寶丟擲術:
            case SkillType.法師.魔力爪:
            case SkillType.法師.魔靈彈:
            case SkillType.火毒巫師.毒霧:
            case SkillType.火毒巫師.火焰箭:
            case SkillType.冰雷巫師.冰錐術:
            case SkillType.冰雷巫師.電閃雷鳴:
            case SkillType.冰雷大魔導士.寒冰地獄:
            case SkillType.火毒大魔導士.炎靈地獄:
            case SkillType.火毒大魔導士.劇毒麻痺:
            case SkillType.閃雷悍將3.鯨噬:
            case SkillType.僧侶.神聖之箭:
            case SkillType.主教.天使之箭:
            case SkillType.拳霸.閃連殺:
                return 810;
            case SkillType.神偷.楓幣炸彈:
                return 840;
            case SkillType.冰雷大魔導士.核爆術:
            case SkillType.火毒大魔導士.核爆術:
            case SkillType.主教.核爆術:
            case SkillType.槍神.海盜魚雷:
            case SkillType.聖魂劍士2.靈魂之刃:
                return 870;
            case SkillType.龍騎士.槍連擊:
            case SkillType.龍騎士.矛連擊:
            case SkillType.冰雷魔導士.冰雷合擊:
            case SkillType.火毒魔導士.火毒合擊:
            case SkillType.打手.迴旋肘擊:
            case SkillType.神槍手.指定攻擊:
            case SkillType.破風使者3.疾風光速神弩:
            case SkillType.神射手.光速神弩:
                return 900;
            case SkillType.拳霸.鬥神降世:
                return 930;
            case SkillType.破風使者3.疾風掃射:
                return 960;
            case SkillType.暗夜行者2.吸血:
            case SkillType.夜使者.挑釁:
            case SkillType.暗影神偷.挑釁:
            case SkillType.閃雷悍將2.蓄能激發:
                return 1020;
            case SkillType.冰雷魔導士.冰風暴:
            case SkillType.祭司.聖光:
            case SkillType.神槍手.火焰噴射:
            case SkillType.神槍手.寒霜噴射:
            case SkillType.神偷.落葉斬:
            case SkillType.烈焰巫師2.火柱:
            case SkillType.狂狼勇士1.雙重攻擊:
                return 1050;
            case SkillType.狂狼勇士3.雙重攻擊:
                return 1131;
            case SkillType.拳霸.元氣彈:
            case SkillType.打手.昇龍拳:
                return 1140;
            case SkillType.格鬥家.損人利己:
            case SkillType.閃雷悍將3.閃連殺:
                return 1170;
            case SkillType.聖魂劍士3.靈魂突刺:
                return 1230;
            case SkillType.烈焰巫師3.火牢術屏障:
            case SkillType.狂狼勇士3.伺機攻擊:
            case SkillType.狂狼勇士4.三重攻擊:
                return 1260;
            case SkillType.狂狼勇士4.雙重攻擊:
                return 1290;
            case SkillType.十字軍.虎咆哮:
            case SkillType.冰雷魔導士.落雷凝聚:
            case SkillType.拳霸.閃索命:
                return 1320;
            case SkillType.龍騎士.龍咆哮:
                return 1410;
            case SkillType.暗影神偷.瞬步連擊:
            case SkillType.夜使者.忍術風影:
                return 1440;
            case SkillType.火毒魔導士.致命毒霧:
            case SkillType.格鬥家.蓄能激發:
            case SkillType.狂狼勇士2.強化連擊:
            case SkillType.閃雷悍將3.衝擊波:
                return 1500;
            case SkillType.狂狼勇士2.三重攻擊:
            case SkillType.打手.狂暴衝擊:
                return 1560;
            case SkillType.火毒魔導士.末日烈焰:
                return 1650;
            case SkillType.狂狼勇士3.挑怪:
                return 1710;
            case SkillType.拳霸.魔龍降臨:
            case SkillType.格鬥家.衝擊波:
                return 1860;
            case SkillType.狂狼勇士4.終極之矛:
                return 1890;
            case SkillType.狂狼勇士2.突刺之矛:
                return 2250;
            case SkillType.狂狼勇士2.猛擲之矛:
                return 2610;
            case SkillType.主教.天怒:
                return 2700;
            case SkillType.狂狼勇士4.極冰暴風:
                return 2820;
            case SkillType.聖騎士.鬼神之擊:
                return 2910;
            case SkillType.槍神.海鷗特戰隊:
            case SkillType.拳霸.閃爆破:
                return 2940;
            case SkillType.冒險之技.地火天爆:
            case SkillType.貴族.地火天爆:
            case SkillType.傳說.地火天爆:
            case SkillType.烈焰巫師3.火流星:
                return 3060;
            case SkillType.冰雷大魔導士.暴風雪:
            case SkillType.火毒大魔導士.火流星:
                return 3480;
            case SkillType.冒險之技.竹竿天擊:
            case SkillType.貴族.竹竿天擊:
            case SkillType.傳說.竹竿天擊:
                return 3900;
        }
        return 0; // 預設值
    }

    private static short getAttackDelayAfterBuff(MapleCharacter chr) {
        // TODO : 普通攻擊因為處理BUFF後Delay值異常重新調整
        short AtkDelay = 0;
        IItem weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item.getItemId());
        String name = weapon.name();
        switch (name) {
            case "指虎":
            case "火槍":
            case "弩":
            case "弓":
                AtkDelay = 570;
                break;
        }
        if (weapon_item != null && weapon_item.getItemId() == 1492037) {
            AtkDelay = 540;
        }
        return AtkDelay;
    }

    public static short getAttackDelayBySkillAfterBuff(int id) {
        // BUFF後Delay值異常的技能重新調整
        switch (id) {
            case SkillType.箭神.暴風神射:
            case SkillType.槍神.瞬迅雷:
            case SkillType.破風使者3.暴風神射:
                return 120;
            case SkillType.海盜.雙子星攻擊:
                return 390;
            case SkillType.英雄.究極突刺:
            case SkillType.聖騎士.究極突刺:
            case SkillType.黑騎士.究極突刺:
            case SkillType.格鬥家.損人利己:
                return 450;
            case SkillType.遊俠.四連箭:
            case SkillType.槍神.海盜加農炮:
            case SkillType.夜使者.三飛閃:
            case SkillType.暗夜行者3.三飛閃:
            case SkillType.暗夜行者1.雙飛斬:
            case SkillType.盜賊.雙飛斬:
                return 600;
            case SkillType.英雄.無雙劍舞:
            case SkillType.閃雷悍將3.閃光擊:
                return 630;
            case SkillType.弓箭手.二連箭:
            case SkillType.海盜.旋風斬:
                return 660;
            case SkillType.神槍手.指定攻擊:
                return 780;
            case SkillType.冰雷魔導士.落雷凝聚:
                return 1140;
            case SkillType.狂狼勇士2.強化連擊:
            case SkillType.格鬥家.蓄能激發:
                return 1500;
        }
        return 0; // 預設值
    }

    private static short getAttackDelayByWeapon(MapleCharacter chr) {
        // 取各類武器中最快的測試取最低值
        short AtkDelay;
        IItem weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item.getItemId());
        String name = weapon.name();
        switch (name) {
            case "拳套":
                AtkDelay = 540;
                break;
            case "雙手劍":
            case "單手劍":
            case "單手棍":
            case "長杖":
            case "短劍":
                AtkDelay = 570;
                break;
            case "單手斧":
            case "短杖":
                AtkDelay = 630;
                break;
            case "矛":
            case "火槍":
            case "弓":
            case "雙手棍":
            case "指虎":
            case "槍":
            case "弩":
            case "沒有武器":
                AtkDelay = 660;
                break;
            case "雙手斧":
                AtkDelay = 720;
                break;
            default:
                AtkDelay = 690;
                break;
        }
        if (weapon_item != null) {
            switch (weapon_item.getItemId()) {
                case 1492037:
                    AtkDelay = 630;
                    break;
            }
        }
        return AtkDelay;
    }

    public static short handleAttackDelayBuff(MapleCharacter chr, short AtkDelay) {
        boolean booster = false;
        // 由於Timer異常，Buff可能判斷錯誤
        if (chr.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
            AtkDelay /= 6;// 使用這Buff之後 tickcount - lastAttackTickCount 可以為0...
        }
        // 攻擊加速
        if (chr.getBuffedValue(MapleBuffStat.BOOSTER) != null) {
            if (chr.isWarrior()) {// 720 -> 600 (1001004)
                AtkDelay /= 1.21;
            } else if (chr.isMage()) {// 810 -> 720 (2001004)
                AtkDelay /= 1.14;
            } else if (chr.isBowman()) {// 810 -> 720 (3101005)
                AtkDelay /= 1.14;
            } else if (chr.isThief()) {//720 -> 600 (4001344)
                AtkDelay /= 1.21;
            } else if (chr.isPirate()) {// 570 - > 510 (5001001)
                AtkDelay /= 1.15;
            } else {
                AtkDelay /= 1.21;
            }
            booster = true;
        }
        // 最終極速
        if (chr.getBuffedValue(MapleBuffStat.SPEED_INFUSION) != null) {
            if (chr.isWarrior()) {// 720 -> 600 (1001004)
                if (!booster) {
                    AtkDelay /= 1.21;
                }
            } else if (chr.isMage()) {// 900 -> 810 (2111006)
                if (!booster) {
                    AtkDelay /= 1.14;
                }
            } else if (chr.isBowman()) {// 810 -> 720 (3101005)
                if (!booster) {
                    AtkDelay /= 1.14;
                }
            } else if (chr.isThief()) {//720 -> 600 (4001344)
                if (!booster) {
                    AtkDelay /= 1.21;
                }
            } else if (chr.isPirate()) {// 570 - > 510 (5001001)
                AtkDelay /= 1.15;
            } else {
                AtkDelay /= 1.21;
            }
        }
        return AtkDelay;
    }

    public static boolean CanAcceptQuest(final int questid) {
        switch (questid) {
            case 3096:
            case 3619:
            case 6006:
            case 6220:
            case 6221:
            case 6290:
            case 6294:
            case 6310:
            case 6340:
            case 6350:
            case 6380:
            case 6390:
            case 8248:
            case 8249:
            case 8511:
            case 8512:
            case 8613:
            case 8614:
            case 8615:
            case 8627:
            case 4949:
            case 8184:
            case 8185:
                return true;
        }
        return false;
    }

    public static boolean CanDoneQuest(final int questid) {
        switch (questid) {
            case 29507:
            case 3083:
            case 3096:
            case 8184:
            case 8185:
                return true;
        }
        return false;
    }
    
    public static boolean spquest(final int questid) {
        switch (questid) {
            case 2001:
            case 8511:
            case 21301:
            case 21302:
            case 3083:
            case 3096:
                return true;
        }
        return false;
    }

    public static short getSlotMax(int itemId) {
        switch (itemId) {
            case 4030003:
            case 4030004:
            case 4030005:
                return 1;
            case 4001168:
            case 4031306:
            case 4031307:
            case 3993000:
            case 3993002:
            case 3993003:
                return 100;
            case 5220010:
            case 5220013:
            case 2050004:
            case 2000005:
                return 1000;
            case 5220020:
                return 2000;
            case 2340000:
            case 2041200:
            case 4031997:
            case 4032385:
                return 1;
        }
        return 0;
    }

    public static boolean isDropRestricted(int itemId) {
        return itemId == 3012000 || itemId == 4030004 || itemId == 1052098 || itemId == 1052202;
    }

    public static boolean isPickupRestricted(int itemId) {
        return itemId == 4030003 || itemId == 4030004;
    }

    public static short getStat(int itemId, int def) {
        switch (itemId) {
            //case 1002419:
            //    return 5;
            case 1002959:
                return 25;
            case 1142002:
                return 10;
            case 1122121:
                return 7;
        }
        return (short) def;
    }

    public static short getHpMp(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 500;
            case 1142002:
            case 1002959:
                return 1000;
        }
        return (short) def;
    }

    public static short getATK(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 3;
            case 1002959:
                return 4;
            case 1142002:
                return 9;
        }
        return (short) def;
    }

    public static short getDEF(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 250;
            case 1002959:
                return 500;
        }
        return (short) def;
    }

    public static class Balloon {

        public int x;
        public int y;
        public String msg;

        public Balloon(String sMessage, int nX, int nY) {
            this.msg = sMessage;
            this.x = nX;
            this.y = nY;
        }
    }

    public static void addBlockedNpc(int id) {
        if (!blockedNpcs.contains(id)) {
            blockedNpcs.add(id);
        }
    }

    public static void removeBlockedNpc(int id) {
        if (blockedNpcs.contains(id)) {
            blockedNpcs.remove(blockedNpcs.indexOf(id));
        }
    }

    public static boolean isBlockedNpc(int id) {
        return blockedNpcs.contains(id);
    }
}
