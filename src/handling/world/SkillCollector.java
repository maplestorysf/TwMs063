package handling.world;

import constants.SkillConstants;
import java.util.LinkedList;
import server.Timer;
import tools.FileoutputUtil;

/**
 *
 * @author Windyboy
 */
public class SkillCollector {

    private final static SkillCollector instance = new SkillCollector();
    private final LinkedList<Integer> CloseRangeAttack = new LinkedList<>();
    private final LinkedList<Integer> RangeAttack = new LinkedList<>();
    private final LinkedList<Integer> MagicAttack = new LinkedList<>();

    public static SkillCollector getInstance() {
        return instance;
    }

    public static class run implements Runnable {

        @Override
        public void run() {
            getInstance().outputList();
        }
    }

    public void init() {
        Timer.WorldTimer.getInstance().register(new run(), 3 * 60 * 60 * 1000);
    }

    public void outputList() {
        String msgs = "\r\n";
        for (int skill : CloseRangeAttack) {
            msgs += ", " + skill;
        }
        if (!CloseRangeAttack.isEmpty()) {
            FileoutputUtil.logToFile("logs/data/攻擊分類/近距離攻擊_" + FileoutputUtil.getChineseData() + ".txt", msgs);
        }
        msgs = "\r\n";
        for (int skill : RangeAttack) {
            msgs += ", " + skill;
        }
        if (!RangeAttack.isEmpty()) {
            FileoutputUtil.logToFile("logs/data/攻擊分類/遠距離攻擊_" + FileoutputUtil.getChineseData() + ".txt", msgs);
        }
        msgs = "\r\n";
        for (int skill : MagicAttack) {
            msgs += ", " + skill;
        }
        if (!MagicAttack.isEmpty()) {
            FileoutputUtil.logToFile("logs/data/攻擊分類/魔法攻擊_" + FileoutputUtil.getChineseData() + ".txt", msgs);
        }
    }

    public void addSkill(int type, int skill) {
        boolean Exist = false;
        switch (type) {
            case 1:
                Exist = SkillConstants.isCloseRangedAttack(skill);
                break;
            case 2:
                Exist = SkillConstants.isRangedAttack(skill);
                break;
            case 3:
                Exist = SkillConstants.isMagicAttack(skill);
                break;
        }
        if (!Exist) {
            if (!getSelectType(type).contains(skill)) {
                getSelectType(type).add(skill);
            }
        }
    }

    public boolean isExistSkill(int type, int skill) {
        boolean Exist = false;
        switch (type) {
            case 1:
                Exist = SkillConstants.isCloseRangedAttack(skill);
                break;
            case 2:
                Exist = SkillConstants.isRangedAttack(skill);
                break;
            case 3:
                Exist = SkillConstants.isMagicAttack(skill);
                break;
            case 4:
                Exist = SkillConstants.isSpecialMove(skill);
                break;
        }
        return Exist;
    }

    private LinkedList<Integer> getSelectType(int type) {
        LinkedList<Integer> list_ = new LinkedList<>();
        switch (type) {
            case 1:
                list_ = CloseRangeAttack;
                break;
            case 2:
                list_ = RangeAttack;
                break;
            case 3:
                list_ = MagicAttack;
                break;
        }
        return list_;
    }
}
