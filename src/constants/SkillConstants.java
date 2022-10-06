/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import constants.skills.SkillType;
import client.MapleCharacter;
import client.MapleJob;

/**
 *
 * @author pungin
 */
public class SkillConstants {

    public static int getJobBySkill(int skillId) {
        int result = skillId / 10000;
        if (skillId / 10000 == 8000) {
            result = skillId / 100;
        }
        return result;
    }

    public static boolean isApplicableSkill(int skil) {
        return ((skil < 80000000 || skil >= 100000000) && (skil % 10000 < 8000 || skil % 10000 > 8006) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

    public static boolean isAngel(int skillId) {
        if (MapleJob.isBeginner(skillId / 10000) || skillId / 100000 == 800) {
            switch (skillId % 10000) {
                case 1085: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1087: // 黑天使 [等級上限：1]\n召喚被黑天使祝福封印的大天使。
                case 1090: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1179: // 白色天使 [最高等級： 1]\n召喚出被封印的聖潔天使。
                case 86: // 大天使祝福 [等級上限：1]\n得到大天使的祝福。
                    return true;
            }
        }
        switch (skillId) {
            case 80000052: // 恶魔之息 获得恶魔的力量，攻击力和魔法攻击力增加6，HP、MP增加5%，可以和其他增益叠加。
            case 80000053: // 恶魔召唤 获得恶魔的力量，攻击力和魔法攻击力增加13，HP、MP增加10%，可以和其他增益叠加。
            case 80000054: // 恶魔契约 获得恶魔的力量，攻击力和魔法攻击力增加15，HP、MP增加20%，可以和其他增益叠加。
            case 80000086: // 戰神祝福 [等級上限：1]\n得到戰神的祝福。
            case 80001154: // 白色天使 [最高等級：1]\n召喚被白天使的祝福封印的白天使。
            case 80001262: // 戰神祝福 [等級上限：1]\n召喚戰神
            case 80001518: // 元素瑪瑙 召喚瑪瑙戒指中的#c元素瑪瑙#.
            case 80001519: // 火焰瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001520: // 閃電瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001521: // 冰凍瑪瑙 召喚瑪瑙戒指中的#c冰凍瑪瑙#.
            case 80001522: // 大地瑪瑙 召喚瑪瑙戒指中的#c大地瑪瑙#.
            case 80001523: // 黑暗瑪瑙 召喚瑪瑙戒指中的#c黑暗瑪瑙#.
            case 80001524: // 神聖瑪瑙 召喚瑪瑙戒指中的#c神聖瑪瑙#.
            case 80001525: // 火精靈瑪瑙 召喚瑪瑙戒指中的#c火精靈瑪瑙#.
            case 80001526: // 電子瑪瑙 召喚瑪瑙戒指中的#c電子瑪瑙#.
            case 80001527: // 水精靈瑪瑙 召喚瑪瑙戒指中的#c水精靈瑪瑙#.
            case 80001528: // 地精靈瑪瑙 召喚瑪瑙戒指中的#c地精靈瑪瑙#.
            case 80001529: // 惡魔瑪瑙 召喚瑪瑙戒指中的#c惡魔瑪瑙#.
            case 80001530: // 天使瑪瑙 召喚瑪瑙戒指中的#c天使瑪瑙#.
            case 80001715: // 元素瑪瑙
            case 80001716: // 火焰瑪瑙
            case 80001717: // 閃電瑪瑙
            case 80001718: // 冰凍瑪瑙
            case 80001719: // 大地瑪瑙
            case 80001720: // 黑暗瑪瑙
            case 80001721: // 神聖瑪瑙
            case 80001722: // 火精靈瑪瑙
            case 80001723: // 電子精靈瑪瑙
            case 80001724: // 水精靈瑪瑙
            case 80001725: // 地精靈瑪瑙
            case 80001726: // 惡魔瑪瑙
            case 80001727: // 天使瑪瑙
                return true;
        }
        return false;
    }

    public static boolean isMoveSkill(int id) {
//        if (is二段跳(id)) {
//            return true;
//        }
        switch (id) {
            case SkillType.槍手.脫離戰場:
            case SkillType.火毒巫師.瞬間移動:
            case SkillType.冰雷巫師.瞬間移動:
            case SkillType.僧侶.瞬間移動:
            case SkillType.烈焰巫師2.瞬間移動:
            case SkillType.管理者.瞬間移動:
            case SkillType.聖魂劍士2.靈魂迅移:
                // case SkillType.狂狼勇士1.戰鬥衝刺:
                return true;
        }

        return false;
    }

    public static boolean is二段跳(int id) {
        boolean x = false;
        switch (id) {
            case 4111006:
            case 14101004:
                x = true;
                break;
        }
        return x;
    }

    public static int getMaxDamage(final MapleCharacter chr, int skillid) {
        int max = 200000;
        int level = chr.getLevel();
        /*    if (player.getLevel() < 4) {
                                atk = 50;
                            } else if (player.getLevel() < 6) {
                                atk = 70;
                            } else if (player.getLevel() < 9) {
                                atk = 150;
                            } else if (player.getLevel() < 10) {
                                atk = 250;
                            } else if (player.getLevel() <= 15) {
                                atk = 600;
                            } else if (player.getLevel() <= 20) {
                                atk = 1000;
                            } else if (player.getLevel() <= 30) {
                                atk = 2500;
                            } else if (player.getLevel() <= 60) {
                                atk = 8000;
                            }*/
        if (level < 4) {
            max = 80;
        } else if (level < 9) {
            max = 150;
        } else if (level < 10) {
            max = 250;
        } else if (level <= 15) {
            max = 600;
        } else if (level <= 20) {
            max = 1000;
        } else if (level <= 25) {
            max = 1500;
        } else if (level <= 30) {
            max = 2200;
        } else if (level <= 35) {
            max = 3200;
        } else if (level <= 40) {
            max = 4000;
        } else if (level <= 50) {
            max = 7000;
        } else if (level <= 60) {
            max = 8000;
        }
        switch (skillid) {
            case SkillType.劍士.魔天一擊:
            case SkillType.聖魂劍士1.魔天一擊:
                max *= 1.2;
        }
        return max;
    }

    public static boolean isMagicAttack(int id) {
        boolean ret = false;
        switch (id) {
            case 1000:
            case 2001004:
            case 2001005:
            case 2101004:
            case 2101005:
            case 2111002:
            case 2111003:
            case 2111006:
            case 2121001:
            case 2121003:
            case 2121006:
            case 2121007:
            case 2201004:
            case 2201005:
            case 2211002:
            case 2211003:
            case 2211006:
            case 2221001:
            case 2221003:
            case 2221006:
            case 2221007:
            case 2301002:
            case 2301005:
            case 2311004:
            case 2321001:
            case 2321007:
            case 2321008:
            case 10001000:
            case 12101002:
            case 12001003:
            case 12101006:
            case 12111003:
            case 12111005:
            case 12111006:
            case 20001000:
                ret = true;
            default:
                break;
        }
        return ret;
    }

    public static boolean isRangedAttack(int id) {
        boolean ret = false;
        switch (id) {
            case 0:
            case 3001004:
            case 3001005:
            case 3100001:
            case 3101005:
            case 3110001:
            case 3111003:
            case 3111004:
            case 3111006:
            case 3121003:
            case 3121004:
            case 3200001:
            case 3201005:
            case 3210001:
            case 3211003:
            case 3211004:
            case 3211006:
            case 3221001:
            case 3221003:
            case 3221007:
            case 4001344:
            case 4101005:
            case 4111004:
            case 4111005:
            case 4121003:
            case 4121007:
            case 4221003:
            case 5001003:
            case 5121002:
            case 5201001:
            case 5201006:
            case 5210000:
            case 5211004:
            case 5211005:
            case 5211006:
            case 5220011:
            case 5221004:
            case 5221007:
            case 5221008:
            case 5221009:
            case 11101004:
            case 13001003:
            case 13101002:
            case 13101005:
            case 13111000:
            case 13111001:
            case 13111002:
            case 13111006:
            case 13111007:
            case 14001004:
            case 14101006:
            case 14111002:
            case 14111005:
            case 15111006:
            case 15111007:
            case 21100004:
            case 21110004:
            case 21120006:
                ret = true;
                break;
            default:
                break;
        }
        return ret;
    }

    public static boolean isCloseRangedAttack(int id) {
        boolean ret = false;
        switch (id) {
            case 0:
            case 1009:
            case 1020:
            case 1001004:
            case 1001005:
            case 1100002:
            case 1100003:
            case 1111003:
            case 1111004:
            case 1111005:
            case 1111006:
            case 1111008:
            case 1121006:
            case 1121008:
            case 1200002:
            case 1200003:
            case 1211002:
            case 1221007:
            case 1221009:
            case 1221011:
            case 1300002:
            case 1300003:
            case 1311001:
            case 1311002:
            case 1311003:
            case 1311004:
            case 1311005:
            case 1311006:
            case 1321003:
            case 3101003:
            case 3201003:
            case 4001002:
            case 4001334:
            case 4121008:
            case 4201004:
            case 4201005:
            case 4211002:
            case 4211004:
            case 4211006:
            case 4221001:
            case 4221007:
            case 5001001:
            case 5001002:
            case 5101002:
            case 5101003:
            case 5101004:
            case 5110001:
            case 5111002:
            case 5111004:
            case 5111006:
            case 5121001:
            case 5121004:
            case 5121005:
            case 5121007:
            case 5201002:
            case 5201004:
            case 5221003:
            case 5001006:
            case 10001009:
            case 10001020:
            case 11001002:
            case 11001003:
            case 11101002:
            case 11111003:
            case 11111004:
            case 11111006:
            case 14001002:
            case 14111006:
            case 15001001:
            case 15001002:
            case 15100004:
            case 15101003:
            case 15101004:
            case 15101005:
            case 15111001:
            case 15111003:
            case 15111004:
            case 20000014:
            case 20000015:
            case 20000016:
            case 20001009:
            case 20001020:
            case 20011020:
            case 21000002:
            case 21100001:
            case 21100002:
            case 21101003:
            case 21110003:
            case 21110006:
            case 21110007:
            case 21110008:
            case 21120005:
            case 21120009:
            case 21120010:
                ret = true;
                break;
            default:
                break;
        }
        return ret;
    }

    public static boolean isSpecialMove(int id) {
        boolean ret = false;
        switch (id) {
            default:
                break;
        }
        return ret;
    }

    public static boolean is究極突刺(int id) {
        switch (id) {
            case SkillType.英雄.究極突刺:
            case SkillType.聖騎士.究極突刺:
            case SkillType.黑騎士.究極突刺:
                return true;
        }
        return false;
    }

    public static boolean is瞬間移動(int id) {
        boolean ret = false;
        switch (id) {
            case SkillType.火毒巫師.瞬間移動:
            case SkillType.冰雷巫師.瞬間移動:
            case SkillType.僧侶.瞬間移動:
            case SkillType.烈焰巫師2.瞬間移動:
            case SkillType.管理者.瞬間移動:
                ret = true;
                break;
        }
        return ret;
    }

}
