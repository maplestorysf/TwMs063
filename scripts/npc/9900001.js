load('nashorn:mozilla_compat.js');
/** Author: nejevoli
 NPC Name: 		NimaKin
 Map(s): 		Victoria Road : Ellinia (180000000)
 Description: 		Maxes out your stats and able to modify your equipment stats
 */
importPackage(java.lang);

var status = 0;
var slot = Array();
var stats = Array("力量", "敏捷", "智力", "幸運", "HP", "MP", "物理攻擊", "魔法攻擊", "物理防禦", "魔法防禦", "命中率", "迴避率", "靈敏度", "移動速度", "跳躍力", "卷軸數", "黃金鐵鎚使用數", "使用卷軸數", "星星數", "淺能 1", "淺能 2", "淺能 3", "裝備名字");
var maps = Array(104000000,100000000,101000000,103000000,120000000,105040300,140000000,130000000,200000000,110000000,230000000,240000000,270000100,211000000,222000000,221000000,260000000,261000000,250000000,251000000,540000000,550000000,500000000,501000000,300000000,600000000,702000000,740000000,742000000,800000000,801000000);
var items = Array(5160013,5160003,5160009,5160000,5160012,5160006,5160008,5160014,5450000,5010057,5190000,5340001,5140000,5370001,5021000,5021006,5021011);
var selected;
var statsSel;
var ss = "";
var dd = "";
var title = "";

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (status >= 0 && mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1)
        status++;
    else
        status--;

    if (status == 0) {
        if (cm.getPlayerStat("GM") == 1) {
            cm.sendSimple("親愛的#h \r\n管理員我能為您做什麼呢？？#b\r\n#L0#幫我能力值加到全滿！！#l\r\n#L1#幫我技能加到全滿！！#l\r\n#L2#幫我修改裝備數值！！#l\r\n#L4#幫我初始化AP/SP！#l#k#b\r\n#L5#for map test#l#k#b\r\n#L6#gain Item#l#k#b\r\n#L7#send message#l#k");
        } else {
            cm.dispose();
        }
    } else if (status == 1) {
        if (selection == 0) {
            if (cm.getPlayerStat("GM") == 1) {
                cm.maxStats();
                cm.sendOk("已經幫您加滿了！！");
            }
            cm.dispose();
        } else if (selection == 1) {
            //Beginner
            if (cm.getPlayerStat("GM") == 1) {
                cm.maxAllSkills();
            }
            cm.dispose();
        } else if (selection == 2 && cm.getPlayerStat("ADMIN") == 1) {
            var avail = "";
            for (var i = 0; i > -199; i--) {
                if (cm.getInventory(1).getItem(i) != null) {
                    avail += "#L" + Math.abs(i) + "##t" + cm.getInventory(-1).getItem(i).getItemId() + "##l\r\n";
                }
                slot.push(i);
            }
            cm.sendSimple("想要修改哪一件裝備能力值呢？？\r\n#b" + avail);
        } else if (selection == 3 && cm.getPlayerStat("ADMIN") == 1) {
            var eek = cm.getAllPotentialInfo();
            var avail = "";
            for (var ii = 0; ii < eek.size(); ii++) {
                avail += "#L" + eek.get(ii) + "#淺能 ID " + eek.get(ii) + "#l\r\n";
            }
            cm.sendSimple("請問想了解？？\r\n#b" + avail);
            status = 9;
        } else if (selection == 4) {
            cm.getPlayer().resetAPSP();
            cm.sendNext("完成，請換頻道or重新登入。");
            cm.dispose();
		} else if (selection == 5) {
			var selStr = "選擇您的目的地.#b";
            for (var i = 0; i < maps.length; i++) {
                selStr += "\r\n#L" + i + "##m" + maps[i] + "# #l";
            }
			cm.sendSimple(selStr);
			cm.dispose();
		} else if (selection == 6) {
			for (var i = 0; i < items.length; i++) {
				cm.gainItem(items[i],1);
			}
			cm.dispose();
		} else if (selection == 7) {
			status =10;
			cm.sendGetText("請輸入你想要顯示的公告標題");
        } else {
            cm.dispose();
        }
    } else if (status == 2 && cm.getPlayerStat("ADMIN") == 1) {
        selected = selection - 1;
        var text = "";
        for (var i = 0; i < stats.length; i++) {
            text += "#L" + i + "#" + stats[i] + "#l\r\n";
        }
        cm.sendSimple("你想要修改你的 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k.\r\n想修改哪個能力值？？\r\n#b" + text);
    } else if (status == 3 && cm.getPlayerStat("ADMIN") == 1) {
        statsSel = selection;
        if (selection == 22) {
            cm.sendGetText("請問你想設置多少 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k's " + stats[statsSel] + " 能力值?");
        } else {
            cm.sendGetNumber("請問你想設置 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k's " + stats[statsSel] + " 多少能力值?", 0, 0, 32767);
        }
    } else if (status == 4 && cm.getPlayerStat("ADMIN") == 1) {
        cm.changeStat(slot[selected], statsSel, selection);
        cm.sendOk("你的 #b#t" + cm.getInventory(-1).getItem(slot[selected]).getItemId() + "##k's " + stats[statsSel] + " 已被設置為 " + selection + ".");
        cm.dispose();
        //cm.getPlayer().fakeRelog();
    } else if (status == 10 && cm.getPlayerStat("ADMIN") == 1) {
        cm.sendSimple("#L3#" + cm.getPotentialInfo(selection) + "#l");
        status = 0;
	} else if (status == 11) {
		title = cm.getText();
		cm.sendGetText("請輸入你想要的名字");
	} else if (status == 12) {
		ss = cm.getText();
		cm.sendGetText("請輸入訊息");
	} else if (status == 13) {
		dd = cm.getText();
		//cm.sendNext("test1:"+title+"\r\ntest2:"+ss+"\r\ntest3:"+dd);
		cm.worldMessage(6 ,"["+title+"] " + ss +"因為"+ dd +"而被管理員永久停權。");
		cm.dispose();
    } else {
        cm.dispose();
    }
}