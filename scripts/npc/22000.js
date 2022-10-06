/* 
	NPC Name: 		Shanks
	Map(s): 		Maple Road : Southperry (60000)
	Description: 		Brings you to Victoria Island
*/
var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (status >= 0 && mode == 0) {
    cm.sendOk("恩... 我猜你還有想在這做的事？");
	cm.dispose();
	return;
    }
    if (mode == 1)
	status++;
    else
	status--;

    if (status == 0) {
    cm.sendYesNo("搭上了這艘船，你可以前往更大的大陸冒險。 只要給我 #e150 楓幣#n，我會帶你去 #b維多利亞島#k。 不過，一旦離開了這裡，就不能再回來囉。 你想要去維多利亞島嗎？");
    } else if (status == 1) {
	if (cm.haveItem(4031801)) {
    cm.sendNextPrev("既然你有推薦信，我不會收你任何的費用。收起來，我們將前往維多利亞島，坐好，旅途中可能會有點動盪！");
	} else {
	    cm.sendNext("真的只要 #e150 楓幣#n 就能搭船!!");
	}
    } else if (status == 2) {
	if (cm.haveItem(4031801)) {
	    cm.sendNextPrev("既然你有推薦信，我不會收你任何的費用。收起來，我們將前往維多利亞島，坐好，旅途中可能會有點動盪！");
	} else {
	    if (cm.getPlayerStat("LVL") >= 7) {
		if (cm.getMeso() < 150) {
		    cm.sendOk("什麼？你說你想搭免費的船？ 你真是個怪人！");
		    cm.dispose();
		} else {
		    cm.sendNext("哇! #e150#n 楓幣我收到了！ 好，準備出發去維多利亞港囉！");
		}
	    } else {
		cm.sendOk("讓我看看... 我覺得你還不夠強壯。 你至少要達到7等我才能讓你到維多利亞港囉。");
		cm.dispose();
	    }
	}
    } else if (status == 3) {
	if (cm.haveItem(4031801)) {
	    cm.gainItem(4031801, -1);
	    cm.warp(104000000,0);
	    cm.dispose();
	} else {
	    cm.gainMeso(-150);
	    cm.warp(104000000,0);
	    cm.dispose();
	}
    }
}